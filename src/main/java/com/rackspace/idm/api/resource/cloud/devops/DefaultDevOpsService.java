package com.rackspace.idm.api.resource.cloud.devops;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.FederatedUsersDeletionRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.FederatedUsersDeletionResponse;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone;
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.filter.LdapLoggingFilter;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.multifactor.service.MultiFactorService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;

@Component
public class DefaultDevOpsService implements DevOpsService {
    private static final Logger LOG = Logger.getLogger(DefaultDevOpsService.class);

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    UserService userService;

    @Autowired
    private Configuration globalConfig;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private MultiFactorService multiFactorService;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired(required = false)
    private CacheableKeyCzarCrypterLocator cacheableKeyCzarCrypterLocator;

    @Override
    @Async
    public void encryptUsers(String authToken) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));
        userService.reEncryptUsers();
    }

    @Override
    public Response.ResponseBuilder getLdapLog(UriInfo uriInfo, String authToken, String logName) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));

        if (!isLdapLoggingAllowed()) {
            throw new WebApplicationException(404);
        }

        try {
            File logDir = getLogParentDir();
            File logFile = new File(logDir, logName);
            String logContents = FileUtils.readFileToString(logFile);

            Response.ResponseBuilder response = Response.ok().entity(logContents);

            return response;
        } catch (IOException e) {
            LOG.error("Encountered exception creating ldap log. Logging disabled", e);
            throw new RuntimeException("Error retrieving log", e);
        }
    }

    @Override
    public Response.ResponseBuilder getKeyMetadata(String authToken) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));
        if (cacheableKeyCzarCrypterLocator == null) {
            return Response.noContent();
        } else {
            final com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory factory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();
            return Response.ok().entity(factory.createMetadata(cacheableKeyCzarCrypterLocator.getCacheInfo()));
        }
    }

    @Override
    public Response.ResponseBuilder resetKeyMetadata(String authToken) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));
        if (cacheableKeyCzarCrypterLocator == null) {
            return Response.noContent();
        } else {
            cacheableKeyCzarCrypterLocator.resetCache();
            final com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory factory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();
            return Response.ok().entity(factory.createMetadata(cacheableKeyCzarCrypterLocator.getCacheInfo()));
        }
    }

    @Override
    public Response.ResponseBuilder getIdmProps(String authToken) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));
        String idmProps = identityConfig.toJSONString();
        Response.ResponseBuilder response = Response.ok().entity(idmProps);
        return response;
    }

    @Override
    public Response.ResponseBuilder expiredFederatedUsersDeletion(String authToken, FederatedUsersDeletionRequest request) {
        requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
        authorizationService.verifyEffectiveCallerHasRoleByName(identityConfig.getReloadableConfig().getFederatedDeletionRole());

        final com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory factory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();
        final FederatedUsersDeletionResponse response = factory.createFederatedUsersDeletionResponse();

        userService.expiredFederatedUsersDeletion(request, response);
        return Response.ok().entity(factory.createFederatedUsersDeletionResponse(response));
    }

    @Override
    public Response.ResponseBuilder setupSmsMfaOnUser(String authToken, String userId, MobilePhone phone) {
        try {
            ScopeAccess token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_MFA_ADMIN.getRoleName());

            //if the target user is not a provisioned user (e.g. - a fed user), throw bad request cause fed users can't have MFA
            EndUser endUser = requestContextHolder.getAndCheckTargetEndUser(userId);
            if (!(endUser instanceof User)) {
                throw new BadRequestException("Only provisioned users store multi-factor information within Identity");
            }
            User user = (User) endUser;

            //Must not have any MFA devices.
            if (multiFactorService.userHasMultiFactorDevices(user) || user.isMultiFactorEnabled()) {
                throw new BadRequestException("You can not configure a user that already has mfa devices or has MFA enabled.");
            }

            if (StringUtils.isBlank(phone.getNumber() )) {
                throw new BadRequestException("Phone must be provided to setup SMA for this user");
            }

            Phonenumber.PhoneNumber phoneNumber = null;
            try {
                phoneNumber = IdmPhoneNumberUtil.getInstance().parsePhoneNumber(phone.getNumber());
            } catch (com.rackspace.identity.multifactor.exceptions.InvalidPhoneNumberException ex) {
                throw new BadRequestException("Invalid phone", ex);
            }

            multiFactorService.setupSmsForUser(userId, phoneNumber);
            return Response.status(Response.Status.NO_CONTENT);
        } catch (Exception ex) {
            LOG.error(String.format("Error setting up SMS MFA for user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder removeMfaFromUser(String authToken, String userId) {
        try {
            ScopeAccess token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_MFA_ADMIN.getRoleName());

            //if the target user is not a provisioned user (e.g. - a fed user), throw bad request cause fed users can't have MFA
            EndUser endUser = requestContextHolder.getAndCheckTargetEndUser(userId);
            if (!(endUser instanceof User)) {
                throw new BadRequestException("Only provisioned users store multi-factor information within Identity");
            }
            User user = (User) endUser;

            if (!user.isMultiFactorEnabled()) {
                throw new BadRequestException("MFA must be enabled on user to use this service", ErrorCodes.ERROR_CODE_MFA_MIGRATION_MFA_NOT_ENABLED);
            }
            if (user.getMultiFactorTypeAsEnum() != FactorTypeEnum.SMS) {
                throw new BadRequestException("User has OTP enabled. Can only remove SMS MFA", ErrorCodes.ERROR_CODE_MFA_MIGRATION_OTP_ENABLED);
            }

            multiFactorService.removeMultifactorFromUserWithoutNotifications(user);
            return Response.status(Response.Status.NO_CONTENT);
        } catch (Exception ex) {
            LOG.error(String.format("Error removing MFA from user '%s'", userId), ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private File getLogParentDir() {
        String logFileDir = globalConfig.getString(LdapLoggingFilter.UNBOUNDID_LOG_LOCATION_PROP_NAME, LdapLoggingFilter.UNBOUNDID_LOG_LOCATION_DEFAULT);
        File logDir = new File(logFileDir);
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                throw new IllegalStateException("Could not create directories '" + logFileDir + " for ldap log files.");
            }
        }
        if (!logDir.isDirectory()) {
            throw new IllegalArgumentException("'" + logFileDir + "' is not a directory. Please set " + LdapLoggingFilter.UNBOUNDID_LOG_LOCATION_PROP_NAME + " to a directory.");
        }
        return logDir;
    }

    private boolean isLdapLoggingAllowed() {
        return globalConfig.getBoolean(LdapLoggingFilter.UNBOUND_LOG_ALLOW_PROP_NAME, LdapLoggingFilter.UNBOUND_LOG_ALLOW_DEFAULT);
    }

    private ScopeAccess getScopeAccessForValidToken(String authToken) {
        String errMsg = "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.";
        if (StringUtils.isBlank(authToken)) {
            throw new NotAuthorizedException(errMsg);
        }
        ScopeAccess authScopeAccess = this.scopeAccessService.getScopeAccessByAccessToken(authToken);
        if (authScopeAccess == null || (authScopeAccess.isAccessTokenExpired(new DateTime()))) {
            throw new NotAuthorizedException(errMsg);
        }
        return authScopeAccess;
    }
}
