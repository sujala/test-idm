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
import com.rackspace.idm.domain.config.*;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.multifactor.service.MultiFactorService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        return getIdmPropsByQuery(authToken, null, null);
    }

    @Override
    public Response.ResponseBuilder getIdmPropsByQuery(String authToken, final List<String> versions, final String name) {
        requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_QUERY_PROPS.getRoleName());
        Predicate<IdmProperty> reloadablePredicate = new Predicate<IdmProperty>() {
                    @Override
                    public boolean evaluate(IdmProperty object) {
                        if (object.getType() != IdmPropertyType.RELOADABLE) return false;
                        if (StringUtils.isNotBlank(name) && !StringUtils.contains(object.getName(), name)) return false;
                        if (CollectionUtils.isNotEmpty(versions) && !versions.contains(object.getVersionAdded())) return false;

                        return true;
                    }
                };

        Predicate<IdmProperty> staticPredicate =  new Predicate<IdmProperty>() {
                    @Override
                    public boolean evaluate(IdmProperty object) {
                        if (object.getType() != IdmPropertyType.STATIC) return false;
                        if (StringUtils.isNotBlank(name) && !StringUtils.contains(object.getName(), name)) return false;
                        if (CollectionUtils.isNotEmpty(versions) && !versions.contains(object.getVersionAdded())) return false;

                        return true;
                    }
                };

        return filterIdmProps(staticPredicate, reloadablePredicate);
    }

    private Response.ResponseBuilder filterIdmProps(Predicate<IdmProperty> staticPredicate, Predicate<IdmProperty> reloadablePredicate) {
        List<IdmProperty> idmPropertyList = identityConfig.getPropertyInfoList();

        List<IdmProperty> queriedReloadableIdmPropertyList = new ArrayList<IdmProperty>();
        CollectionUtils.select(idmPropertyList, reloadablePredicate, queriedReloadableIdmPropertyList);
        Collections.sort(queriedReloadableIdmPropertyList);

        List<IdmProperty> queriedStaticIdmPropertyList = new ArrayList<IdmProperty>();
        CollectionUtils.select(idmPropertyList, staticPredicate, queriedStaticIdmPropertyList);
        Collections.sort(queriedStaticIdmPropertyList);

        JSONObject props = new JSONObject();
        props.put("configPath", identityConfig.getConfigRoot());
        props.put(PropertyFileConfiguration.CONFIG_FILE_NAME, toJSONObject(queriedStaticIdmPropertyList));
        props.put(PropertyFileConfiguration.RELOADABLE_CONFIG_FILE_NAME, toJSONObject(queriedReloadableIdmPropertyList));
        String idmProps = props.toJSONString();
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

    /**
     * Return JSON representation of properties and their values, as annotated by {@link com.rackspace.idm.domain.config.IdmProp}.
     *
     * Uses reflection to discover getters that have been annotated with {@link com.rackspace.idm.domain.config.IdmProp}
     * @return JSONObject properties
     */
    private JSONArray toJSONObject(List<IdmProperty> idmProperties) {
        final String name = "name";
        final String description = "description";
        final String versionAdded= "versionAdded";
        final String propValue = "value";
        final String defaultValue = "defaultValue";

        JSONArray propArr = new JSONArray();
        for (IdmProperty idmProperty : idmProperties) {
            JSONObject prop = new JSONObject();
            try {
                prop.put(name, idmProperty.getName());
                prop.put(description, idmProperty.getDescription());
                prop.put(versionAdded, idmProperty.getVersionAdded());

                Object convertedDefaultValue = valueToAddToJSON(idmProperty.getDefaultValue());
                prop.put(defaultValue, convertedDefaultValue);

                Object convertedValue = valueToAddToJSON(idmProperty.getValue());
                prop.put(propValue, convertedValue);
                propArr.add(prop);
            } catch (Exception e) {
                LOG.error(String.format("error retrieving property '%s'", idmProperty.getName()), e);
            }
        }
        return propArr;
    }

    private Object valueToAddToJSON(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        } else if (value instanceof String[] ) {
            JSONArray valueArray = new JSONArray();
            for (String val : (String[])value) {
                valueArray.add(val);
            }
            return valueArray;
        } else if (value instanceof Enum) {
            return ((Enum)value).name();
        } else {
            return value.toString();
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
