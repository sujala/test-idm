package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForCredentialType;
import com.rackspace.idm.domain.service.impl.*;
import com.rackspace.idm.validation.Cloud20CreateUserValidator;
import com.rackspace.idm.api.resource.pagination.Paginator;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Domains;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.SamlLogoutResponseUtil;
import com.rackspace.idm.util.SamlUnmarshaller;
import com.rackspace.idm.validation.PrecedenceValidator;
import com.rackspace.idm.validation.Validator;
import com.rackspace.idm.validation.Validator20;
import com.unboundid.ldap.sdk.LDAPException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.impl.LogoutResponseMarshaller;
import org.opensaml.xml.io.MarshallingException;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.*;

@Component
public class DefaultCloud20Service implements Cloud20Service {

    public static final String NOT_AUTHORIZED = "Not Authorized";
    public static final String ID_MISMATCH = "Id in url does not match id in body.";
    public static final String USER_AND_USER_ID_MIS_MATCHED = "User and UserId mis-matched";
    public static final String RBAC = "rbac";
    public static final String SETUP_MFA_SCOPE_FORBIDDEN = "SETUP-MFA SCOPE not supported";
    public static final String MUST_SETUP_MFA = "User must setup multi-factor";

    public static final String FEATURE_USER_TOKEN_SELF_VALIDATION = "feature.user.token.selfValidation";
    public static final boolean FEATURE_USER_TOKEN_SELF_VALIDATION_DEFAULT_VALUE = false;

    public static final String INVALID_DOMAIN_ERROR = "Invalid domain";
    public static final String DOMAIN_ID_NOT_FOUND_ERROR_MESSAGE = "Domain ID %s does not exist.";
    public static final String CANNOT_SPECIFY_GROUPS_ERROR = "Cannot specify groups for sub-users";
    public static final String V11_API_QNAME = "http://docs.rackspacecloud.com/auth/api/v1.1";

    public static final String FEDERATION_IDP_TYPE_ERROR_MESSAGE = "%s is currently the only supported IDP type allowed for filtering.";
    public static final String FEDERATION_IDP_FILTER_CONFLICT_ERROR_MESSAGE = "The provided IDP filters cannot be used together.";
    public static final String FEDERATION_IDP_FILTER_TENANT_NO_DOMAIN_ERROR_MESSAGE = "The provided tenant is not associated with a domain";

    public static final String DUPLICATE_SERVICE_NAME_ERROR_MESSAGE = "More than one service exists with the given name. Please specify a different service name for the endpoint template.";
    public static final String DUPLICATE_SERVICE_ERROR_MESSAGE = "Unable to fulfill request. More than one service exists with the given name.";

    public static final String ERROR_CANNOT_DELETE_USER_TYPE_ROLE_MESSAGE = "Cannot delete identity user-type roles from a user.";

    public static final String ERROR_CANNOT_DELETE_ENDPOINT_TEMPLATE_MESSAGE = "Deleting enabled templates or templates associated with one or more tenants is not allowed";

    public static final String ROLE_ID_NOT_FOUND_ERROR_MESSAGE = "Role with ID %s not found.";

    public static final String USER_NOT_FOUND_ERROR_MESSAGE = "User with ID %s not found.";

    @Autowired
    private AuthConverterCloudV20 authConverterCloudV20;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private EndpointConverterCloudV20 endpointConverterCloudV20;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private RoleConverterCloudV20 roleConverterCloudV20;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private ServiceConverterCloudV20 serviceConverterCloudV20;

    @Autowired
    private TenantConverterCloudV20 tenantConverterCloudV20;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TokenConverterCloudV20 tokenConverterCloudV20;

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private IdentityProviderConverterCloudV20 identityProviderConverterCloudV20;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private CloudGroupBuilder cloudGroupBuilder;

    @Autowired
    private CloudKsGroupBuilder cloudKsGroupBuilder;

    @Lazy
    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private Validator validator;

    @Autowired
    private Validator20 validator20;

    @Autowired
    private DefaultRegionService defaultRegionService;

    @Autowired
    private DomainConverterCloudV20 domainConverterCloudV20;

    @Autowired
    private DomainService domainService;

    @Autowired
    private FederatedIdentityService federatedIdentityService;

    @Autowired
    private CloudRegionService cloudRegionService;

    @Autowired
    private RegionConverterCloudV20 regionConverterCloudV20;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionConverterCloudV20 questionConverter;

    @Autowired
    private SecretQAService secretQAService;

    @Autowired
    private SecretQAConverterCloudV20 secretQAConverterCloudV20;

    @Autowired
    private Paginator<User> userPaginator;

    @Autowired
    private Paginator<EndUser> endUserPaginator;

    @Autowired
    private Paginator<ClientRole> applicationRolePaginator;

    @Autowired
    private PrecedenceValidator precedenceValidator;

    @Autowired
    private Paginator<Domain> domainPaginator;

    @Autowired
    private AuthWithToken authWithToken;

    @Autowired
    private AuthWithPasswordCredentials authWithPasswordCredentials;

    @Autowired
    private AuthWithApiKeyCredentials authWithApiKeyCredentials;

    @Autowired
    private AuthWithForgotPasswordCredentials authWithForgotPasswordCredentials;

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private SamlUnmarshaller samlUnmarshaller;

    @Autowired
    private Cloud20CreateUserValidator createUserValidator;

    @Autowired
    private CreateIdentityAdminService createIdentityAdminService;

    @Autowired
    private CreateUserAdminService createUserAdminService;

    @Autowired
    private CreateSubUserService createSubUserService;

    @Autowired
    private DefaultAuthenticateResponseService authenticateResponseService;

    private com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory raxAuthObjectFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();

    private LogoutResponseMarshaller marshaller = new LogoutResponseMarshaller();

    private Map<String, JAXBElement<Extension>> extensionMap;

    private JAXBElement<Extensions> currentExtensions;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, EndpointTemplate endpoint) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(Integer.toString(endpoint.getId()));
            tenant.getBaseUrlIds().add(String.valueOf(endpoint.getId()));
            this.tenantService.updateTenant(tenant);
            return Response.ok(
                    objFactories.getOpenStackIdentityV2Factory().createEndpoint(endpointConverterCloudV20.toEndpoint(baseUrl)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, EndpointTemplate endpoint) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            validator20.validateEndpointTemplate(endpoint);

            final CloudBaseUrl baseUrl = this.endpointConverterCloudV20.toCloudBaseUrl(endpoint);

            // Keystone V3 compatibility
            addEndpointTemplateKeystoneV3Data(endpoint, baseUrl);

            // Save the baseUrl
            this.endpointService.addBaseUrl(baseUrl);

            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            String id = String.valueOf(baseUrl.getBaseUrlId());
            URI build = requestUriBuilder.path(id).build();
            Response.ResponseBuilder response = Response.created(build);
            EndpointTemplate value = this.endpointConverterCloudV20.toEndpointTemplate(baseUrl);
            ObjectFactory openStackIdentityExtKscatalogV1Factory = objFactories.getOpenStackIdentityExtKscatalogV1Factory();
            response.entity(openStackIdentityExtKscatalogV1Factory.createEndpointTemplate(value).getValue());
            return response;
        } catch (DuplicateException dex) {
            return exceptionHandler.conflictExceptionResponse(dex.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private void addEndpointTemplateKeystoneV3Data(EndpointTemplate endpointTemplate, CloudBaseUrl cloudBaseUrl) {
        cloudBaseUrl.setPublicUrlId(UUID.randomUUID().toString().replaceAll("-", ""));
        if (!StringUtils.isEmpty(endpointTemplate.getInternalURL())) {
            cloudBaseUrl.setInternalUrlId(UUID.randomUUID().toString().replaceAll("-", ""));
        }
        if (!StringUtils.isEmpty(endpointTemplate.getAdminURL())) {
            cloudBaseUrl.setAdminUrlId(UUID.randomUUID().toString().replaceAll("-", ""));
        }
        // No need to retrieve the application if service id is provided on endpoint template creation.
        if (endpointTemplate.getServiceId() != null) {
            return;
        }
        String serviceName = endpointTemplate.getName();
        if (serviceName != null) {
            Application application;
            try {
                application = applicationService.checkAndGetApplicationByName(serviceName);
            } catch(IllegalStateException e) {
                if (e.getCause() != null && e.getCause() instanceof LDAPException) {
                    throw new UnrecoverableIdmException(DUPLICATE_SERVICE_NAME_ERROR_MESSAGE, e);
                }
                throw e;
            }
            if (!endpointTemplate.getType().equalsIgnoreCase(application.getOpenStackType())) {
                String msg = String.format("Incorrect type for %s service", application.getName());
                throw new BadRequestException(msg);
            }
            cloudBaseUrl.setClientId(application.getClientId());
        } else {
            throw new BadRequestException("Service name cannot be empty.");
        }
    }

    @Override
    public ResponseBuilder updateEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String endpointTemplateId, EndpointTemplate endpoint) {
        try {
            authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));

            if(endpoint.getId() != null &&  !endpointTemplateId.equals(endpoint.getId().toString())) {
                throw new BadRequestException("Endpoint template ID in request must mach ID in the path");
            }

            validator20.validateEndpointTemplateForUpdate(endpoint);

            CloudBaseUrl cloudBaseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);

            if(endpoint.isGlobal() != null) {
                cloudBaseUrl.setGlobal(endpoint.isGlobal());
            }

            if(endpoint.isDefault() != null) {
                cloudBaseUrl.setDef(endpoint.isDefault());
            }

            if(endpoint.isEnabled() != null) {
                cloudBaseUrl.setEnabled(endpoint.isEnabled());
            }

            VersionForService version = endpoint.getVersion();
            if (version != null) {
                if(StringUtils.isNotBlank(version.getId())) {
                    cloudBaseUrl.setVersionId(version.getId());
                }
                if(StringUtils.isNotBlank(version.getInfo())) {
                    cloudBaseUrl.setVersionInfo(version.getInfo());
                }
                if(StringUtils.isNotBlank(version.getList())) {
                    cloudBaseUrl.setVersionList(version.getList());
                }
            }

            endpointService.updateBaseUrl(cloudBaseUrl);

            EndpointTemplate value = this.endpointConverterCloudV20.toEndpointTemplate(cloudBaseUrl);
            return Response.ok(objFactories.getOpenStackIdentityExtKscatalogV1Factory().createEndpointTemplate(value).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role) {
        try {
            ScopeAccess tokenScopeAccess = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN);
            BaseUser caller = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            //set defaults when optional params not specified
            if (role != null && StringUtils.isBlank(role.getServiceId())) {
                role.setServiceId(config.getString("cloudAuth.globalRoles.clientId"));
            }
            if (role != null && role.isPropagate() == null) {
                role.setPropagate(Boolean.FALSE);
            }
            if (role != null && StringUtils.isBlank(role.getAdministratorRole())) {
                IdentityUserTypeEnum callerUserType = authorizationService.getIdentityTypeRoleAsEnum(caller);
                role.setAdministratorRole(callerUserType.getRoleName());
            }

            validator20.validateRoleForCreation(role);

            Application service = applicationService.checkAndGetApplication(role.getServiceId());

            ClientRole clientRole = null;
            try {
                clientRole = roleConverterCloudV20.fromRole(role, service.getClientId());
            } catch (IdmException ex) {
                if (ex.getErrorCode().equals(ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE)) {
                    throw new BadRequestException(ex.getMessage()); //translate invalid data to invalid request
                } else {
                    throw ex; //just rethrow
                }
            }
            precedenceValidator.verifyCallerRolePrecedenceForAssignment((User) caller, clientRole);

            applicationService.addClientRole(clientRole);

            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            String id = clientRole.getId();
            URI build = requestUriBuilder.path(id).build();
            Response.ResponseBuilder response = Response.created(build);
            org.openstack.docs.identity.api.v2.ObjectFactory openStackIdentityV2Factory = objFactories.getOpenStackIdentityV2Factory();
            Role value = roleConverterCloudV20.toRoleFromClientRole(clientRole);
            return response.entity(openStackIdentityV2Factory.createRole(value).getValue());
        } catch (DuplicateException bre) {
            return exceptionHandler.conflictExceptionResponse(bre.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) {
        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserManagedLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            if (authorizationService.authorizeCloudUserAdmin(scopeAccess)
                    || authorizationService.authorizeUserManageRole(scopeAccess)) {
                if (!caller.getDomainId().equals(user.getDomainId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }

            ClientRole role = checkAndGetClientRole(roleId);

            if (StringUtils.startsWithIgnoreCase(role.getName(), "identity:")) {
                throw new ForbiddenException("Cannot add specified role to tenants on users.");
            }

            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, role);

            TenantRole tenantRole = new TenantRole();
            tenantRole.setName(role.getName());
            tenantRole.setClientId(role.getClientId());
            tenantRole.setRoleRsId(role.getId());
            tenantRole.setUserId(user.getId());
            tenantRole.getTenantIds().add(tenant.getTenantId());

            tenantService.addTenantRoleToUser(user, tenantRole);

            return Response.ok();

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service) {
        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyServiceAdminLevelAccess(scopeAccess);

            if (service == null) {
                String errMsg = "service cannot be null";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(service.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(service.getType())) {
                String errMsg = "Expecting type";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            Application client = new Application();
            client.setOpenStackType(service.getType());
            client.setDescription(service.getDescription());
            client.setName(service.getName());

            this.applicationService.add(client);
            service.setId(client.getClientId());

            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            String id = service.getId();
            URI build = requestUriBuilder.path(id).build();
            org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory openStackIdentityExtKsadmnV1Factory = objFactories.getOpenStackIdentityExtKsadmnV1Factory();
            return Response.created(build).entity(openStackIdentityExtKsadmnV1Factory.createService(service).getValue());

        } catch (DuplicateException de) {
            return exceptionHandler.conflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken,
                                     org.openstack.docs.identity.api.v2.Tenant tenant) {
        try {
            final ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyIdentityAdminLevelAccess(scopeAccess);

            if (StringUtils.isBlank(tenant.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            // Our implementation has the id and the name the same
            tenant.setId(tenant.getName());
            final Tenant savedTenant = this.tenantConverterCloudV20.fromTenant(tenant);

            Domain domain;
            if (StringUtils.isBlank(tenant.getDomainId())) {
                // New tenants get added to the default domain if no domain Id is specified. Default domain MUST exist and must be enabled
                domain = domainService.getDomain(identityConfig.getReloadableConfig().getTenantDefaultDomainId());
                if (domain == null || !Boolean.TRUE.equals(domain.getEnabled())) {
                    throw new IllegalStateException("Default domain must exist and be enabled in order to create tenants");
                }
            } else {
                // Assert the domain provided exists.
                domain = domainService.getDomain(savedTenant.getDomainId());
                if (domain == null){
                    String errMsg = String.format(DOMAIN_ID_NOT_FOUND_ERROR_MESSAGE, tenant.getDomainId());
                    throw new BadRequestException(errMsg);
                }
            }
            savedTenant.setDomainId(domain.getDomainId());

            // Saves the Tenant
            this.tenantService.addTenant(savedTenant);

            //update the domain backpointer to the tenant
            domainService.addTenantToDomain(savedTenant.getTenantId(), savedTenant.getDomainId());

            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            String tenantId = savedTenant.getTenantId();
            URI build = requestUriBuilder.path(tenantId).build();
            org.openstack.docs.identity.api.v2.ObjectFactory openStackIdentityV2Factory = objFactories.getOpenStackIdentityV2Factory();
            org.openstack.docs.identity.api.v2.Tenant value = this.tenantConverterCloudV20.toTenant(savedTenant);
            return Response.created(build).entity(openStackIdentityV2Factory.createTenant(value).getValue());
        } catch (DuplicateException de) {
            return exceptionHandler.tenantConflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, org.openstack.docs.identity.api.v2.User usr) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.USER_MANAGER, null);
            User caller = (User) userService.getUserByScopeAccess(scopeAccessByAccessToken);

            //ignore the mfa attributes
            usr.setMultiFactorEnabled(null);
            usr.setUserMultiFactorEnforcementLevel(null);
            usr.setFactorType(null);

            //ignore the core contact id for users that are not service or identity admins
            if (!authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null)) {
                usr.setContactId(null);
            }

            User userForDefaults = createUserValidator.validateCreateUserAndGetUserForDefaults(usr, caller);

            User user;
            IdentityUserTypeEnum userForDefaultsUserType = authorizationService.getIdentityTypeRoleAsEnum(userForDefaults);
            if (IdentityUserTypeEnum.SERVICE_ADMIN == userForDefaultsUserType) {
                user = createIdentityAdminService.setDefaultsAndCreateUser(usr, userForDefaults);
            } else if (IdentityUserTypeEnum.IDENTITY_ADMIN == userForDefaultsUserType) {
                user = createUserAdminService.setDefaultsAndCreateUser(usr, userForDefaults);
            } else if (IdentityUserTypeEnum.USER_ADMIN == userForDefaultsUserType ||
                        IdentityUserTypeEnum.USER_MANAGER == userForDefaultsUserType) {
                user = createSubUserService.setDefaultsAndCreateUser(usr, userForDefaults);
            } else {
                //cannot create user with the given user for default
                throw new NotAuthorizedException("Cannot create user with data provided.");
            }

            org.openstack.docs.identity.api.v2.User userTO = this.userConverterCloudV20.toUser(user, true);

            if (!StringUtils.isBlank(usr.getPassword())) {
                userTO.setPassword(null);
            }

            // This hack is to ensure backward compatibility for the original create user call that did
            // not return roles or groups
            if (!CreateUserUtil.isCreateUserOneCall(usr)) {
                userTO.setRoles(null);
                userTO.setGroups(null);
            }

            ResponseBuilder builder = Response.created(uriInfo.getRequestUriBuilder().path(user.getId()).build());

            return builder.entity(userTO);

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId, UserForCreate user) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(scopeAccessByAccessToken);

            //update user call can not be used to update the domainId. Use addUserToDomain calls
            user.setDomainId(null);

            if (!authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null)) {
                user.setContactId(null);
            }

            if (user.getPassword() != null) {
                validator.validatePasswordForCreateOrUpdate(user.getPassword());
            }

            User retrievedUser = userService.checkAndGetUserById(userId);
            BaseUser caller = userService.getUserByScopeAccess(scopeAccessByAccessToken);

            boolean isDisabled = retrievedUser.isDisabled();

            if (!userId.equals(user.getId()) && user.getId() != null) {
                throw new BadRequestException(ID_MISMATCH);
            }

            if (user.getUsername() != null) {
                if(!StringUtils.equalsIgnoreCase(retrievedUser.getUsername(), user.getUsername()) && !userService.isUsernameUnique(user.getUsername())){
                    throw new DuplicateUsernameException("User with username: '" + user.getUsername() + "' already exists.");
                }
            }

            boolean isUpdatingSelf = caller.getId().equals(userId);
            boolean callerIsIdentityAdmin = authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken);
            boolean callerIsServiceAdmin = authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken);
            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken);
            boolean callerHasUserManageRole = authorizationService.authorizeUserManageRole(scopeAccessByAccessToken);
            boolean callerIsSubUser = authorizationService.authorizeCloudUser(scopeAccessByAccessToken);

            // Just identity admins and service admins can update 'tokenFormat', but only when ae tokens are enabled
            if (!(callerIsIdentityAdmin || callerIsServiceAdmin) || !identityConfig.getReloadableConfig().getFeatureAETokensDecrypt()) {
                user.setTokenFormat(null);
            }

            String domainId = user.getDomainId();
            if(StringUtils.isNotBlank(domainId)){
                Domain domain = domainService.getDomain(domainId);
                if(domain == null){
                    String errMsg = String.format("Domain %s does not exist.", domainId);
                    throw new BadRequestException(errMsg);
                }
            }


            if (!callerHasUserManageRole && authorizationService.authorizeCloudUser(scopeAccessByAccessToken)) {
                if (!caller.getId().equals(retrievedUser.getId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }

            //identity admins can not update service admin accounts
            if (callerIsIdentityAdmin && authorizationService.hasServiceAdminRole(retrievedUser)) {
                throw new ForbiddenException("Cannot update user with same or higher access level");
            }

            //sub users who are not user-managers can only update their own accounts.
            if (callerIsSubUser && !callerHasUserManageRole && !isUpdatingSelf) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            //user admins and user managers can only update accounts within their domain (this prevents userAdmins/managers from
            //updating service/identity admins)
            if (callerIsUserAdmin || callerHasUserManageRole) {
                authorizationService.verifyDomain(caller, retrievedUser);
            }

            if((callerHasUserManageRole && authorizationService.hasUserManageRole(retrievedUser) && !isUpdatingSelf) ||
                    (callerHasUserManageRole && authorizationService.hasUserAdminRole(retrievedUser))) {
                throw new ForbiddenException("Cannot update user with same or higher access level");
            }

            if (!StringUtils.isBlank(user.getUsername())) {
                validator.isUsernameValid(user.getUsername());
            }

            if (user.isEnabled() != null && !user.isEnabled() && isUpdatingSelf) {
                throw new BadRequestException("User cannot enable/disable his/her own account.");
            }

            User userDO = this.userConverterCloudV20.fromUser(user);
            if (user.isEnabled() == null) {
                userDO.setEnabled(retrievedUser.getEnabled());
            }
            if (userDO.isDisabled() && !isDisabled) {
                atomHopperClient.asyncPost(retrievedUser, AtomHopperConstants.DISABLED);
            } else if (!userDO.isDisabled() && isDisabled) {
                atomHopperClient.asyncPost(retrievedUser, AtomHopperConstants.ENABLED);
            }

            Boolean updateRegion = true;
            if (userDO.getRegion() != null && retrievedUser != null) {
                if (userDO.getRegion().equals(retrievedUser.getRegion())) {
                    updateRegion = false;
                }
            }
            userDO.setId(retrievedUser.getId());
            if (StringUtils.isBlank(user.getUsername())) {
                userDO.setUsername(retrievedUser.getUsername());
            }
            if (userDO.getRegion() != null && updateRegion) {
                defaultRegionService.validateDefaultRegion(userDO.getRegion(), retrievedUser);
            }

            userService.updateUser(userDO);

            atomHopperClient.asyncPost(userDO, AtomHopperConstants.UPDATE);

            userDO = userService.getUserById(userDO.getId());
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(userDO)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    User getUser(ScopeAccess scopeAccessByAccessToken) {
        User user = null;
        /*
         * existing code appears to have returned null if the specified token was not associated with a provisioned user
         * (e.g. was a federated
         * user, racker, or a non-user based token).
         *
         */
        if (scopeAccessByAccessToken instanceof BaseUserToken) {
            String userId = ((BaseUserToken)scopeAccessByAccessToken).getIssuedToUserId();
            user = identityUserService.getProvisionedUserById(userId);
        }

        return user;
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String userId, String body) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            JAXBElement<? extends CredentialType> credentials;

            if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                credentials = getXMLCredentials(body);
            } else {
                credentials = getJSONCredentials(body);
            }

            User user;

            if (credentials.getValue() instanceof PasswordCredentialsBase) {
                PasswordCredentialsBase userCredentials = (PasswordCredentialsBase) credentials.getValue();
                validator20.validatePasswordCredentialsForCreateOrUpdate(userCredentials);
                user = userService.checkAndGetUserById(userId);
                if (!userCredentials.getUsername().equals(user.getUsername())) {
                    String errMsg = USER_AND_USER_ID_MIS_MATCHED;
                    logger.warn(errMsg);
                    throw new BadRequestException(errMsg);
                }
                user.setUserPassword(userCredentials.getPassword());
                user.setPassword(userCredentials.getPassword());
                userService.updateUser(user);
            } else if (credentials.getValue() instanceof ApiKeyCredentials) {
                ApiKeyCredentials userCredentials = (ApiKeyCredentials) credentials.getValue();
                //TODO validate username breaks authenticate call
                validator20.validateApiKeyCredentials(userCredentials);
                user = userService.checkAndGetUserById(userId);
                if (!userCredentials.getUsername().equals(user.getUsername())) {
                    String errMsg = USER_AND_USER_ID_MIS_MATCHED;
                    logger.warn(errMsg);
                    throw new BadRequestException(errMsg);
                }
                user.setApiKey(userCredentials.getApiKey());
                userService.updateUser(user);
            }
            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            URI build = requestUriBuilder.build();
            return Response.created(build).entity(credentials.getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserManagedLevelAccess(scopeAccessByAccessToken);

            ClientRole cRole = checkAndGetClientRole(roleId);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, cRole);

            if (user.getId().equals(caller.getId())) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            checkForMultipleIdentityAccessRoles(user, cRole);

            if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken) ||
                    authorizationService.authorizeUserManageRole(scopeAccessByAccessToken)) {
                if (!caller.getDomainId().equalsIgnoreCase(user.getDomainId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }

            if(!authorizationService.hasDefaultUserRole(user) &&  cRole.getName().equalsIgnoreCase("identity:user-manage")) {
                throw new BadRequestException("Cannot add user-manage role to non default-user");
            }

            assignRoleToUser(user, cRole);
            return Response.ok();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private void assignRoleToUser(User user, ClientRole clientRole) {
        if (clientRole != null) {
            TenantRole role = new TenantRole();
            role.setClientId(clientRole.getClientId());
            role.setName(clientRole.getName());
            role.setRoleRsId(clientRole.getId());
            this.tenantService.addTenantRoleToUser(user, role);
        }
    }

    // Core Service Methods

    private AuthenticateResponse authenticateFederatedDomain(AuthenticationRequest authenticationRequest,
                                                     com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        // ToDo: Validate Domain
        if(!domain.getName().equalsIgnoreCase(GlobalConstants.RACKSPACE_DOMAIN)){
            throw new BadRequestException("Invalid domain specified");
        }// The below is only for Racker Auth for now....

        Racker racker = null;
        RackerScopeAccess rsa;
        List<String> authenticatedBy = new ArrayList<String>();
        if (authenticationRequest.getCredential().getValue() instanceof PasswordCredentialsBase) {
            PasswordCredentialsBase creds = (PasswordCredentialsBase) authenticationRequest.getCredential().getValue();
            creds.setUsername(creds.getUsername().trim());
            validator20.validatePasswordCredentials(creds);
            Domain domainDO = domainConverterCloudV20.fromDomain(domain);
            UserAuthenticationResult result = authenticationService.authenticateDomainUsernamePassword(creds.getUsername(), creds.getPassword(), domainDO);
            racker = (Racker) result.getUser();
            authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSWORD);
        } else if (authenticationRequest.getCredential().getValue() instanceof RsaCredentials) {
            RsaCredentials creds = (RsaCredentials) authenticationRequest.getCredential().getValue();
            creds.setUsername(creds.getUsername().trim());
            validator20.validateUsername(creds.getUsername());
            Domain domainDO = domainConverterCloudV20.fromDomain(domain);
            UserAuthenticationResult result = authenticationService.authenticateDomainRSA(creds.getUsername(), creds.getTokenKey(), domainDO);
            racker = (Racker) result.getUser();
            authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_RSAKEY);
        }
        rsa = scopeAccessService.getValidRackerScopeAccessForClientId(racker, identityConfig.getStaticConfig().getCloudAuthClientId(), authenticatedBy);

        //Get the roles for the racker
        List<TenantRole> roleList =  tenantService.getEphemeralRackerTenantRoles(racker.getId());
        return authConverterCloudV20.toRackerAuthenticationResponse(racker, rsa, roleList, Collections.EMPTY_LIST);
    }

    public ResponseBuilder upgradeUserToCloud(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, org.openstack.docs.identity.api.v2.User upgradeUser) {
        try {
            //verify token exists and valid
            ScopeAccess token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            User caller = (User) userService.getUserByScopeAccess(token);

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_UPGRADE_USER_TO_CLOUD.getRoleName());

            //verify the data provided to upgrade the user
            if (StringUtils.isBlank(upgradeUser.getId())) {
                throw new BadRequestException("Must specify ID of user to upgrade");
            }
            if (upgradeUser.getSecretQA() == null) {
                throw new BadRequestException("Missing secret Question and Answer");
            }
            if (upgradeUser.getSecretQA() != null) {
                if (StringUtils.isBlank(upgradeUser.getSecretQA().getQuestion())) {
                    throw new BadRequestException("Missing secret question");
                }
                if (StringUtils.isBlank(upgradeUser.getSecretQA().getAnswer())) {
                    throw new BadRequestException("Missing secret answer");
                }
            }
            if (upgradeUser.getRoles() != null) {
                for (Role role : upgradeUser.getRoles().getRole()) {
                    if (StringUtils.isBlank(role.getName())) {
                        throw new BadRequestException("Role name cannot be blank");
                    }
                    if (roleService.isIdentityAccessRole(role.getName())) {
                        throw new BadRequestException("Can not set Identity roles on upgraded user");
                    }
                    ClientRole clientRole = roleService.getRoleByName(role.getName());
                    if (clientRole == null) {
                        throw new BadRequestException(String.format("Role with name %s not found", role.getName()));
                    }
                    //need to try/catch here because we need to return a 400 for bad roles in the request, not a 403
                    try {
                        precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, clientRole);
                    } catch (ForbiddenException e) {
                        throw new BadRequestException(String.format("You do not have access to assign role %s", role.getName()));
                    }
                }
            }
            if (upgradeUser.getGroups() != null) {
                for (com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group : upgradeUser.getGroups().getGroup()) {
                    if (StringUtils.isBlank(group.getName())) {
                        throw new BadRequestException("Group name cannot be blank");
                    }
                    //have to try/catch here b/c groupService.getGroupByName breaks convention and throws an exception
                    //when the group does not exist instead of returning null
                    try {
                        groupService.getGroupByName(group.getName());
                    } catch(NotFoundException e) {
                        throw new BadRequestException(String.format("Group with name %s not found", group.getName()));
                    }
                }
            }
            defaultRegionService.validateDefaultRegion(upgradeUser.getDefaultRegion());

            //verify that the new domain ID is numeric and no domain must exist with this ID
            try {
                Integer.parseInt(upgradeUser.getDomainId());
            } catch (NumberFormatException e) {
                throw new BadRequestException(String.format("Must specify a numeric domain less than %s", Integer.MAX_VALUE));
            }
            Domain domain = domainService.getDomain(upgradeUser.getDomainId());
            if (domain != null) {
                throw new DuplicateException(String.format("Domain with ID %s already exists", domain.getDomainId()));
            }

            //verify that the mosso and nast tenants do not exist already
            Tenant mossoTenant = tenantService.getTenant(upgradeUser.getDomainId());
            if (mossoTenant != null) {
                throw new DuplicateException(String.format("The MOSSO tenant for domain %s already exists.", upgradeUser.getDomainId()));
            }
            Tenant nastTenant = tenantService.getTenant(userService.getNastTenantId(upgradeUser.getDomainId()));
            if (nastTenant != null) {
                throw new DuplicateException(String.format("The NAST tenant for domain %s already exists.", upgradeUser.getDomainId()));
            }

            User upgradedUserEntity = this.userConverterCloudV20.fromUser(upgradeUser);

            //Call the service to upgrade
            User user = userService.upgradeUserToCloud(upgradedUserEntity);

            org.openstack.docs.identity.api.v2.User userTO = this.userConverterCloudV20.toUser(user, true);

            return Response.ok(uriInfo.getRequestUriBuilder().path(user.getId()).build()).entity(userTO);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder authenticateForForgotPassword(HttpHeaders httpHeaders, ForgotPasswordCredentials forgotPasswordCredentials) {
        try {
            //this will throw exception if auth does NOT succeed
            AuthenticationRequest authRequestAdapter = objFactories.getOpenStackIdentityV2Factory().createAuthenticationRequest();
            authRequestAdapter.setCredential(raxAuthObjectFactory.createForgotPasswordCredentials(forgotPasswordCredentials));

            AuthResponseTuple auth = authWithForgotPasswordCredentials.authenticateForAuthResponse(authRequestAdapter);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder passwordReset(HttpHeaders httpHeaders, String authToken, PasswordReset passwordReset) {

        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);

            if(!(scopeAccess instanceof UserScopeAccess)) {
                throw new NotFoundException("Only provisioned users can use password reset tokens");
            }

            UserScopeAccess usa = (UserScopeAccess) scopeAccess;
            if(!TokenScopeEnum.PWD_RESET.getScope().equals(usa.getScope())) {
                throw new ForbiddenException("Please provide a valid password reset token");
            }

            User user = userService.checkAndGetUserById(usa.getUserRsId());

            validator.validatePasswordForCreateOrUpdate(passwordReset.getPassword());

            user.setUserPassword(passwordReset.getPassword());
            user.setPassword(passwordReset.getPassword());
            this.userService.updateUser(user);

            Audit.logSuccessfulPasswordResetRequest(user);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) {
        /*
         TODO: Refactor this method. It's getting messy. Wait till after MFA though to avoid making it too difficult to follow the mfa changes
        */
        try {
            AuthResponseTuple authResponseTuple;
            if (authenticationRequest.getCredential() == null && authenticationRequest.getToken() == null) {
                throw new BadRequestException("Invalid request body: unable to parse Auth data. Please review XML or JSON formatting.");
            }
            if (!StringUtils.isBlank(authenticationRequest.getTenantName()) && !StringUtils.isBlank(authenticationRequest.getTenantId())) {
                throw new BadRequestException("Invalid request. Specify tenantId OR tenantName, not both.");
            }
            // Check for domain in request
            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = authenticationRequest.getDomain();
            if(domain != null) {
                // Scoped tokens not supported here
                if (authenticationRequest.getScope() != null) {
                    throw new ForbiddenException(SETUP_MFA_SCOPE_FORBIDDEN);
                }

                AuthenticateResponse auth = authenticateFederatedDomain(authenticationRequest, domain);
                return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(auth).getValue());
            }

            if (authenticationRequest.getCredential() != null && authenticationRequest.getCredential().getValue() instanceof PasscodeCredentials) {
                // Scoped tokens not supported here
                if (authenticationRequest.getScope() != null) {
                    throw new ForbiddenException(SETUP_MFA_SCOPE_FORBIDDEN);
                }

                //performing 2 factor auth. User must supply session-id header or request is invalid
                List<String> sessionIdList = httpHeaders.getRequestHeader(MultiFactorCloud20Service.X_SESSION_ID_HEADER_NAME);
                if (CollectionUtils.isEmpty(sessionIdList) || sessionIdList.size() != 1) {
                    throw new BadRequestException("Invalid " + MultiFactorCloud20Service.X_SESSION_ID_HEADER_NAME);
                }
                try {
                    authResponseTuple = multiFactorCloud20Service.authenticateSecondFactor(sessionIdList.get(0), authenticationRequest.getCredential().getValue());
                } catch (MultiFactorNotEnabledException e) {
                    logger.warn("Request for multifactor authentication was made on account for which multifactor is not enabled", e);
                    throw new BadRequestException("Unknown credential type");
                }
            }
            else if (authenticationRequest.getToken() != null) {
                // Scoped tokens not supported here
                if (authenticationRequest.getScope() != null) {
                    throw new ForbiddenException(SETUP_MFA_SCOPE_FORBIDDEN);
                }
                authResponseTuple = authWithToken.authenticate(authenticationRequest);
            }
            else {
                boolean canUseMfaWithCredential = false;
                UserAuthenticationFactor userAuthenticationFactor = null;
                if (authenticationRequest.getCredential().getValue() instanceof PasswordCredentialsBase) {
                    userAuthenticationFactor = authWithPasswordCredentials;
                    canUseMfaWithCredential = true;
                } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                    userAuthenticationFactor = authWithApiKeyCredentials;
                }
                else {
                    throw new BadRequestException("Unknown credential type");
                }

                UserAuthenticationResult authResult = userAuthenticationFactor.authenticate(authenticationRequest);

                if (canUseMfaWithCredential && ((User)authResult.getUser()).isMultiFactorEnabled()) {
                    // Scoped tokens not supported here
                    if (authenticationRequest.getScope() != null) {
                        throw new ForbiddenException(SETUP_MFA_SCOPE_FORBIDDEN);
                    }

                    //only perform MFA challenge when MFA is enabled, the user has mfa enabled, and user is using a credential that is protected by mfa (password for now)
                    return multiFactorCloud20Service.performMultiFactorChallenge((User) authResult.getUser(), authResult.getAuthenticatedBy());
                } else {
                    /*
                    user requested "regular" token and provided correct initial credentials. Make sure they are not required to use MFA
                    for that credential before issuing token
                     */
                    if (authenticationRequest.getScope() == null && canUseMfaWithCredential) {
                        checkMfaEnforcement(authResult.getUser());
                    }
                    else if (authenticationRequest.getScope() != null) {
                        // If the auth request is Scoped to SETUP-MFA then we need to check the enforcement level
                        // on the user and the domain to make sure they require a scoped SETUP-MFA token before
                        // issuing one. If not required, we'll throw a FORBIDDEN EXCEPTION.
                        checkIfSetupMfaScopeAllowed(authResult.getUser());
                    }
                    authResponseTuple = scopeAccessService.createScopeAccessForUserAuthenticationResult(authResult);
                }
            }
            AuthenticateResponse auth = authenticateResponseService.buildAuthResponseForAuthenticate(authResponseTuple, authenticationRequest);
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(auth).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder authenticateFederated(HttpHeaders httpHeaders, byte[] samlResponseBytes) {
        try {
            org.opensaml.saml2.core.Response samlResponse = samlUnmarshaller.unmarshallResponse(samlResponseBytes);
            SamlAuthResponse samlAuthResponse = federatedIdentityService.processSamlResponse(samlResponse);
            AuthenticateResponse response = authConverterCloudV20.toAuthenticationResponse(samlAuthResponse);
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(response).getValue());
        } catch (BadRequestException ex) {
            logger.debug("Received invalid Federation auth request", ex);
            return exceptionHandler.exceptionResponse(ex);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder logoutFederatedUser(HttpHeaders httpHeaders, byte[] samlLogoutRequestBytes) {
        SamlLogoutResponse logoutResponse = null;
        try {
            org.opensaml.saml2.core.LogoutRequest logoutRequest = samlUnmarshaller.unmarshallLogoutRequest(samlLogoutRequestBytes);
            logoutResponse = federatedIdentityService.processLogoutRequest(logoutRequest);
        } catch (BadRequestException ex) {
            logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(null, StatusCode.REQUESTER_URI, ex.getMessage(), ex);
        } catch (Exception ex) {
            logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(null, StatusCode.RESPONDER_URI, "Encountered an exception processing federation logout request", ex);
        }

        try {
            int status = HttpStatus.SC_OK;
            if (logoutResponse.hasException()) {
                logger.error("Encountered exception processing federation logout request", logoutResponse.getExceptionThrown());
                status = exceptionHandler.exceptionToHttpStatus(logoutResponse.getExceptionThrown());
            }

            return buildFedLogoutResponseBuilder(logoutResponse.getLogoutResponse(), status);
        } catch (Exception e) {
            logger.debug("Error generating error output. Returning 500", e);
            return exceptionHandler.exceptionResponse(e);
        }
    }

    private ResponseBuilder buildFedLogoutResponseBuilder(LogoutResponse logoutResponse, int status) throws MarshallingException {
        Element element = marshaller.marshall(logoutResponse);;
        return Response.status(status).entity(new GenericEntity<DOMSource>(new DOMSource(element), DOMSource.class));
    }

    @Override
    public ResponseBuilder addIdentityProvider(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, IdentityProvider provider) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName());

            validator20.validateIdentityProviderForCreation(provider);

            com.rackspace.idm.domain.entity.IdentityProvider newProvider = identityProviderConverterCloudV20.fromIdentityProvider(provider);
            federatedIdentityService.addIdentityProvider(newProvider);
            ResponseBuilder builder = Response.created(uriInfo.getRequestUriBuilder().path(newProvider.getName()).build());
            return builder.entity(identityProviderConverterCloudV20.toIdentityProvider(newProvider));
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateIdentityProvider(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String providerId, IdentityProvider provider) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName());

            com.rackspace.idm.domain.entity.IdentityProvider existingProvider = federatedIdentityService.checkAndGetIdentityProvider(providerId);

            validator20.validateIdentityProviderForUpdate(provider);
            existingProvider.setAuthenticationUrl(provider.getAuthenticationUrl()); //copy over the only attribute allowed to be updated

            federatedIdentityService.updateIdentityProvider(existingProvider); //update
            ResponseBuilder builder = Response.ok(uriInfo.getRequestUriBuilder().path(existingProvider.getName()).build());
            return builder.entity(identityProviderConverterCloudV20.toIdentityProvider(existingProvider));
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getIdentityProvider(HttpHeaders httpHeaders, String authToken, String providerId) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                    IdentityRole.IDENTITY_PROVIDER_READ_ONLY.getRoleName()));

            com.rackspace.idm.domain.entity.IdentityProvider provider = federatedIdentityService.checkAndGetIdentityProvider(providerId);

            return Response.ok(objFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProvider(identityProviderConverterCloudV20.toIdentityProvider(provider)).getValue());
        } catch (SizeLimitExceededException ex) {
            throw new BadRequestException(ex.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getIdentityProviders(HttpHeaders httpHeaders, String authToken, String approvedDomainId, String approvedTenantId, String idpType) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                    IdentityRole.IDENTITY_PROVIDER_READ_ONLY.getRoleName()));

            IdentityProviderTypeFilterEnum idpFilter = null;
            if (StringUtils.isNotBlank(idpType)) {
                idpFilter = IdentityProviderTypeFilterEnum.parseIdpTypeFilter(idpType);
                if(idpFilter == null) {
                    throw new BadRequestException(String.format(FEDERATION_IDP_TYPE_ERROR_MESSAGE, IdentityProviderTypeFilterEnum.EXPLICIT.name()));
                }
            }

            //prevent use of domain and tenant filter at the same time
            if (StringUtils.isNotBlank(approvedDomainId) && StringUtils.isNotBlank(approvedTenantId)) {
                throw new BadRequestException(FEDERATION_IDP_FILTER_CONFLICT_ERROR_MESSAGE);
            }

            if (StringUtils.isNotBlank(approvedTenantId)) {
                //verify that the tenant exists if trying to filter by tenant
                Tenant tenantForFilter = tenantService.getTenant(approvedTenantId);
                //return empty list if the tenant does not exist
                if (tenantForFilter == null) {
                    return Response.ok(objFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProviders(identityProviderConverterCloudV20.toIdentityProviderList(new ArrayList<com.rackspace.idm.domain.entity.IdentityProvider>())).getValue());
                }
                //do not allow for tenants assigned to the default domain to be used in the filter
                if(StringUtils.isBlank(tenantForFilter.getDomainId()) ||
                        identityConfig.getReloadableConfig().getTenantDefaultDomainId().equals(tenantForFilter.getDomainId())) {
                    throw new BadRequestException(FEDERATION_IDP_FILTER_TENANT_NO_DOMAIN_ERROR_MESSAGE);
                }
                approvedDomainId = tenantForFilter.getDomainId();
            }

            //return an empty list if trying to filter by a domain that does not exist
            if (StringUtils.isNotBlank(approvedDomainId)) {
                Domain domain = domainService.getDomain(approvedDomainId);
                if (domain == null) {
                    return Response.ok(objFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProviders(identityProviderConverterCloudV20.toIdentityProviderList(new ArrayList<com.rackspace.idm.domain.entity.IdentityProvider>())).getValue());
                }
            }

            List<com.rackspace.idm.domain.entity.IdentityProvider> providerEntities;
            if (StringUtils.isNotBlank(approvedDomainId) && IdentityProviderTypeFilterEnum.EXPLICIT.equals(idpFilter)) {
                providerEntities = federatedIdentityService.findIdentityProvidersExplicitlyApprovedForDomain(approvedDomainId);
            } else if (StringUtils.isNotBlank(approvedDomainId)) {
                providerEntities = federatedIdentityService.findIdentityProvidersApprovedForDomain(approvedDomainId);
            } else if (IdentityProviderTypeFilterEnum.EXPLICIT.equals(idpFilter)) {
                providerEntities = federatedIdentityService.findIdentityProvidersExplicitlyApprovedForAnyDomain();
            } else {
                providerEntities = federatedIdentityService.findAllIdentityProviders();
            }

            return Response.ok(objFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProviders(identityProviderConverterCloudV20.toIdentityProviderList(providerEntities)).getValue());
        } catch (SizeLimitExceededException ex) {
            return exceptionHandler.exceptionResponse(new ForbiddenException(ex.getMessage())); //translate size limit to forbidden
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteIdentityProvider(HttpHeaders httpHeaders, String authToken, String providerId) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName());

            federatedIdentityService.deleteIdentityProviderById(providerId);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addIdentityProviderCert(HttpHeaders httpHeaders, String authToken, String identityProviderId, PublicCertificate publicCertificate) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName());

            //load the IDP, return 404 is does not exist
            com.rackspace.idm.domain.entity.IdentityProvider provider = federatedIdentityService.checkAndGetIdentityProvider(identityProviderId);

            validator20.validatePublicCertificateForIdentityProvider(publicCertificate, provider);

            //set the cert on the provider and save
            byte[] certBytes = Base64.decodeBase64(publicCertificate.getPemEncoded());
            if(provider.getUserCertificates() == null) {
                provider.setUserCertificates(new ArrayList<byte[]>());
            }
            provider.getUserCertificates().add(certBytes);
            federatedIdentityService.updateIdentityProvider(provider);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteIdentityProviderCert(HttpHeaders httpHeaders, String authToken, String identityProviderId, String certificateId) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName());

            if (StringUtils.isEmpty(certificateId)) {
                throw new BadRequestException("Bad certificate id");
            }

            //load the IDP, return 404 is does not exist
            com.rackspace.idm.domain.entity.IdentityProvider provider = federatedIdentityService.checkAndGetIdentityProvider(identityProviderId);

            //verify that the cert exists on the IDP
            boolean certExists = false;
            byte[] matchingCert = null;
            for (byte[] cert : provider.getUserCertificates()) {
                if (certificateId.equals(DigestUtils.sha1Hex(cert))) {
                    certExists = true;
                    matchingCert = cert;
                }
            }
            if (!certExists) {
                throw new NotFoundException("Certificate does not exist on identity provider");
            }

            provider.getUserCertificates().remove(matchingCert);
            federatedIdentityService.updateIdentityProvider(provider);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, String endpointId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Tenant tenant = tenantService.checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(endpointId);
            String baseUrlId = String.valueOf(baseUrl.getBaseUrlId());
            tenant.getBaseUrlIds().remove(baseUrlId);
            tenant.getV1Defaults().remove(baseUrlId);
            tenantService.updateTenant(tenant);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);

            if (baseUrl.getEnabled() || !tenantService.getTenantsForEndpoint(endpointTemplateId).isEmpty()) {
                logger.warn(ERROR_CANNOT_DELETE_ENDPOINT_TEMPLATE_MESSAGE);
                throw new ForbiddenException(ERROR_CANNOT_DELETE_ENDPOINT_TEMPLATE_MESSAGE);
            }
            this.endpointService.deleteBaseUrl(baseUrl.getBaseUrlId());
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            if (roleId == null) {
                throw new BadRequestException("roleId cannot be null");
            }

            ClientRole role = checkAndGetClientRole(roleId);
            if (StringUtils.startsWithIgnoreCase(role.getName(), "identity:")) {
                throw new ForbiddenException("role cannot be deleted");
            }

            User caller = userService.getUserByAuthToken(authToken);

            precedenceValidator.verifyCallerRolePrecedence(caller, role);

            this.applicationService.deleteClientRole(role);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) {
        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserManagedLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            if (authorizationService.authorizeCloudUserAdmin(scopeAccess)
                    || authorizationService.authorizeUserManageRole(scopeAccess)) {
                if (!caller.getDomainId().equals(user.getDomainId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }

            ClientRole role = checkAndGetClientRole(roleId);

            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, role);

            TenantRole tenantRole = tenantService.checkAndGetTenantRoleForUserById(user, roleId);

            if(!tenantRole.getTenantIds().contains(tenant.getTenantId())) {
                throw new NotFoundException(String.format("Role %s not associated with Tenant %s on user %s.", roleId, tenantId, userId));
            }

            this.tenantService.deleteTenantOnRoleForUser(user, tenantRole, tenant);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId) {
        try {
            authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Application client = applicationService.checkAndGetApplication(serviceId);
            this.applicationService.delete(client.getClientId());
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Tenant tenant = tenantService.checkAndGetTenant(tenantId);
            tenantService.deleteTenant(tenant);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserManagedLevelAccess(scopeAccessByAccessToken);
            User user = userService.checkAndGetUserById(userId);
            //is same domain?
            boolean callerHasUserManageRole = authorizationService.authorizeUserManageRole(scopeAccessByAccessToken);
            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken);

            if (callerIsUserAdmin || callerHasUserManageRole) {
                User caller = userService.getUserByAuthToken(authToken);
                authorizationService.verifyDomain(caller, user);
            }
            if (authorizationService.hasUserAdminRole(user) && userService.hasSubUsers(userId)) {
                throw new BadRequestException("Please delete sub-users before deleting last user-admin for the account");
            }
            if(callerHasUserManageRole && authorizationService.hasUserManageRole(user)) {
                throw new NotAuthorizedException("Cannot delete user with same access level");
            }
            userService.deleteUser(user);

            atomHopperClient.asyncPost(user, AtomHopperConstants.DELETED);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(scopeAccessByAccessToken);

            if (credentialType.equals(JSONConstants.PASSWORD_CREDENTIALS)) {
                IdentityFault fault = new IdentityFault();
                fault.setCode(HttpServletResponse.SC_NOT_IMPLEMENTED);
                fault.setMessage("Method not implemented");
                return Response.ok(
                        objFactories.getOpenStackIdentityV2Factory().createIdentityFault(fault))
                        .status(HttpServletResponse.SC_NOT_IMPLEMENTED);
            }

            if (!credentialType.equals(JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)) {
                throw new BadRequestException("unsupported credential type");
            }

            if (identityConfig.getReloadableConfig().preventRackerImpersonationApiKeyAccess() &&
                    requestContextHolder.getRequestContext().getSecurityContext().isRackerImpersonatedRequest()) {
                throw new ForbiddenException("Impersonation tokens cannot be used to delete user API key credentials");
            }

            User user = userService.checkAndGetUserById(userId);

            if (StringUtils.isEmpty(user.getApiKey())) {
                throw new NotFoundException("Credential type RAX-KSKEY:apiKeyCredentials was not found for User with Id: " + user.getId());
            }

            User caller = userService.getUserByAuthToken(authToken);

            boolean isSelfDelete = caller.getId().equals(user.getId());

            boolean callerIsServiceAdmin = authorizationService.hasServiceAdminRole(caller);
            boolean callerIsIdentityAdmin = authorizationService.hasIdentityAdminRole(caller);
            boolean callerIsUserAdmin = authorizationService.hasUserAdminRole(caller);
            boolean callerHasUserManageRole = authorizationService.hasUserManageRole(caller);

            boolean userIsServiceAdmin = authorizationService.hasServiceAdminRole(user);
            boolean userIsIdentityAdmin = authorizationService.hasIdentityAdminRole(user);
            boolean userIsUserAdmin = authorizationService.hasUserAdminRole(user);
            boolean userHasUserManage = authorizationService.hasUserManageRole(user);

            boolean authorized = false;

            // This will throw a Forbidden Exception if the caller is not a the same
            // level or above the user being modified except in the case of self delete.
            if (!isSelfDelete) {
                precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            }

            if (isSelfDelete) {
                // All users can delete API Key Credentials from themselves
                // EXCEPT for identity:user-admins
                authorized = !userIsUserAdmin;
            } else if (callerIsServiceAdmin) {
                // identity:service-admin can delete API Key Credential from all users
                // EXCEPT other identity:service-admin
                authorized = !userIsServiceAdmin;
            } else if (callerIsIdentityAdmin) {
                // identity:admin can delete API KEy Credentials from all users
                // EXCEPT for identity:service-admins and other identity:admins
                authorized = !userIsIdentityAdmin;
            } else if (callerIsUserAdmin) {
                // identity:user-admin can delete API Key Credentials from identity:user-manage
                // and identity:default users within their domain.
                authorized = !userIsUserAdmin && caller.getDomainId().equals(user.getDomainId());
            } else if (callerHasUserManageRole) {
                // identity:user-manage can delete API Key Credentails from identity:default
                // users within their domain.
                authorized = !userHasUserManage && caller.getDomainId().equals(user.getDomainId());
            }

            if (!authorized) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            user.setApiKey("");

            userService.updateUser(user);

            return Response.noContent();

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserManagedLevelAccess(scopeAccessByAccessToken);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            if (user.getId().equals(caller.getId())) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken) ||
                    authorizationService.authorizeUserManageRole(scopeAccessByAccessToken)) {
                if (!caller.getDomainId().equals(user.getDomainId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }

            List<TenantRole> globalRoles = this.tenantService.getGlobalRolesForUser(user);
            TenantRole role = null;
            for (TenantRole globalRole : globalRoles) {
                if (globalRole.getRoleRsId().equals(roleId)) {
                    role = globalRole;
                }
            }

            if (role == null) {
                String errMsg = String.format("Role %s not found for user %s", roleId, userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecedence(caller, role);

            //the only user-type role you can delete on a user is the "identity:user-manage" role
            IdentityUserTypeEnum userTypeEnum = IdentityUserTypeEnum.fromRoleName(role.getName());
            if (identityConfig.getReloadableConfig().isDeleteIdentityAccessRolePreventionEnabled() &&
                    userTypeEnum != null && userTypeEnum != IdentityUserTypeEnum.USER_MANAGER) {
                throw new ForbiddenException(ERROR_CANNOT_DELETE_USER_TYPE_ROLE_MESSAGE);
            }

            this.tenantService.deleteTenantRoleForUser(user, role);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, String endpointId) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            if (!tenant.getBaseUrlIds().contains(endpointId)) {
                String errMsg = String.format("Tenant %s does not have endpoint %s", tenantId, endpointId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(endpointId);

            return Response.ok(
                    objFactories.getOpenStackIdentityV2Factory().createEndpoint(endpointConverterCloudV20.toEndpoint(baseUrl)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);

            return Response.ok(objFactories.getOpenStackIdentityExtKscatalogV1Factory()
                    .createEndpointTemplate(this.endpointConverterCloudV20.toEndpointTemplate(baseUrl)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) {
        try {
            if (StringUtils.isBlank(alias)) {
                throw new BadRequestException("Invalid extension alias '" + alias + "'.");
            }

            final String normalizedAlias = alias.trim().toUpperCase();

            if (extensionMap == null) {
                extensionMap = new HashMap<String, JAXBElement<Extension>>();

                if (currentExtensions == null) {
                    JAXBContext jaxbContext = JAXBContextResolver.get();
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
                    StreamSource ss = new StreamSource(is);
                    currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
                }

                Extensions exts = currentExtensions.getValue();

                for (Extension e : exts.getExtension()) {
                    extensionMap.put(e.getAlias().trim().toUpperCase(), objFactories.getOpenStackCommonV1Factory().createExtension(e));
                }
            }

            if (!extensionMap.containsKey(normalizedAlias)) {
                throw new NotFoundException("Extension with alias '" + normalizedAlias + "' is not available.");
            }

            return Response.ok(extensionMap.get(normalizedAlias).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId) {
        try {
            ScopeAccess callersScopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserManagedLevelAccess(callersScopeAccess);
            User caller = getUser(callersScopeAccess);
            ClientRole userIdentityRole = applicationService.getUserIdentityRole(caller);

            ClientRole role = checkAndGetClientRole(roleId);
            if(userIdentityRole == null || userIdentityRole.getRsWeight() > role.getRsWeight()) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            return Response.ok(objFactories.getOpenStackIdentityV2Factory()
                    .createRole(this.roleConverterCloudV20.toRoleFromClientRole(role)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getSecretQA(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            User user = userService.checkAndGetUserById(userId);
            SecretQA secrets = objFactories.getRackspaceIdentityExtKsqaV1Factory().createSecretQA();

            secrets.setAnswer(user.getSecretAnswer());
            secrets.setQuestion(user.getSecretQuestion());
            return Response.ok(objFactories.getRackspaceIdentityExtKsqaV1Factory().createSecretQA(secrets).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Application client = applicationService.checkAndGetApplication(serviceId);
            return Response.ok(objFactories.getOpenStackIdentityExtKsadmnV1Factory().createService(serviceConverterCloudV20.toService(client)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Tenant tenant = tenantService.checkAndGetTenant(tenantsId);
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createTenant(tenantConverterCloudV20.toTenant(tenant)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.getTenantByName(name);
            if (tenant == null) {
                String errMsg = String.format("Tenant with id/name: '%s' was not found.", name);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            return Response.ok(objFactories.getOpenStackIdentityV2Factory()
                    .createTenant(this.tenantConverterCloudV20.toTenant(tenant)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);

            //legacy code assumed User and did not check for disabled. We're expanding to Fed, and still won't check for disabled
            EndUser caller = (EndUser) userService.getUserByScopeAccess(scopeAccessByAccessToken, false);

            //if caller has default user role
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken) &&
                    !authorizationService.hasUserManageRole(caller)) {
                if (caller.getId().equals(userId)) {
                    return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUser(this.userConverterCloudV20.toUser(caller)).getValue());
                } else {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }
            authorizationService.verifyUserManagedLevelAccess(scopeAccessByAccessToken);
            EndUser user = this.identityUserService.getEndUserById(userId);
            if (user == null) {
                String errMsg = String.format("User with id: '%s' was not found", userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }
            setEmptyUserValues(user);
            if (authorizationService.authorizeUserManageRole(scopeAccessByAccessToken) ||
                    authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
                authorizationService.verifyDomain(caller, user);
            }
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUser(this.userConverterCloudV20.toUser(user)).getValue());
        } catch (Exception ex) {
            Exception e = ex;
            if (ex instanceof ForbiddenException || ex instanceof NotFoundException) {
                // [B-82794] Modify [Get User] API Error Message
                logger.warn("Obfuscated exception", ex);
                e = new NotFoundException("User not found");
            }
            return exceptionHandler.exceptionResponse(e);
        }
    }

    void setEmptyUserValues(EndUser user) {
        if (StringUtils.isEmpty(user.getEmail())) {
            user.setEmail("");
        }
        if (StringUtils.isEmpty(user.getRegion())) {
            user.setRegion("");
        }
        if (StringUtils.isEmpty(user.getDomainId())) {
            user.setDomainId("");
        }
    }

    @Override
    public ResponseBuilder getUserByTenantId(HttpHeaders httpHeaders, String authToken, String tenantId) {
        try {
            if (identityConfig.getReloadableConfig().getV11LegacyEnabled()) {
                final ScopeAccess requesterScopeAccess = getScopeAccessForValidToken(authToken);
                authorizationService.verifyIdentityAdminLevelAccess(requesterScopeAccess);

                final User user = userService.getUserByTenantId(tenantId);
                if (user == null) {
                    throw new NotFoundException(String.format("User with tenantId %s not found", tenantId));
                }

                final org.openstack.docs.identity.api.v2.User jaxbUser = userConverterCloudV20.toUser(user);
                if (user.getNastId() != null) {
                    jaxbUser.getOtherAttributes().put(new QName(V11_API_QNAME, "nastId"), user.getNastId());
                }
                if (user.getMossoId() != null) {
                    jaxbUser.getOtherAttributes().put(new QName(V11_API_QNAME, "mossoId"), String.valueOf(user.getMossoId()));
                }

                return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUser(jaxbUser).getValue());
            } else {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE);
            }
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name) {
        try {
            ScopeAccess requesterScopeAccess = getScopeAccessForValidToken(authToken);

            authorizationService.verifyUserLevelAccess(requesterScopeAccess);

            User user = userService.getUser(name);

            if (user == null) {
                String errMsg = String.format("User not found: '%s'", name);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            User requester = (User) userService.getUserByScopeAccess(requesterScopeAccess);

            if (authorizationService.authorizeUserManageRole(requesterScopeAccess) ||
                    authorizationService.authorizeCloudUserAdmin(requesterScopeAccess)) {
                authorizationService.verifyDomain(requester, user);
            } else if (authorizationService.authorizeCloudUser(requesterScopeAccess)) {
                authorizationService.verifySelf(requester, user);
            }

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(user)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUsersByEmail(HttpHeaders httpHeaders, String authToken, String email) {
        try {
            ScopeAccess requesterScopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserManagedLevelAccess(requesterScopeAccess);

            Iterable<User> users = userService.getUsersByEmail(email);

            User caller = (User) userService.getUserByScopeAccess(requesterScopeAccess);
            if (authorizationService.authorizeUserManageRole(requesterScopeAccess) ||
                     authorizationService.authorizeCloudUserAdmin(requesterScopeAccess)) {
                users = filterUsersInDomain(users, caller);
            }

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(users)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private List<User> filterUsersInDomain(Iterable<User> users, User caller) {
        List<User> result = new ArrayList<User>();
        for (User user : users) {
            if (authorizationService.hasSameDomain(caller, user)) {
                result.add(user);
            }
        }
        return result;
    }

    @Override
    public ResponseBuilder getUserPasswordCredentials(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            getScopeAccessForValidToken(authToken);

            throw new ForbiddenException(NOT_AUTHORIZED);

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(scopeAccessByAccessToken);

            if (identityConfig.getReloadableConfig().preventRackerImpersonationApiKeyAccess() &&
                    requestContextHolder.getRequestContext().getSecurityContext().isRackerImpersonatedRequest()) {
                throw new ForbiddenException("Impersonation tokens cannot be used to get user API key credentials");
            }

            User user = this.userService.checkAndGetUserById(userId);
            User caller = getUser(scopeAccessByAccessToken);

            if (user == null) {
                String errMsg = String.format("User with id: %s does not exist", userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            if(!authorizationService.isSelf(caller, user)){
                precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);

                if(!(authorizationService.hasIdentityAdminRole(caller) || authorizationService.hasServiceAdminRole(caller))){
                    authorizationService.verifyDomain(caller, user);
                }
            }

            if (StringUtils.isBlank(user.getApiKey())) {
                throw new NotFoundException("User doesn't have api key credentials");
            }
            ApiKeyCredentials userCreds = new ApiKeyCredentials();
            userCreds.setApiKey(user.getApiKey());
            userCreds.setUsername(user.getUsername());
            JAXBElement<? extends CredentialType> creds = objFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(userCreds);

            return Response.ok(creds.getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            User user = userService.checkAndGetUserById(userId);

            List<TenantRole> globalRoles = tenantService.getGlobalRolesForUser(user);

            TenantRole role = null;

            for (TenantRole globalRole : globalRoles) {
                if (globalRole.getRoleRsId().equals(roleId)) {
                    role = globalRole;
                }
            }

            if (role == null) {
                String errMsg = String.format("Role %s not found for user %s", roleId, userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            ClientRole cRole = checkAndGetClientRole(roleId);

            role.setDescription(cRole.getDescription());
            role.setName(cRole.getName());

            return Response.ok(objFactories.getOpenStackIdentityV2Factory()
                    .createRole(this.roleConverterCloudV20.toRole(role)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, Integer marker, Integer limit) {
        try {
            ScopeAccess callerScopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(callerScopeAccess);

            if (identityConfig.getReloadableConfig().preventRackerImpersonationApiKeyAccess() &&
                    requestContextHolder.getRequestContext().getSecurityContext().isRackerImpersonatedRequest()) {
                throw new ForbiddenException("Impersonation tokens cannot be used to view user credentials");
            }

            User user = userService.checkAndGetUserById(userId);
            User caller = (User) userService.getUserByScopeAccess(callerScopeAccess);

            if(!authorizationService.isSelf(caller, user)){
                precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);

                if(!(authorizationService.hasIdentityAdminRole(caller) || authorizationService.hasServiceAdminRole(caller))){
                    authorizationService.verifyDomain(caller, user);
                }
            }

            CredentialListType creds = objFactories.getOpenStackIdentityV2Factory().createCredentialListType();

            if (!StringUtils.isBlank(user.getApiKey())) {
                ApiKeyCredentials userCreds = new ApiKeyCredentials();
                userCreds.setApiKey(user.getApiKey());
                userCreds.setUsername(user.getUsername());
                creds.getCredential().add(objFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(userCreds));
            }

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createCredentials(creds).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    boolean isUserAdmin(User user) {
        List<TenantRole> tenantRoleList = tenantService.getTenantRolesForUser(user);
        boolean hasRole = false;
        for (TenantRole tenantRole : tenantRoleList) {
            String name = tenantRole.getName();
            if (name.equals("identity:user-admin")) {
                hasRole = true;
            }
        }
        return hasRole;
    }

    boolean isDefaultUser(User user) {
        List<TenantRole> tenantRoleList = tenantService.getTenantRolesForUser(user);
        boolean hasRole = false;
        for (TenantRole tenantRole : tenantRoleList) {
            String name = tenantRole.getName();
            if (name.equals("identity:default")) {
                hasRole = true;
            }
        }
        return hasRole;
    }

    @Override
    public ResponseBuilder listEndpoints(HttpHeaders httpHeaders, String authToken, String tenantId) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            List<CloudBaseUrl> baseUrls = this.endpointService.getGlobalBaseUrls();
            Collection<String> globalBaseUrlIds = org.apache.commons.collections4.CollectionUtils.collect(baseUrls, new Transformer<CloudBaseUrl, String>() {
                @Override
                public String transform(CloudBaseUrl input) {
                    return input.getBaseUrlId();
                }
            });
            if (tenant.getBaseUrlIds() != null) {
                for (String id : tenant.getBaseUrlIds()) {
                    Integer baseUrlId = Integer.parseInt(id);
                    //do not add a base URL to the list if it is already in the list b/c it is global
                    if (!globalBaseUrlIds.contains(id)) {
                        baseUrls.add(this.endpointService.getBaseUrlById(Integer.toString(baseUrlId)));
                    }
                }
            }
            return Response.ok(objFactories.getOpenStackIdentityV2Factory()
                    .createEndpoints(this.endpointConverterCloudV20.toEndpointListFromBaseUrls(baseUrls)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId) {

        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            boolean allowForSameToken = identityConfig.getReloadableConfig().isFeatureListEndpointsForOwnTokenEnabled();
            boolean sameToken = StringUtils.equals(authToken, tokenId);

            //skip authorization checks if the token being used is the same token to list endpionts for
            if (!(allowForSameToken && sameToken)) {
                authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.GET_TOKEN_ENDPOINTS_GLOBAL.getRoleName());
            }

            ScopeAccess sa = checkAndGetToken(tokenId);
            if (sa instanceof ImpersonatedScopeAccess) {
                ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) sa;
                String impersonatedTokenId = impersonatedScopeAccess.getImpersonatingToken();
                sa = scopeAccessService.getScopeAccessByAccessToken(impersonatedTokenId);
            }

            //do not allow for any type of scoped token to have endpoints listed for it
            if(sa == null || StringUtils.isNotBlank(sa.getScope())) {
                throw new NotFoundException("Valid token not found");
            }

            BaseUser baseUser = userService.getUserByScopeAccess(sa, false);
            ServiceCatalogInfo scInfo = scopeAccessService.getServiceCatalogInfo(baseUser);

            EndpointList list;
            if (authorizationService.restrictTokenEndpoints(scInfo)) {
                //terminator is in effect. All tenants disabled so blank endpoint list
                list = objFactories.getOpenStackIdentityV2Factory().createEndpointList();
            } else {
                list = endpointConverterCloudV20.toEndpointList(scInfo.getUserEndpoints());
            }

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createEndpoints(list).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }


    @Override
    public ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders, String authToken, String serviceId) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            List<CloudBaseUrl> baseUrls;

            if (StringUtils.isBlank(serviceId)) {
                baseUrls = new ArrayList<CloudBaseUrl>();
                for (CloudBaseUrl cloudBaseUrl : this.endpointService.getBaseUrls()) {
                    baseUrls.add(cloudBaseUrl);
                }
            } else {
                Application client = applicationService.checkAndGetApplication(serviceId);
                baseUrls = this.endpointService.getBaseUrlsByServiceType(client.getOpenStackType());
            }

            return Response.ok(
                    objFactories.getOpenStackIdentityExtKscatalogV1Factory()
                            .createEndpointTemplates(endpointConverterCloudV20.toEndpointTemplateList(baseUrls)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders) {
        try {
            if (currentExtensions == null) {
                JAXBContext jaxbContext = JAXBContextResolver.get();
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
                StreamSource ss = new StreamSource(is);

                currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
            }
            return Response.ok(currentExtensions.getValue());
        } catch (Exception e) {
            // Return 500 error. Is WEB-IN/extensions.xml malformed?
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String serviceId, String roleName, Integer marker, Integer limit) {
        try {
            authorizationService.verifyUserManagedLevelAccess(getScopeAccessForValidToken(authToken));

            PaginatorContext<ClientRole> context;
            User caller = userService.getUserByAuthToken(authToken);
            ClientRole userIdentityRole = applicationService.getUserIdentityRole(caller);

            if (StringUtils.isNotBlank(serviceId) && StringUtils.isNotBlank(roleName)) {
                throw new BadRequestException("Cannot specify serviceId and roleName together");
            }

            if (StringUtils.isNotBlank(serviceId)) {
                context = this.applicationService.getAvailableClientRolesPaged(serviceId, marker, limit, userIdentityRole.getRsWeight());
            } else if (StringUtils.isNotBlank(roleName)) {
                context = applicationService.getAvailableClientRolesByName(roleName, userIdentityRole.getRsWeight(), marker, limit);
            } else {
                context = this.applicationService.getAvailableClientRolesPaged(marker, limit, userIdentityRole.getRsWeight());
            }

            String linkHeader = applicationRolePaginator.createLinkHeader(uriInfo, context);

            return Response.status(200)
                    .header("Link", linkHeader)
                    .entity(objFactories.getOpenStackIdentityV2Factory()
                            .createRoles(roleConverterCloudV20.toRoleListFromClientRoles(context.getValueList())).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, Integer marker, Integer limit) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            List<TenantRole> roles = this.tenantService.getTenantRolesForTenant(tenant.getTenantId());

            return Response.ok(
                    objFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                    String userId) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            boolean self = caller.getId().equals(user.getId());

            if (!self) {
                precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            }

            List<TenantRole> roles = this.tenantService.getTenantRolesForUserOnTenant(user, tenant);

            return Response.ok(
                    objFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }

    }

    // KSADM Extension Role Methods

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String name, Integer marker, Integer limit) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Iterable<Application> clients;

            if (StringUtils.isNotBlank(name)) {
                Application service;
                try {
                    service = this.applicationService.getByName(name);
                } catch (IllegalStateException e) {
                    if (e.getCause() != null && e.getCause() instanceof LDAPException) {
                        throw new UnrecoverableIdmException(DUPLICATE_SERVICE_ERROR_MESSAGE, e);
                    }
                    throw e;
                }
                clients = new ArrayList<>();
                if (service != null) {
                    clients = Arrays.asList(service);
                }
            } else {
                clients = this.applicationService.getOpenStackServices();
            }

            return Response.ok(
                    objFactories.getOpenStackIdentityExtKsadmnV1Factory().createServices(serviceConverterCloudV20.toServiceList(clients)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, Integer marker, Integer limit) {
        try {
            ScopeAccess access = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(access);

            List<Tenant> tenants = null;

            //safe cast as only enduser would pass verify check
            EndUser user = (EndUser) userService.getUserByScopeAccess(access);

            tenants = this.tenantService.getTenantsForUserByTenantRoles(user);

            return Response.ok(
                    objFactories.getOpenStackIdentityV2Factory().createTenants(tenantConverterCloudV20.toTenantList(tenants)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            ScopeAccess callersScopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(callersScopeAccess);
            User user = this.userService.getUserById(userId);
            if (user == null) {
                String errMsg = String.format(USER_NOT_FOUND_ERROR_MESSAGE, userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            User caller = getUser(callersScopeAccess);

            if (identityConfig.getReloadableConfig().listGlobalRolesForUserPrecedenceRestrictionEnabled()) {
                /*
                1. Users with role 'identity:get-user-roles-global' can list roles for any user

                2. Users can always list roles for themselves

                3. Users can list roles based on usual order of precedence service-admin -> identity-admin -> user-admin -> user-manage -> default-user

                4. If user-admin or below, the users must be in the same domain
                */
                if (!user.getId().equals(caller.getId()) &&
                        !authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName())) {

                    precedenceValidator.verifyCallerPrecedenceOverUserForListGlobalRoles(caller, user);

                    IdentityUserTypeEnum userType = authorizationService.getIdentityTypeRoleAsEnum(caller);
                    if (userType.isDomainBasedAccessLevel() && !caller.getDomainId().equals(user.getDomainId())) {
                        throw new ForbiddenException(NOT_AUTHORIZED);
                    }
                }
            } else {
                /*
                ********** LEGACY AUTHORIZATION LOGIC. Will be removed in CID-282 **********
                1. service-admins, identity-admins, and users with global role can perform this against anyone.

                2. user-admins, user-manager can get roles for any user within same domain (but not at same level (e.g. -
                user manager getting roles on another user-manager)

                3. default users can get only their own roles
                */
                boolean mustVerifyDomainAndPrecedence = true;
                if (authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN
                        , IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName()) || user.getId().equals(caller.getId())) {
                    mustVerifyDomainAndPrecedence = false;
                }

                if (mustVerifyDomainAndPrecedence) {
                    if(caller.getDomainId() == null) {
                        //caller is a user admin, user manage, or default user but with a null domain ID
                        //this is bad data, but protecting against it anyways
                        throw new ForbiddenException(NOT_AUTHORIZED);
                    } else if(!caller.getDomainId().equals(user.getDomainId())) {
                        throw new ForbiddenException(NOT_AUTHORIZED);
                    }
                    precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
                }
            }

            List<TenantRole> roles = tenantService.getGlobalRolesForUser(user);
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserRoles(HttpHeaders httpHeaders, String authToken, String userId, String roleType) {
        try {

            // Currently only roleType=rbac is supported
            if (!roleType.equals(RBAC)) {
                throw new BadRequestException(String.format("type '%s' not supported", roleType));
            }

            ScopeAccess callersScopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserManagedLevelAccess(callersScopeAccess);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);

            if (authorizationService.authorizeCloudUserAdmin(callersScopeAccess) ||
                    authorizationService.authorizeUserManageRole(callersScopeAccess)) {
                if (!caller.getDomainId().equalsIgnoreCase(user.getDomainId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }

            tenantService.deleteRbacRolesForUser(user);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUserGlobalRolesByServiceId(HttpHeaders httpHeaders, String authToken, String userId,
                                                          String serviceId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            User user = userService.checkAndGetUserById(userId);

            List<TenantRole> roles = tenantService.getGlobalRolesForUser(user, serviceId);

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listGroups(HttpHeaders httpHeaders, String authToken, String groupName, Integer marker, Integer limit) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups cloudGroups = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups();

            for (Group group : groupService.getGroups(marker, limit)) {
                com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(group);
                cloudGroups.getGroup().add(cloudGroup);
            }

            return Response.ok(objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups(cloudGroups).getValue());
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder getGroup(HttpHeaders httpHeaders, String authToken, String groupName) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Group group = groupService.getGroupByName(groupName);
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(group);
            return Response.ok(objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup(cloudGroup).getValue());
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder impersonate(HttpHeaders httpHeaders, String authToken, ImpersonationRequest impersonationRequest) {
        try {
            ScopeAccess callerScopeAccess = getScopeAccessForValidToken(authToken);
            BaseUser impersonator = userService.getUserByScopeAccess(callerScopeAccess);
            authorizationService.verifyCallerCanImpersonate(impersonator, callerScopeAccess);

            ImpersonatorType impersonatorType = null;
            if (callerScopeAccess instanceof RackerScopeAccess) {
                validator20.validateImpersonationRequestForRacker(impersonationRequest);
                impersonatorType = ImpersonatorType.RACKER;
            } else if (callerScopeAccess instanceof UserScopeAccess) {
                validator20.validateImpersonationRequestForService(impersonationRequest);
                impersonatorType = ImpersonatorType.SERVICE;
            } else {
                //this shouldn't really happen due to verification that user is racker or identity admin, but just in case.
                // TODO: Should be a 403 rather than 401, but this is how it has been historically
                logger.warn(String.format("Invalid impersonation request. Unrecognized token type '%s'", callerScopeAccess));
                throw new NotAuthorizedException("User does not have access");
            }

            //validate the user being impersonated can be found and is allowed to be impersonated
            EndUser user;
            if(StringUtils.isNotBlank(impersonationRequest.getUser().getFederatedIdp())){
                if (!identityConfig.allowFederatedImpersonation()) {
                    throw new ForbiddenException("Impersonating federated users is not currently enabled.");
                }
                user = identityUserService.checkAndGetFederatedUserByUsernameAndIdentityProviderUri(impersonationRequest.getUser().getUsername(), impersonationRequest.getUser().getFederatedIdp());
            } else {
                //if idp not provided, impersonating a provisioned user so go against provisioned user service
                user = userService.checkAndGetUserByName(impersonationRequest.getUser().getUsername());
            }

            if (!isValidImpersonatee(user)) {
                throw new BadRequestException("User cannot be impersonated; No valid impersonation roles assigned");
            }

            ImpersonatedScopeAccess impersonatedToken = scopeAccessService.processImpersonatedScopeAccessRequest(impersonator, user, impersonationRequest, impersonatorType, callerScopeAccess.getAuthenticatedBy());
            ImpersonationResponse auth = authConverterCloudV20.toImpersonationResponse(impersonatedToken);
            return Response.ok(objFactories.getRackspaceIdentityExtRaxgaV1Factory().createAccess(auth).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listDefaultRegionServices(String authToken) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        DefaultRegionServices defaultRegionServices = raxAuthObjectFactory.createDefaultRegionServices();
        for (Application application : applicationService.getOpenStackServices()) {
            Boolean useForDefaultRegion = application.getUseForDefaultRegion();
            if (useForDefaultRegion != null && useForDefaultRegion) {
                defaultRegionServices.getServiceName().add(application.getName());
            }
        }
        return Response.ok(defaultRegionServices);
    }

    @Override
    public ResponseBuilder setDefaultRegionServices(String authToken, DefaultRegionServices defaultRegionServices) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        List<String> serviceNames = defaultRegionServices.getServiceName();

        for (String serviceName : serviceNames) {
            boolean found = false;
            for (Application application : applicationService.getOpenStackServices()) {
                if (serviceName.equals(application.getName())) {
                    found = true;
                }
            }
            if (!found) {
                throw new BadRequestException("Service " + serviceName + " does not exist.");
            }

        }
        for (Application application : applicationService.getOpenStackServices()) {
            application.setUseForDefaultRegion(false);
            applicationService.updateClient(application);
        }
        for (String serviceName : serviceNames) {
            Application application = applicationService.getByName(serviceName);
            application.setUseForDefaultRegion(true);
            applicationService.updateClient(application);
        }

        return Response.noContent();
    }

    @Override
    public ResponseBuilder addDomain(String authToken, UriInfo uriInfo, com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            if (StringUtils.isBlank(domain.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            Domain savedDomain = this.domainConverterCloudV20.fromDomain(domain);
            this.domainService.addDomain(savedDomain);
            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            String domainId = savedDomain.getDomainId();
            URI build = requestUriBuilder.path(domainId).build();
            com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain value = this.domainConverterCloudV20.toDomain(savedDomain);
            return Response.created(build).entity(objectFactory.createDomain(value).getValue());
        } catch (DuplicateException de) {
            return exceptionHandler.conflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getDomain(String authToken, String domainId) {
        authorizationService.verifyUserLevelAccess(getScopeAccessForValidToken(authToken));

        ScopeAccess token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
        User caller = (User) userService.getUserByScopeAccess(token);
        ClientRole requesterIdentityClientRole = applicationService.getUserIdentityRole(caller);
        IdentityUserTypeEnum requesterIdentityRole = authorizationService.getIdentityTypeRoleAsEnum(requesterIdentityClientRole);
        if (requesterIdentityRole.isDomainBasedAccessLevel() && (caller.getDomainId() == null || !caller.getDomainId().equalsIgnoreCase(domainId))) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }

        Domain domain = domainService.checkAndGetDomain(domainId);
        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain value = this.domainConverterCloudV20.toDomain(domain);

        if (requesterIdentityRole.isDomainBasedAccessLevel()) {
            value.setDescription(null);
            value.setName(null);
        }

        return Response.ok(objectFactory.createDomain(value).getValue());
    }

    @Override
    public ResponseBuilder updateDomain(String authToken, String domainId, com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
        BaseUser caller = userService.getUserByScopeAccess(scopeAccess);
        authorizationService.verifyIdentityAdminLevelAccess(scopeAccess);

        IdentityUserTypeEnum callersUserType = authorizationService.getIdentityTypeRoleAsEnum(caller);
        if(IdentityUserTypeEnum.IDENTITY_ADMIN == callersUserType) {
            List<User> superAdmins = domainService.getDomainSuperAdmins(domainId);
            if(containsServiceAdmin(superAdmins)) {
                throw new ForbiddenException("Cannot modify a domain containing a service admin");
            }
            if(containsIdentityAdmin(superAdmins) && caller.getDomainId() != null && !caller.getDomainId().equals(domainId)) {
                throw new ForbiddenException("Cannot modify a domain containing an identity admin when you are not in the domain");
            }
        }

        Domain domainDO = domainService.checkAndGetDomain(domainId);


        setDomainEmptyValues(domain, domainId);
        validateDomain(domain, domainId);

        Boolean shouldExpireAllTokens =  domainDO.getEnabled() && !domain.isEnabled();

        domainDO.setDescription(domain.getDescription());
        domainDO.setName(domain.getName());
        domainDO.setEnabled(domain.isEnabled());
        domainDO.setName(domain.getName());

        this.domainService.updateDomain(domainDO);

        if(shouldExpireAllTokens){
            domainService.expireAllTokenInDomain(domainDO.getDomainId());
        }

        return Response.ok(objFactories.getRackspaceIdentityExtRaxgaV1Factory().createDomain(domainConverterCloudV20.toDomain(domainDO)).getValue());
    }

    boolean containsServiceAdmin(List<User> users) {
        for(User user : users) {
            if(authorizationService.hasServiceAdminRole(user)) {
                return true;
            }
        }
        return false;
    }

    boolean containsIdentityAdmin(List<User> users) {
        for(User user : users) {
            if(authorizationService.hasIdentityAdminRole(user)) {
                return true;
            }
        }
        return false;
    }

    void validateDomain(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain, String domainId) {
        if (!domainId.equalsIgnoreCase(domain.getId())) {
            throw new BadRequestException("Domain Id does not match.");
        }
        if (StringUtils.isBlank(domain.getName())) {
            throw new BadRequestException("Domain name cannot be empty.");
        }

    }

    void setDomainEmptyValues(com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain, String domainId) {
        if (StringUtils.isBlank(domain.getDescription())) {
            domain.setDescription(null);
        }
        if (StringUtils.isBlank(domain.getName())) {
            domain.setName(null);
        }
        if (StringUtils.isBlank(domain.getId()) && !StringUtils.isBlank(domainId)) {
            domain.setId(domainId);
        }
    }

    @Override
    public ResponseBuilder deleteDomain(String authToken, String domainId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            String defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();
            if (defaultDomainId.equals(domainId)) {
                throw new BadRequestException(GlobalConstants.ERROR_MSG_DELETE_DEFAULT_DOMAIN);
            }

            Domain domain = domainService.checkAndGetDomain(domainId);
            if (identityConfig.getReloadableConfig().enforceDomainDeleteRuleMustBeDisabled()
                    && Boolean.TRUE.equals(domain.getEnabled())) {
                throw new BadRequestException(GlobalConstants.ERROR_MSG_DELETE_ENABLED_DOMAIN);
            }

            Iterable<EndUser> users = identityUserService.getEndUsersByDomainId(domainId);
            if (users.iterator().hasNext()) {
                throw new BadRequestException(GlobalConstants.ERROR_MSG_DELETE_DOMAIN_WITH_USERS);
            }

            String[] associatedTenants = domain.getTenantIds();
            if (ArrayUtils.isNotEmpty(associatedTenants)) {
                /*
                assign tenants to default domain. Since we're deleting the domain, there's no need to update
                the domain to remove the association (assuming the delete domain works). However, to maintain
                consistency use the standard call which will cause extra reads and updates to the domain. Delete domain
                is minimally used so I don't think the inefficiency outweighs the consistency.
                 */
                for (String associatedTenantId : associatedTenants) {
                    logger.info(String.format("Deleting domain '%s'. Setting associated tenant '%s' to default domain '%s'", domainId, associatedTenantId, defaultDomainId));
                    domainService.removeTenantFromDomain(associatedTenantId, domainId);
                }
            }

            domainService.deleteDomain(domain.getDomainId());
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getDomainTenants(String authToken, String domainId, String enabled) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            domainService.checkAndGetDomain(domainId);
            List<Tenant> tenants = tenantService.getTenantsByDomainId(domainId);
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createTenants(tenantConverterCloudV20.toTenantList(tenants)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUsersByDomainIdAndEnabledFlag(String authToken, String domainId, String enabled) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        domainService.checkAndGetDomain(domainId);
        Iterable<EndUser> users;
        if (enabled == null) {
            users = identityUserService.getEndUsersByDomainId(domainId);
        } else {
            users = identityUserService.getEndUsersByDomainIdAndEnabledFlag(domainId, Boolean.valueOf(enabled));
        }

        return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
    }

    @Override
    public ResponseBuilder addUserToDomain(String authToken, String domainId, String userId) throws IOException, JAXBException {
        ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
        authorizationService.verifyIdentityAdminLevelAccess(scopeAccess);
        Domain domain = domainService.checkAndGetDomain(domainId);
        if (!domain.getEnabled()) {
            throw new ForbiddenException("Cannot add users to a disabled domain.");
        }

        User userDO = userService.checkAndGetUserById(userId);
        IdentityUserTypeEnum userType = authorizationService.getIdentityTypeRoleAsEnum(userDO);

        //service admins can only update other service admins and identity admins
        if(IdentityUserTypeEnum.SERVICE_ADMIN == userType || IdentityUserTypeEnum.IDENTITY_ADMIN == userType) {
            authorizationService.verifyServiceAdminLevelAccess(scopeAccess);
        }

        List<TenantRole> roles = userDO.getRoles();
        List<TenantRole> globalRoles = tenantService.getGlobalRolesForUser(userDO);
        if ((roles == null || roles.size() == 0) && (globalRoles == null || globalRoles.size() == 0)) {
            throw new ForbiddenException("Cannot add user with no roles to a domain.");
        }

        userDO.setDomainId(domain.getDomainId());
        this.userService.updateUser(userDO);
        return Response.noContent();
    }

    @Override
    public ResponseBuilder getEndpointsByDomainId(String authToken, String domainId) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        Domain domain = domainService.checkAndGetDomain(domainId);
        List<Tenant> tenantList = tenantService.getTenantsFromNameList(domain.getTenantIds());
        List<OpenstackEndpoint> endpoints = endpointService.getEndpointsFromTenantList(tenantList);
        EndpointList list = endpointConverterCloudV20.toEndpointList(endpoints);
        return Response.ok(objFactories.getOpenStackIdentityV2Factory().createEndpoints(list).getValue());
    }

    @Override
    public ResponseBuilder addTenantToDomain(String authToken, String domainId, String tenantId) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        tenantService.checkAndGetTenant(tenantId);
        domainService.addTenantToDomain(tenantId, domainId);
        return Response.noContent();
    }

    @Override
    public ResponseBuilder removeTenantFromDomain(String authToken, String domainId, String tenantId) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        domainService.removeTenantFromDomain(tenantId, domainId);
        return Response.noContent();
    }

    @Override
    public ResponseBuilder getAccessibleDomains(UriInfo uriInfo, String authToken, Integer marker, Integer limit) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            if (this.authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken) ||
                    this.authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken)) {
                PaginatorContext<Domain> domainContext = this.domainService.getDomains(marker, limit);
                String linkHeader = this.domainPaginator.createLinkHeader(uriInfo, domainContext);

                Domains domains = new Domains();
                domains.getDomain().addAll(domainContext.getValueList());
                com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains domainsObj = domainConverterCloudV20.toDomains(domains);

                return Response.status(200).header("Link", linkHeader).entity(raxAuthObjectFactory.createDomains(domainsObj).getValue());

            } else {
                User user = (User) userService.getUserByScopeAccess(scopeAccessByAccessToken);
                return getAccessibleDomainsForUser(authToken, user.getId());
            }
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getAccessibleDomainsForUser(String authToken, String userId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);

            CloudUserAccessibility cloudUserAccessibility = getUserAccessibility(scopeAccessByAccessToken);

            User user = userService.checkAndGetUserById(userId);
            Domains domains = cloudUserAccessibility.getAccessibleDomainsByUser(user);

            domains = cloudUserAccessibility.addUserDomainToDomains(user, domains);
            domains = cloudUserAccessibility.removeDuplicateDomains(domains);

            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains domainsObj = domainConverterCloudV20.toDomains(domains);

            return Response.ok().entity(raxAuthObjectFactory.createDomains(domainsObj).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    public CloudUserAccessibility getUserAccessibility(ScopeAccess scopeAccess) {
        if (this.authorizationService.authorizeCloudUser(scopeAccess)) {
            return new CloudDefaultUserAccessibility(tenantService, domainService,
                    authorizationService, userService, config, objFactories.getOpenStackIdentityV2Factory(), scopeAccess);
        }
        if (this.authorizationService.authorizeCloudUserAdmin(scopeAccess)) {
            return new CloudUserAdminAccessibility(tenantService, domainService,
                    authorizationService, userService, config, objFactories.getOpenStackIdentityV2Factory(), scopeAccess);
        }
        if (this.authorizationService.authorizeCloudIdentityAdmin(scopeAccess)) {
            return new CloudIdentityAdminAccessibility(tenantService, domainService,
                    authorizationService, userService, config, objFactories.getOpenStackIdentityV2Factory(), scopeAccess);
        }
        if (this.authorizationService.authorizeCloudServiceAdmin(scopeAccess)) {
            return new CloudServiceAdminAccessibility(tenantService, domainService,
                    authorizationService, userService, config, objFactories.getOpenStackIdentityV2Factory(), scopeAccess);
        }
        return new CloudUserAccessibility(tenantService, domainService,
                authorizationService, userService, config, objFactories.getOpenStackIdentityV2Factory(), scopeAccess);
    }

    @Override
    public ResponseBuilder getAccessibleDomainsEndpointsForUser(String authToken, String userId, String domainId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);

            CloudUserAccessibility cloudUserAccessibility = getUserAccessibility(scopeAccessByAccessToken);

            User user = userService.checkAndGetUserById(userId);
            domainService.checkAndGetDomain(domainId);

            List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForUser(user);
            List<Tenant> tenants = tenantService.getTenantsByDomainId(domainId);

            List<OpenstackEndpoint> domainEndpoints = cloudUserAccessibility.getAccessibleDomainEndpoints(endpoints, tenants, user);

            EndpointList list = cloudUserAccessibility.convertPopulateEndpointList(domainEndpoints);

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createEndpoints(list).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    public ResponseBuilder listUsersWithRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String roleId, Integer marker, Integer limit) {
        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            ClientRole role = this.applicationService.getClientRoleById(roleId);

            if (role == null) {
                throw new NotFoundException(String.format(ROLE_ID_NOT_FOUND_ERROR_MESSAGE, roleId));
            }

            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccess);

            PaginatorContext<User> userContext;

            if (callerIsUserAdmin) {
                User caller = (User) this.userService.getUserByScopeAccess(scopeAccess);
                if (caller.getDomainId() == null || StringUtils.isBlank(caller.getDomainId())) {
                    throw new BadRequestException("User-admin has no domain");
                }
                userContext = this.userService.getUsersWithDomainAndRole(caller.getDomainId(), roleId, marker, limit);
            } else {
                userContext = this.userService.getUsersWithRole(roleId, marker, limit);
            }

            String linkHeader = this.userPaginator.createLinkHeader(uriInfo, userContext);

            return Response.status(200).header("Link", linkHeader).entity(objFactories.getOpenStackIdentityV2Factory()
                    .createUsers(this.userConverterCloudV20.toUserList(userContext.getValueList())).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addRegion(UriInfo uriInfo, String authToken, Region region) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            cloudRegionService.addRegion(regionConverterCloudV20.fromRegion(region));
            String regionName = region.getName();
            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            URI build = requestUriBuilder.path(regionName).build();
            return Response.created(build);
        } catch (DuplicateException de) {
            return exceptionHandler.conflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getRegion(String authToken, String name) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            com.rackspace.idm.domain.entity.Region region = this.cloudRegionService.checkAndGetRegion(name);
            return Response.ok().entity(regionConverterCloudV20.toRegion(region).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getRegions(String authToken) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        Iterable<com.rackspace.idm.domain.entity.Region> regions = this.cloudRegionService.getRegions(config.getString("cloud.region"));
        return Response.ok().entity(regionConverterCloudV20.toRegions(regions).getValue());
    }

    @Override
    public ResponseBuilder updateRegion(String authToken, String name, Region region) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            cloudRegionService.checkAndGetRegion(name);
            com.rackspace.idm.domain.entity.Region updateRegion = regionConverterCloudV20.fromRegion(region);
            cloudRegionService.updateRegion(name, updateRegion);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteRegion(String authToken, String name) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            cloudRegionService.checkAndGetRegion(name);
            cloudRegionService.deleteRegion(name);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addQuestion(UriInfo uriInfo, String authToken, Question question) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            String questionId = questionService.addQuestion(questionConverter.fromQuestion(question));

            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            URI build = requestUriBuilder.path(questionId).build();
            return Response.created(build);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getQuestion(String authToken, String questionId) {
        try {
            authorizationService.verifyUserLevelAccess(getScopeAccessForValidToken(authToken));
            return Response.ok().entity(questionConverter.toQuestion(questionService.getQuestion(questionId)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getQuestions(String authToken) {
        try {
            authorizationService.verifyUserLevelAccess(getScopeAccessForValidToken(authToken));
            return Response.ok().entity(questionConverter.toQuestions(questionService.getQuestions()).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateQuestion(String authToken, String questionId, Question question) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            questionService.updateQuestion(questionId, questionConverter.fromQuestion(question));
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteQuestion(String authToken, String questionId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            questionService.deleteQuestion(questionId);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getSecretQAs(String authToken, String userId) {
        try {
            isUserAllowed(authToken, userId);

            com.rackspace.idm.domain.entity.SecretQAs secretQAsEntity = secretQAService.getSecretQAs(userId);
            SecretQAs secretQAs = secretQAConverterCloudV20.toSecretQAs(secretQAsEntity.getSecretqa()).getValue();
            return Response.ok(secretQAs);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder createSecretQA(String authToken, String userId, com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA secretQA) {
        try{
            isUserAllowed(authToken, userId);
            com.rackspace.idm.domain.entity.SecretQA secretQAEntity = secretQAConverterCloudV20.fromSecretQA(secretQA);
            secretQAService.addSecretQA(userId, secretQAEntity);
            return Response.ok();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserAdminsForUser(String authToken, String userId) {
        requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

        BaseUser caller =  requestContextHolder.getRequestContext().getEffectiveCaller();
        EndUser user = identityUserService.checkAndGetUserById(userId);

        boolean callerIsDefaultUser = authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName());
        boolean callerIsUserAdmin = authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName());

        boolean isSelfCall = caller.getId().equals(user.getId());
        String callerDomain = caller.getDomainId();
        String targetDomain = user.getDomainId();

        if (!isSelfCall) {
            if (callerIsDefaultUser) {
                // Default users can only call on themself
                throw new ForbiddenException(NOT_AUTHORIZED);
            } else if (callerIsUserAdmin &&
                    ((callerDomain != null && !callerDomain.equals(targetDomain))
                     || (callerDomain == null && targetDomain != null))
                    ) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }
        }

        // Additional fed user tests added as part of CID-66
        if (user instanceof FederatedUser) {
            FederatedUser federatedUser = (FederatedUser) user;
            if (federatedUser.isExpired()) {
                throw new NotFoundException(String.format("User %s not found", userId));
            }
        }

        List<User> admins = new ArrayList<User>();
        if (user.getDomainId() != null ) {
            admins = domainService.getEnabledDomainAdmins(user.getDomainId());
        }

        return Response.status(200)
                .entity(objFactories.getOpenStackIdentityV2Factory()
                        .createUsers(userConverterCloudV20.toUserList(admins)).getValue());
    }

    private void isUserAllowed(String authToken, String userId) {
        ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
        authorizationService.verifyUserLevelAccess(scopeAccess);
        User caller = getUser(scopeAccess);
        User user = userService.checkAndGetUserById(userId);
        boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccess);
        Boolean access = false;
        if (userId.equals(caller.getId())) {
            access = true;
        } else if (callerIsUserAdmin) {
            if (caller.getDomainId().equals(user.getDomainId())) {
                access = true;
            }
        } else {
            authorizationService.verifyIdentityAdminLevelAccess(scopeAccess);
            access = true;
        }
        if (!access) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    public boolean isValidImpersonatee(EndUser user) {
        List<TenantRole> tenantRolesForUser = tenantService.getGlobalRolesForUser(user);
        for (TenantRole role : tenantRolesForUser) {
            String name = role.getName();
            if (name.equals("identity:default") || name.equals("identity:user-admin")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();
            if (!caller.getId().equals(userId) || !identityConfig.getReloadableConfig().isListGroupsForSelfEnabled()) {
                authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.GET_USER_GROUPS_GLOBAL.getRoleName());
            }

            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups cloudGroups = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups();

            EndUser user = identityUserService.checkAndGetUserById(userId);

            Iterable<Group> groups = identityUserService.getGroupsForEndUser(user.getId());

            if (!groups.iterator().hasNext()) {
                Group defGroup = groupService.getGroupById(config.getString("defaultGroupId"));
                com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(defGroup);
                cloudGroups.getGroup().add(cloudGroup);
            }
            for (Group group : groups) {
                com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(group);
                cloudGroups.getGroup().add(cloudGroup);
            }
            return Response.ok(objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups(cloudGroups).getValue());

        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder getGroupById(HttpHeaders httpHeaders, String authToken, String groupId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Group group = groupService.checkAndGetGroupById(groupId);
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(group);
            return Response.ok(objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup(cloudGroup).getValue());
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder addGroup(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken,
                                    com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            validator20.validateKsGroup(group);
            Group groupDO = cloudGroupBuilder.build(group);
            groupService.addGroup(groupDO);
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs = cloudKsGroupBuilder.build(groupDO);
            return Response.created(uriInfo.getRequestUriBuilder().path(groupKs.getId()).build())
                    .entity(objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup(groupKs).getValue());
        } catch (DuplicateException bre) {
            return exceptionHandler.conflictExceptionResponse(bre.getMessage());
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }


    }

    @Override
    public ResponseBuilder updateGroup(HttpHeaders httpHeaders, String authToken, String groupId,
                                       com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            validator20.validateKsGroup(group);

            if(group.getId() != null && !groupId.equals(group.getId())){
                throw new BadRequestException(ID_MISMATCH);
            }

            group.setId(groupId);
            Group groupDO = cloudGroupBuilder.build(group);
            groupService.updateGroup(groupDO);
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs = cloudKsGroupBuilder.build(groupDO);

            return Response.ok().entity(objFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup(groupKs).getValue());
        } catch (DuplicateException bre) {
            return exceptionHandler.conflictExceptionResponse(bre.getMessage());
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder deleteGroup(HttpHeaders httpHeaders, String authToken, String groupId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            groupService.deleteGroup(groupId);
            return Response.noContent();
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder addUserToGroup(HttpHeaders httpHeaders, String authToken, String groupId, String userId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Group group = groupService.checkAndGetGroupById(groupId);

            User caller = userService.getUserByAuthToken(authToken);
            EndUser user = identityUserService.checkAndGetUserById(userId);

            if (authorizationService.hasDefaultUserRole(user)) {
                throw new BadRequestException("Cannot add Sub-Users directly to a Group, must assign their Parent User.");
            }

            if (!userService.isUserInGroup(userId, group.getGroupId())) {

                precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);

                if (authorizationService.hasUserAdminRole(user)) {
                    Iterable<EndUser> subUsers = identityUserService.getEndUsersByDomainId(user.getDomainId());
                    for (EndUser subUser : subUsers) {
                        if (!user.getId().equalsIgnoreCase(subUser.getId())) {
                            identityUserService.addGroupToEndUser(groupId, subUser.getId());
                            if (user instanceof User) {
                                //we don't send fed user create events, so won't send update events
                                atomHopperClient.asyncPost((User)user, AtomHopperConstants.GROUP);
                            }
                        }
                    }
                }
                //i guess the purpose here is to add the group to the user-admin last...
                identityUserService.addGroupToEndUser(groupId, user.getId());
                if (user instanceof User) {
                    //we don't send fed user create events, so won't send update events
                    atomHopperClient.asyncPost((User)user, AtomHopperConstants.GROUP);
                }
            }
            return Response.noContent();
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder removeUserFromGroup(HttpHeaders httpHeaders, String authToken, String groupId, String userId) {
        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyIdentityAdminLevelAccess(scopeAccess);
            Group group = groupService.checkAndGetGroupById(groupId);

            if (userId == null || userId.trim().isEmpty()) {
                throw new BadRequestException("Invalid user id");
            }

            EndUser user = identityUserService.checkAndGetUserById(userId);
            boolean isDefaultUser = authorizationService.hasDefaultUserRole(user);
            boolean isUserAdmin = authorizationService.hasUserAdminRole(user);

            if (isDefaultUser) {
                throw new BadRequestException("Cannot remove Sub-Users directly from a Group, must remove their Parent User.");
            }

            if (!userService.isUserInGroup(userId, group.getGroupId())) {
                throw new NotFoundException("Group '" + group.getName() + "' is not assigned to user.");
            }

            if (isUserAdmin) {
                Iterable<EndUser> subUsers = identityUserService.getEndUsersByDomainId(user.getDomainId());
                for (EndUser subUser : subUsers) {
                    if (!user.getId().equalsIgnoreCase(subUser.getId())) {
                        identityUserService.removeGroupFromEndUser(groupId, subUser.getId());
                        if (user instanceof User) {
                            //we don't send fed user create events, so won't send update events
                            atomHopperClient.asyncPost((User)user, AtomHopperConstants.GROUP);
                        }
                    }
                }
            }
            //i guess the purpose here is to remove the group from the user-admin last...
            identityUserService.removeGroupFromEndUser(groupId, user.getId());
            if (user instanceof User) {
                //we don't send fed user create events, so won't send update events
                atomHopperClient.asyncPost((User)user, AtomHopperConstants.GROUP);
            }
            return Response.noContent();
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder getUsersForGroup(HttpHeaders httpHeaders, String authToken, String groupId, Integer marker, Integer limit)  {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            groupService.checkAndGetGroupById(groupId);
            PaginatorContext<User> users = userService.getEnabledUsersByGroupId(groupId, marker, limit);

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(users.getValueList())).getValue());
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    // KSADM Extension User methods
    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Integer marker, Integer limit) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            BaseUser caller = userService.getUserByScopeAccess(scopeAccessByAccessToken);

            //if default user & NOT user-manage
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken)
                    && !authorizationService.authorizeUserManageRole(scopeAccessByAccessToken)) {
                List<EndUser> users = new ArrayList<EndUser>();
                //at this point, we know that the user is not a racker and can cast to an EndUser
                users.add((EndUser) caller);
                return Response.ok(objFactories.getOpenStackIdentityV2Factory()
                        .createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
            }
            authorizationService.verifyUserManagedLevelAccess(scopeAccessByAccessToken);

            PaginatorContext<EndUser> paginatorContext;
            if (authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken) ||
                    authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken)) {
                paginatorContext = this.identityUserService.getEnabledEndUsersPaged(marker, limit);
            } else {
                if (caller.getDomainId() != null) {
                    String domainId = caller.getDomainId();
                    paginatorContext = this.identityUserService.getEndUsersByDomainIdPaged(domainId, marker, limit);
                } else {
                    throw new BadRequestException("User-admin has no domain");
                }
            }

            List<EndUser> users = new ArrayList<EndUser>();

            // If role is identity:user-manage then we need to filter out the identity:user-admin
            if (authorizationService.authorizeUserManageRole(scopeAccessByAccessToken)) {
                for (EndUser user : paginatorContext.getValueList()) {
                    if (!authorizationService.hasUserAdminRole(user)) {
                        users.add(user);
                    }
                }
            } else {
                users = paginatorContext.getValueList();
            }

            String linkHeader = endUserPaginator.createLinkHeader(uriInfo, paginatorContext);

            return Response.status(200)
                    .header("Link", linkHeader)
                    .entity(objFactories.getOpenStackIdentityV2Factory()
                            .createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String tenantId, Integer marker, Integer limit) {

        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);

            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            PaginatorContext<User> pageContext = this.tenantService.getUsersForTenant(tenant.getTenantId(), marker, limit);
            String linkHeader = userPaginator.createLinkHeader(uriInfo, pageContext);

            return Response.status(200)
                    .header("Link", linkHeader)
                    .entity(objFactories.getOpenStackIdentityV2Factory()
                            .createUsers(userConverterCloudV20.toUserList(pageContext.getValueList())).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String tenantId,
                                                      String roleId, Integer marker, Integer limit) {

        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            ClientRole role = checkAndGetClientRole(roleId);

            PaginatorContext<User> pageContext = this.tenantService.getUsersWithTenantRole(tenant, role, marker, limit);
            String linkHeader = userPaginator.createLinkHeader(uriInfo, pageContext);

            return Response.status(200)
                    .header("Link", linkHeader)
                    .entity(objFactories.getOpenStackIdentityV2Factory()
                            .createUsers(userConverterCloudV20.toUserList(pageContext.getValueList())).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken, String userId,
                                          org.openstack.docs.identity.api.v2.User user) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            User userDO = userService.checkAndGetUserById(userId);

            boolean isDisabled = userDO.isDisabled();
            userDO.setEnabled(user.isEnabled());

            if (userDO.isDisabled() && !isDisabled) {
                atomHopperClient.asyncPost(userDO, AtomHopperConstants.DISABLED);
            } else if (!userDO.isDisabled() && isDisabled) {
                atomHopperClient.asyncPost(userDO, AtomHopperConstants.ENABLED);
            }

            this.userService.updateUser(userDO);

            return Response.ok(objFactories.getOpenStackIdentityV2Factory()
                    .createUser(this.userConverterCloudV20.toUser(userDO)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    public void setUserGroupService(GroupService cloudGroupService) {
        this.groupService = cloudGroupService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseBuilder updateSecretQA(HttpHeaders httpHeaders, String authToken, String userId, SecretQA secrets) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            if (StringUtils.isBlank(secrets.getAnswer())) {
                throw new BadRequestException("Excpeting answer");
            }

            if (StringUtils.isBlank(secrets.getQuestion())) {
                throw new BadRequestException("Excpeting question");
            }

            String questionId = null;

            for(com.rackspace.idm.domain.entity.Question question : questionService.getQuestions()){
                if(secrets.getQuestion().trim().equalsIgnoreCase(question.getQuestion())){
                    questionId = question.getId();
                }
            }

            User user = userService.checkAndGetUserById(userId);

            user.setSecretAnswer(secrets.getAnswer());
            user.setSecretQuestion(secrets.getQuestion());

            if(questionId != null){
                user.setSecretQuestionId(questionId);
            }else {
                user.setSecretQuestionId("0");
            }

            this.userService.updateUser(user);

            return Response.ok();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                        org.openstack.docs.identity.api.v2.Tenant tenant) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenantDO = tenantService.checkAndGetTenant(tenantId);

            tenantDO.setDescription(tenant.getDescription());
            tenantDO.setDisplayName(tenant.getDisplayName());
            tenantDO.setEnabled(tenant.isEnabled());
            if (identityConfig.getReloadableConfig().getAllowTenantNameToBeChangedViaUpdateTenant()) {
                tenantDO.setName(tenant.getName());
            }

            this.tenantService.updateTenant(tenantDO);

            return Response.ok(
                    objFactories.getOpenStackIdentityV2Factory().createTenant(tenantConverterCloudV20.toTenant(tenantDO)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId,
                                                       String credentialType, ApiKeyCredentials creds) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            if (StringUtils.isBlank(creds.getApiKey())) {
                String errMsg = "Expecting apiKey";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            validator20.validateUsername(creds.getUsername());
            User credUser = this.userService.getUser(creds.getUsername());

            if (credUser == null) {
                String errMsg = String.format("User: '%s' not found.", creds.getUsername());
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }


            User user = userService.checkAndGetUserById(userId);
            if (!creds.getUsername().equals(user.getUsername())) {
                String errMsg = USER_AND_USER_ID_MIS_MATCHED;
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            user.setApiKey(creds.getApiKey());
            this.userService.updateUser(user);

            return Response.ok(objFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(creds).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder resetUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId, String credentialType) {

        try {
            ScopeAccess authScopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(authScopeAccess);

            if (identityConfig.getReloadableConfig().preventRackerImpersonationApiKeyAccess() &&
                    requestContextHolder.getRequestContext().getSecurityContext().isRackerImpersonatedRequest()) {
                throw new ForbiddenException("Impersonation tokens cannot be used to reset user API key credentials");
            }

            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(authScopeAccess);
            boolean callerIsDefaultUser = authorizationService.authorizeCloudUser(authScopeAccess);

            User caller = userService.getUserByAuthToken(authToken);
            User credUser = this.userService.checkAndGetUserById(userId);
            if (callerIsDefaultUser && !caller.getId().equals(userId)) {
                throw new ForbiddenException("This user can only reset their own apiKey");
            } else if (callerIsUserAdmin) {
                authorizationService.verifyDomain(caller, credUser);
            } else if (authorizationService.authorizeCloudIdentityAdmin(authScopeAccess)) {
                if (authorizationService.hasServiceAdminRole(credUser)) {
                    throw new ForbiddenException("This user cannot set or reset Service Admin apiKey.");
                }
            }

            final String apiKey = UUID.randomUUID().toString().replaceAll("-", "");
            ApiKeyCredentials creds = new ApiKeyCredentials();
            creds.setApiKey(apiKey);
            creds.setUsername(credUser.getUsername());

            credUser.setApiKey(creds.getApiKey());
            this.userService.updateUser(credUser);

            return Response.ok(objFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(creds).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateUserPasswordCredentials(HttpHeaders httpHeaders, String authToken, String userId,
                                                         String credentialType, PasswordCredentialsBase creds) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            validator.isUsernameValid(creds.getUsername());
            validator.validatePasswordForCreateOrUpdate(creds.getPassword());

            User user = userService.checkAndGetUserById(userId);
            if (!creds.getUsername().equals(user.getUsername())) {
                String errMsg = USER_AND_USER_ID_MIS_MATCHED;
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            user.setUserPassword(creds.getPassword());
            user.setPassword(creds.getPassword());
            this.userService.updateUser(user);

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createCredential(creds).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    // Core Admin Token Methods

    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String tenantId) {
        try {
            // Feature flag to enable self-validating tokens (B-80571:TK-171274).
            final boolean selfValidate = config.getBoolean(FEATURE_USER_TOKEN_SELF_VALIDATION, FEATURE_USER_TOKEN_SELF_VALIDATION_DEFAULT_VALUE);
            final boolean sameToken = StringUtils.equals(authToken, tokenId);

            // User can validate his own token (B-80571:TK-165775).
            ScopeAccess callerToken = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            if (!(selfValidate && sameToken)) {
                //TODO: This token can be a Racker, Service or User of Proper Level
                authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.VALIDATE_TOKEN_GLOBAL.getRoleName());
            }

            if (StringUtils.isNotBlank(callerToken.getScope())) {
                throw new ForbiddenException("Cannot use scoped tokens to validate tokens.");
            }

            if (!StringUtils.deleteWhitespace(tokenId).equals(tokenId))  {
                throw new NotFoundException("Token not found.");
            }

            final ScopeAccess sa = checkAndGetToken(tokenId); //throws not found exception if token can't not be decrypted
            //no scoped tokens can currently be validated through the v2 validate call
            if (sa.isAccessTokenExpired(new DateTime()) || StringUtils.isNotBlank(sa.getScope())) {
                throw new NotFoundException("Token not found.");
            }

            AuthenticateResponse authenticateResponse;
            if (sa instanceof RackerScopeAccess) {
                authenticateResponse = authenticateResponseService.buildAuthResponseForValidateToken((RackerScopeAccess) sa);
            } else if (sa instanceof UserScopeAccess) {
                authenticateResponse = authenticateResponseService.buildAuthResponseForValidateToken((UserScopeAccess) sa, tenantId);
            } else {
                authenticateResponse = authenticateResponseService.buildAuthResponseForValidateToken((ImpersonatedScopeAccess) sa, tenantId);
            }

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(authenticateResponse).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder revokeToken(HttpHeaders httpHeaders, String authToken) throws IOException, JAXBException {
        ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
        authorizationService.verifyUserLevelAccess(scopeAccessByAccessToken);
        scopeAccessService.expireAccessToken(authToken);
        return Response.status(204);
    }

    @Override
    public ResponseBuilder revokeToken(HttpHeaders httpHeaders, String authToken, String tokenId) throws IOException, JAXBException {
        ScopeAccess scopeAccessAdmin = getScopeAccessForValidToken(authToken);

        //can expire your own token - regardless of type
        if (authToken.equals(tokenId)) {
            scopeAccessService.expireAccessToken(tokenId);
            return Response.status(204);
        }

        //if NOT expiring own token, the caller token MUST not be a scoped token.
        if (StringUtils.isNotBlank(scopeAccessAdmin.getScope())) {
            throw new ForbiddenException(DefaultAuthorizationService.NOT_AUTHORIZED_MSG);
        }

        authorizationService.verifyUserAdminLevelAccess(scopeAccessAdmin);
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(tokenId);
        if (scopeAccess == null) {
            throw new NotFoundException("Token not found");
        }

        if (authorizationService.authorizeCloudUserAdmin(scopeAccessAdmin)) {
            User caller = userService.getUserByAuthToken(authToken);
            User user = userService.getUserByAuthToken(tokenId);
            authorizationService.verifyDomain(caller, user);
            scopeAccessService.expireAccessToken(tokenId);
            return Response.status(204);
        }

        scopeAccessService.expireAccessToken(tokenId);
        return Response.status(204);
    }

    ClientRole checkAndGetClientRole(String id) {
        ClientRole cRole = this.applicationService.getClientRoleById(id);
        if (cRole == null) {
            String errMsg = String.format(ROLE_ID_NOT_FOUND_ERROR_MESSAGE, id);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return cRole;
    }

    ScopeAccess checkAndGetToken(String tokenId) {
        return checkToken(getToken(tokenId), tokenId);
    }

    ScopeAccess getToken(String tokenId) {
        return this.scopeAccessService.getScopeAccessByAccessToken(tokenId);
    }

    ScopeAccess checkToken(ScopeAccess sa, String tokenId) {
        if (sa == null) {
            String errMsg = String.format("Token %s not found", tokenId);
            logger.warn(errMsg);
            throw new NotFoundException("Token not found.");
        }
        return sa;
    }

    void checkForMultipleIdentityAccessRoles(User user, ClientRole roleToAdd) {
        user.setRoles(tenantService.getGlobalRolesForUser(user));
        if (user.getRoles() == null ||
            roleToAdd == null ||
            !roleService.isIdentityAccessRole(roleToAdd) ||
            roleToAdd.getName().equalsIgnoreCase("identity:user-manage")
        ) {
            return;
        }

        for (TenantRole userRole : user.getRoles()) {
            ClientRole clientRole = applicationService.getClientRoleById(userRole.getRoleRsId());
            if(roleService.isIdentityAccessRole(clientRole)) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }
        }
    }

    public ScopeAccess getScopeAccessForValidToken(String authToken) {
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

    JAXBElement<? extends CredentialType> getJSONCredentials(String jsonBody) {

        JAXBElement<? extends CredentialType> jaxbCreds = null;

        CredentialType creds = JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString(jsonBody);

        if (creds instanceof ApiKeyCredentials) {
            jaxbCreds = objFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials((ApiKeyCredentials) creds);
        }
        if ((PasswordCredentialsBase.class).isAssignableFrom(creds.getClass())) {
            jaxbCreds = objFactories.getOpenStackIdentityV2Factory().createCredential(creds);
        }

        return jaxbCreds;
    }

    @SuppressWarnings("unchecked")
    JAXBElement<? extends CredentialType> getXMLCredentials(String body) {
        JAXBElement<? extends CredentialType> cred;
        try {
            body = fixApiKeyCredentialEmptyNamespace(body); // ToDo: remove if not needed!
            JAXBContext context = JAXBContextResolver.get();
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends CredentialType>) unmarshaller.unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            throw new BadRequestException(e);
        }
        return cred;
    }

    // Silly patch to implement Cloud Auth allowing empty/invalid namespaces. We will just allow empty for ApiKeyCreds.
    String fixApiKeyCredentialEmptyNamespace(String body) {
        if (body != null && body.contains("apiKeyCredentials") && !body.contains("xmlns")) {
            body = body.replace("<apiKeyCredentials", "<apiKeyCredentials xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\"");
        }
        return body;
    }


    /**
     * This method checks to make sure that a user is allowed to request a token scoped
     * to SETUP-MFA. This check consists of 5 parts:
     * 1. The User is a provisioned User
     * 2. The User CAN NOT already have MFA enabled
     * 3. The User MUST have have access to MFA
     * 4. The User CAN NOT have an OPTIONAL multi-factor enforcement level
     * 5. If the User has DEFAULT for the multi-factor enforcement level then the
     *    multi-factor enforcement level on the domain CAN NOT be OPTIONAL.
     *
     * @param baseUser
     */
    private void checkIfSetupMfaScopeAllowed(BaseUser baseUser) {

        // The user MUST be a provisioned User
        if (!(baseUser instanceof User)) {
            throw new IllegalArgumentException();
        }

        User user = (User)baseUser;

        // The User CAN NOT already be Multi-Factor Enabled
        if (user.isMultiFactorEnabled()) {
            throw new ForbiddenException(SETUP_MFA_SCOPE_FORBIDDEN);
        }

        // The User CAN NOT have a Multi-Factor Enforcement level of OPTIONAL
        if (GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL.equalsIgnoreCase(user.getUserMultiFactorEnforcementLevelIfNullWillReturnDefault())) {
            throw new ForbiddenException(SETUP_MFA_SCOPE_FORBIDDEN);
        }

        // If a User has a Multi-Factor Enforcement level of DEFAULT then the user's
        // domain CAN NOT have a Multi-Factor Enforcement Level of OPTIONAL
        if (GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT.equalsIgnoreCase(user.getUserMultiFactorEnforcementLevelIfNullWillReturnDefault())) {

            Domain domain = domainService.getDomain(user.getDomainId());

            if (domain != null && GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL.equalsIgnoreCase(domain.getDomainMultiFactorEnforcementLevelIfNullWillReturnOptional())) {
                throw new ForbiddenException(SETUP_MFA_SCOPE_FORBIDDEN);
            }
        }
    }

    private void checkMfaEnforcement(BaseUser baseUser) {
        // The user MUST be a provisioned User
        if (!(baseUser instanceof User)) {
            throw new IllegalArgumentException();
        }

        User user = (User)baseUser;

        // If the user's mfa enforcement flag is OPTIONAL then normal auth
        // can proceed.
        if (GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL.equalsIgnoreCase(user.getUserMultiFactorEnforcementLevelIfNullWillReturnDefault())) {
            return;
        }

        // If a User has a Multi-Factor Enforcement level of DEFAULT and the user's domain
        // has a Multi-Factor Enforcement Level of OPTIONAL then normal auth can proceed
        if (GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT.equalsIgnoreCase(user.getUserMultiFactorEnforcementLevelIfNullWillReturnDefault())) {

            Domain domain = domainService.getDomain(user.getDomainId());

            if (domain == null || GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL.equalsIgnoreCase(domain.getDomainMultiFactorEnforcementLevelIfNullWillReturnOptional())) {
                return;
            }
        }

        // If all those checks fail we need to return a Forbidden Exception that tells the user they must setup Multi-Factor
        throw new ForbiddenException(MUST_SETUP_MFA);
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void setTenantConverterCloudV20(TenantConverterCloudV20 tenantConverterCloudV20) {
        this.tenantConverterCloudV20 = tenantConverterCloudV20;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    public void setEndpointConverterCloudV20(EndpointConverterCloudV20 endpointConverterCloudV20) {
        this.endpointConverterCloudV20 = endpointConverterCloudV20;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setUserConverterCloudV20(UserConverterCloudV20 userConverterCloudV20) {
        this.userConverterCloudV20 = userConverterCloudV20;
    }

    public void setCloudGroupBuilder(CloudGroupBuilder cloudGroupBuilder) {
        this.cloudGroupBuilder = cloudGroupBuilder;
    }

    public void setCloudKsGroupBuilder(CloudKsGroupBuilder cloudKsGroupBuilder) {
        this.cloudKsGroupBuilder = cloudKsGroupBuilder;
    }

    public void setTokenConverterCloudV20(TokenConverterCloudV20 tokenConverterCloudV20) {
        this.tokenConverterCloudV20 = tokenConverterCloudV20;
    }

    public void setDomainConverterCloudV20(DomainConverterCloudV20 domainConverterCloudV20) {
        this.domainConverterCloudV20 = domainConverterCloudV20;
    }

    public void setAtomHopperClient(AtomHopperClient atomHopperClient) {
        this.atomHopperClient = atomHopperClient;
    }

    public void setRoleConverterCloudV20(RoleConverterCloudV20 roleConverterCloudV20) {
        this.roleConverterCloudV20 = roleConverterCloudV20;
    }

    public void setAuthConverterCloudV20(AuthConverterCloudV20 authConverterCloudV20) {
        this.authConverterCloudV20 = authConverterCloudV20;
    }

    public void setExtensionMap(Map<String, JAXBElement<Extension>> extensionMap) {
        this.extensionMap = extensionMap;
    }

    public void setCurrentExtensions(JAXBElement<Extensions> currentExtensions) {
        this.currentExtensions = currentExtensions;
    }

    public void setServiceConverterCloudV20(ServiceConverterCloudV20 serviceConverterCloudV20) {
        this.serviceConverterCloudV20 = serviceConverterCloudV20;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void setValidator20(Validator20 validator20) {
        this.validator20 = validator20;
    }

    public void setDefaultRegionService(DefaultRegionService defaultRegionService) {
        this.defaultRegionService = defaultRegionService;
    }

    public void setDomainService(DomainService domainService) {
        this.domainService = domainService;
    }

    public void setApplicationRolePaginator(Paginator<ClientRole> rolePaginator) {
        this.applicationRolePaginator = rolePaginator;
    }

    public void setUserPaginator(Paginator<User> userPaginator) {
        this.userPaginator = userPaginator;
    }
    
    public void setQuestionService(QuestionService questionService) {
        this.questionService = questionService;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }


}

