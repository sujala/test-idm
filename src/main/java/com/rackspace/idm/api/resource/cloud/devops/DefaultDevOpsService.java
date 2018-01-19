package com.rackspace.idm.api.resource.cloud.devops;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone;
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyConverter;
import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter;
import com.rackspace.idm.api.filter.LdapLoggingFilter;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.config.*;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.multifactor.service.MultiFactorService;
import com.rackspace.idm.validation.IdentityPropertyValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DefaultDevOpsService implements DevOpsService {

    public static final String IDENTITY_PROPERTY_SOURCE = "directory";
    public static final String IDENTITY_PROPERTY_NOT_FOUND_MSG = "Identity Property with provided ID not found";
    public static final String IDENTITY_PROPERTY_NAME_CONFLICT_MSG = "An Identity property with the given name already exists";

    private static final Logger logger = LoggerFactory.getLogger(DefaultDevOpsService.class);

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    UserService userService;

    @Autowired
    private DomainService domainService;

    @Autowired
    IdentityUserService identityUserService;

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

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private IdentityPropertyService identityPropertyService;

    @Autowired
    private IdentityPropertyConverter identityPropertyConverter;

    @Autowired
    private IdentityPropertyValidator identityPropertyValidator;

    @Autowired
    private JsonWriterForIdmProperty jsonWriterForIdmProperty;

    @Autowired
    private AETokenService aeTokenService;

    @Autowired
    private AETokenRevocationService aeTokenRevocationService;

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private IdentityPropertyValueConverter propertyValueConverter;

    final com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory v1ObjectFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();

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
            logger.error("Encountered exception creating ldap log. Logging disabled", e);
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
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRoles(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_QUERY_PROPS.getRoleName(), IdentityRole.IDENTITY_PROPERTY_ADMIN.getRoleName());

        return filterIdmProps(versions, name);
    }

    private Response.ResponseBuilder filterIdmProps(final List<String> versions, final String name) {
        Predicate<IdmProperty> reloadablePredicate = new Predicate<IdmProperty>() {
            @Override
            public boolean evaluate(IdmProperty object) {
                if (object.getType() != IdmPropertyType.RELOADABLE) return false;
                if (StringUtils.isNotBlank(name) && !StringUtils.containsIgnoreCase(object.getName(), name)) return false;
                if (CollectionUtils.isNotEmpty(versions) && !versions.contains(object.getVersionAdded())) return false;

                return true;
            }
        };

        Predicate<IdmProperty> staticPredicate =  new Predicate<IdmProperty>() {
            @Override
            public boolean evaluate(IdmProperty object) {
                if (object.getType() != IdmPropertyType.STATIC) return false;
                if (StringUtils.isNotBlank(name) && !StringUtils.containsIgnoreCase(object.getName(), name)) return false;
                if (CollectionUtils.isNotEmpty(versions) && !versions.contains(object.getVersionAdded())) return false;

                return true;
            }
        };

        List<IdmProperty> idmPropertyList = identityConfig.getPropertyInfoList();

        List<IdmProperty> queriedReloadableIdmPropertyList = new ArrayList<>();
        CollectionUtils.select(idmPropertyList, reloadablePredicate, queriedReloadableIdmPropertyList);

        List<IdmProperty> queriedStaticIdmPropertyList = new ArrayList<>();
        CollectionUtils.select(idmPropertyList, staticPredicate, queriedStaticIdmPropertyList);

        Iterable<com.rackspace.idm.domain.entity.IdentityProperty> directoryIdentityProps =
                identityPropertyService.getIdentityPropertyByNameAndVersions(name, versions);
        List<IdmProperty> directoryIdmProperties = convertIdentityPropertyToIdmProperty(directoryIdentityProps);

        List<IdmProperty> allPropertiesList = new ArrayList<>();
        allPropertiesList.addAll(queriedReloadableIdmPropertyList);
        allPropertiesList.addAll(queriedStaticIdmPropertyList);
        allPropertiesList.addAll(directoryIdmProperties);
        Collections.sort(allPropertiesList);

        return Response.ok().entity(jsonWriterForIdmProperty.toJsonString(allPropertiesList));
    }

    @Override
    public Response.ResponseBuilder expiredFederatedUsersDeletion(String authToken, FederatedUsersDeletionRequest request) {
        requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
        authorizationService.verifyEffectiveCallerHasRoleByName(identityConfig.getReloadableConfig().getFederatedDeletionRole());

        final FederatedUsersDeletionResponse response = v1ObjectFactory.createFederatedUsersDeletionResponse();

        userService.expiredFederatedUsersDeletion(request, response);
        return Response.ok().entity(v1ObjectFactory.createFederatedUsersDeletionResponse(response));
    }

    @Override
    public Response.ResponseBuilder purgeObsoleteTrrs(String authToken, TokenRevocationRecordDeletionRequest request) {
        requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
        authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PURGE_TOKEN_REVOCATION_RECORDS.getRoleName());

        //if not provided use default delay.
        int requestedDelay = request.getDelay() == null ? IdentityConfig.PURGE_TRRS_DEFAULT_DELAY : request.getDelay();
        if (requestedDelay < 0 || requestedDelay > identityConfig.getReloadableConfig().getPurgeTokenRevocationRecordsMaxDelay()) {
            throw new BadRequestException(String.format("When provided, the requested delay must be >= 0 and <= %d"
                    , identityConfig.getReloadableConfig().getPurgeTokenRevocationRecordsMaxDelay()));
        }

        //if not provided or provided is not valid (e.g. negative, > max allowed), use max allowed.
        int requestedLimit = request.getLimit() == null ? identityConfig.getReloadableConfig().getPurgeTokenRevocationRecordsMaxLimit() : request.getLimit();
        if (requestedLimit <= 0 || requestedLimit > identityConfig.getReloadableConfig().getPurgeTokenRevocationRecordsMaxLimit()) {
            throw new BadRequestException(String.format("When provided, the requested limit must be > 0 and <= %d"
                    , identityConfig.getReloadableConfig().getPurgeTokenRevocationRecordsMaxLimit()));
        }

        TokenRevocationRecordDeletionResponse tokenRevocationRecordDeletionResponse = tokenRevocationService.purgeObsoleteTokenRevocationRecords(requestedLimit, requestedDelay);
        return Response.ok().entity(v1ObjectFactory.createTokenRevocationRecordDeletionResponse(tokenRevocationRecordDeletionResponse));
    }

    @Override
    public Response.ResponseBuilder createIdmProperty(String authToken, IdentityProperty identityProperty) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROPERTY_ADMIN.getRoleName());

            identityPropertyValidator.validateIdentityPropertyForCreate(identityProperty);

            com.rackspace.idm.domain.entity.IdentityProperty existingProperty = identityPropertyService.getIdentityPropertyByName(identityProperty.getName());
            if (existingProperty != null) {
                throw new DuplicateException(IDENTITY_PROPERTY_NAME_CONFLICT_MSG);
            }

            com.rackspace.idm.domain.entity.IdentityProperty idmProp = identityPropertyConverter.toIdentityProperty(identityProperty);
            identityPropertyService.addIdentityProperty(idmProp);

            // Do not return the value to the user if the property is not searchable
            if (!idmProp.isSearchable()) {
                idmProp.setValue(null);
            }

            return Response.status(Response.Status.CREATED).entity(objFactories.getRackspaceIdentityExtRaxgaV1Factory()
                    .createIdentityProperty(identityPropertyConverter.fromIdentityProperty(idmProp)).getValue());
        } catch (Exception ex) {
            logger.error("Error creating Identity property", ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder updateIdmProperty(String authToken, String idmPropertyId, IdentityProperty identityProperty) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROPERTY_ADMIN.getRoleName());

            com.rackspace.idm.domain.entity.IdentityProperty propEntity = identityPropertyService.getIdentityPropertyById(idmPropertyId);

            if (propEntity == null) {
                throw new NotFoundException(IDENTITY_PROPERTY_NOT_FOUND_MSG);
            }

            // Set the value type of the stored property so that we can property validate it
            identityProperty.setValueType(propEntity.getValueType());
            identityPropertyValidator.validateIdentityPropertyForUpdate(identityProperty);

            // Manually set the properties that a user is allowed to update
            if (StringUtils.isNotBlank(identityProperty.getValue())) {
                byte [] propValue = identityProperty.getValue().getBytes(StandardCharsets.UTF_8);
                propEntity.setValue(propValue);
            }

            if (StringUtils.isNotBlank(identityProperty.getDescription())) {
                propEntity.setDescription(identityProperty.getDescription());
            }

            if (StringUtils.isNotBlank(identityProperty.getIdmVersion())) {
                propEntity.setIdmVersion(identityProperty.getIdmVersion());
            }

            if (identityProperty.isSearchable() != null) {
                propEntity.setSearchable(identityProperty.isSearchable());
            }

            identityPropertyService.updateIdentityProperty(propEntity);

            // Do not return the value to the user if the property is not searchable
            if (!propEntity.isSearchable()) {
                propEntity.setValue(null);
            }

            return Response.status(Response.Status.OK).entity(objFactories.getRackspaceIdentityExtRaxgaV1Factory()
                    .createIdentityProperty(identityPropertyConverter.fromIdentityProperty(propEntity)).getValue());
        } catch (Exception ex) {
            logger.error("Error updating Identity property", ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder deleteIdmProperty(String authToken, String idmPropertyId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROPERTY_ADMIN.getRoleName());

            if (StringUtils.isEmpty(idmPropertyId)) {
                throw new BadRequestException();
            }

            com.rackspace.idm.domain.entity.IdentityProperty idmProp = identityPropertyService.getIdentityPropertyById(idmPropertyId);

            if (idmProp == null) {
                throw new NotFoundException(IDENTITY_PROPERTY_NOT_FOUND_MSG);
            }

            identityPropertyService.deleteIdentityProperty(idmProp);

            return Response.status(Response.Status.NO_CONTENT);
        } catch (Exception ex) {
            logger.error("Error deleting Identity property", ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder analyzeToken(String authToken, String subjectToken) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_ANALYZE_TOKEN.getRoleName());

            if (StringUtils.isEmpty(subjectToken)) {
                throw new BadRequestException("Must provide an X-Subject-Token header with the token to analyze");
            }

            String tokenAnalysis = null;
            try {
                // Unmarshall token without checking for revocation. Just want to crack the token
                ScopeAccess saSubjectToken = aeTokenService.unmarshallToken(subjectToken);

                List<LdapTokenRevocationRecord> trrs = Collections.emptyList();
                BaseUser user = null;
                BaseUser impersonatedUser = null;
                Domain userDomain = null;
                Domain impersonatedUserDomain = null;

                if (saSubjectToken != null) {
                    // Find any TRRs that would revoke this token
                    trrs = aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(saSubjectToken);

                    // Get user associated with token
                    try {
                        user = userService.getUserByScopeAccess(saSubjectToken, false);
                    } catch (NotFoundException e) {
                        user = getDeletedUserByScopeAccess(saSubjectToken);
                    }

                    if (StringUtils.isNotBlank(user.getDomainId())) {
                        userDomain = domainService.getDomain(user.getDomainId());
                    }

                    if (saSubjectToken instanceof ImpersonatedScopeAccess){
                        ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) saSubjectToken;
                        impersonatedUser = identityUserService.getEndUserById(impersonatedScopeAccess.getRsImpersonatingRsId());
                        if (impersonatedUser != null) {
                            impersonatedUserDomain = domainService.getDomain(impersonatedUser.getDomainId());
                        } else {
                            impersonatedUser = new User();
                            ((User)impersonatedUser).setId(impersonatedScopeAccess.getRsImpersonatingRsId());
                        }
                    }
                }
                tokenAnalysis = TokenAnalysis.fromEntities(saSubjectToken, user, impersonatedUser, userDomain, impersonatedUserDomain, trrs).toJson();
            } catch (UnmarshallTokenException e) {
                tokenAnalysis = TokenAnalysis.fromException(e).toJson();
                logger.debug("Unable to unmarshall the token: " + tokenAnalysis);
            }
            return Response.ok(tokenAnalysis);
        } catch (Exception ex) {
            logger.error("Error analyzing token", ex);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private BaseUser getDeletedUserByScopeAccess(ScopeAccess scopeAccess) {
        BaseUser user = null;

        if (scopeAccess instanceof ImpersonatedScopeAccess) {
            ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) scopeAccess;

            if (impersonatedScopeAccess.getRackerId() != null) {
                user = new Racker();
                ((Racker)user).setId(impersonatedScopeAccess.getRackerId());
            } else {
                user = new User();
                ((User)user).setId(impersonatedScopeAccess.getUserRsId());
            }
        } else {
            if (scopeAccess instanceof UserScopeAccess) {
                UserScopeAccess userScopeAccess = (UserScopeAccess)scopeAccess;
                if (CollectionUtils.isNotEmpty(scopeAccess.getAuthenticatedBy()) && scopeAccess.getAuthenticatedBy().contains(GlobalConstants.AUTHENTICATED_BY_FEDERATION)) {
                    user = new FederatedUser();
                    ((FederatedUser)user).setId(userScopeAccess.getUserRsId());
                } else {
                    user = new User();
                    ((User)user).setId(userScopeAccess.getUserRsId());
                }
            }
        }
        return user;
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

    private List<IdmProperty> convertIdentityPropertyToIdmProperty(Iterable<com.rackspace.idm.domain.entity.IdentityProperty> directoryIdentityProps) {
        List<IdmProperty> idmProps = new ArrayList<>();

        if (directoryIdentityProps != null) {
            for (com.rackspace.idm.domain.entity.IdentityProperty identityProperty : directoryIdentityProps) {
                IdmProperty idmProperty = new IdmProperty();
                idmProperty.setId(identityProperty.getId());
                idmProperty.setType(IdmPropertyType.DIRECTORY);
                idmProperty.setName(identityProperty.getName());
                idmProperty.setDescription(identityProperty.getDescription());
                try {
                    // try to parse the value into a primitive type
                    idmProperty.setValue(propertyValueConverter.convertPropertyValue(identityProperty));
                } catch (Exception e) {
                    // but fall back to a String if not parseable
                    idmProperty.setValue(identityProperty.getValue());
                }
                idmProperty.setValueType(identityProperty.getValueType());
                idmProperty.setVersionAdded(identityProperty.getIdmVersion());
                idmProperty.setSource(IDENTITY_PROPERTY_SOURCE);
                idmProperty.setReloadable(identityProperty.isReloadable());

                idmProps.add(idmProperty);
            }
        }

        return idmProps;
    }

}
