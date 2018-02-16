package com.rackspace.idm.api.resource.cloud.v20;

import com.google.common.collect.ImmutableList;
import com.newrelic.api.agent.NewRelic;
import com.rackspace.docs.core.event.EventType;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.NewRelicTransactionNames;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForCredentialType;
import com.rackspace.idm.api.resource.pagination.Paginator;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Domains;
import com.rackspace.idm.domain.entity.IdentityProperty;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.impl.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.modules.endpointassignment.service.RuleService;
import com.rackspace.idm.modules.usergroups.api.resource.converter.RoleAssignmentConverter;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import com.rackspace.idm.util.SamlLogoutResponseUtil;
import com.rackspace.idm.util.SamlUnmarshaller;
import com.rackspace.idm.validation.Cloud20CreateUserValidator;
import com.rackspace.idm.validation.PrecedenceValidator;
import com.rackspace.idm.validation.Validator;
import com.rackspace.idm.validation.Validator20;
import com.rackspace.idm.validation.property.IdentityProviderDefaultPolicyPropertyValidator;
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
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.impl.LogoutResponseMarshaller;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class DefaultCloud20Service implements Cloud20Service {

    public static final String NOT_AUTHORIZED = "Not Authorized";
    public static final String ID_MISMATCH = "Id in url does not match id in body.";
    public static final String USER_AND_USER_ID_MIS_MATCHED = "User and UserId mis-matched";
    public static final String RBAC = "rbac";
    public static final String SETUP_MFA_SCOPE_FORBIDDEN = "SETUP-MFA SCOPE not supported";
    public static final String MUST_SETUP_MFA = "User must setup multi-factor";

    public static final String INVALID_DOMAIN_ERROR = "Invalid domain";
    public static final String DOMAIN_ID_NOT_FOUND_ERROR_MESSAGE = "Domain ID %s does not exist.";
    public static final String CANNOT_SPECIFY_GROUPS_ERROR = "Cannot specify groups for sub-users";
    public static final String V11_API_QNAME = "http://docs.rackspacecloud.com/auth/api/v1.1";

    public static final String FEDERATION_IDP_TYPE_ERROR_MESSAGE = "%s is currently the only supported IDP type allowed for filtering.";
    public static final String FEDERATION_IDP_FILTER_CONFLICT_ERROR_MESSAGE = "The provided IDP filters cannot be used together.";
    public static final String FEDERATION_IDP_FILTER_TENANT_NO_DOMAIN_ERROR_MESSAGE = "The provided tenant is not associated with a domain";
    public static final String FEDERATION_IDP_POLICY_TYPE_NOT_FOUND_ERROR_MESSAGE = "No %s mapping policy found for IDP with ID %s.";
    public static final String FEDERATION_IDP_CREATION_NOT_AVAILABLE_MISSING_DEFAULT_POLICY_MESSAGE = "IDP creation is currently unavailable due to missing default for IDP policy.";
    public static final String FEDERATION_IDP_CANNOT_MANUALLY_UPDATE_CERTS_ON_METADATA_IDP_MESSAGE = "IDP certificates cannot be updated outside of providing a new IDP metadata xml.";
    public static final String FEDERATION_LIST_IDP_EMAIL_DOMAIN_WITH_OTHER_PARAMS_ERROR_MSG = "The email domain search is mutually exclusive from all other search fields";

    public static final String DUPLICATE_SERVICE_NAME_ERROR_MESSAGE = "More than one service exists with the given name. Please specify a different service name for the endpoint template.";
    public static final String DUPLICATE_SERVICE_ERROR_MESSAGE = "Unable to fulfill request. More than one service exists with the given name.";

    public static final String ERROR_CANNOT_DELETE_USER_TYPE_ROLE_MESSAGE = "Cannot delete identity user-type roles from a user.";

    public static final String ERROR_CANNOT_DELETE_ENDPOINT_TEMPLATE_MESSAGE = "Deleting enabled templates or templates associated with one or more tenants is not allowed";
    public static final String ERROR_CANNOT_DELETE_ENDPOINT_TEMPLATE_IN_ASSIGNMENT_RULE_MESSAGE = "Deleting endpoint templates associated with one or more endpoint assignment rules is not allowed.";

    public static final String ERROR_CANNOT_ASSIGN_GLOBAL_ONLY_ROLES_VIA_TENANT_ASSIGNMENT = "The assignment of 'global only' roles via tenant assignment is not allowed";

    public static final String ERROR_CANNOT_ASSIGN_TENANT_ONLY_ROLES_VIA_GLOBAL_ASSIGNMENT = "The assignment of 'tenant only' roles via global assignment is not allowed";

    public static final String ERROR_CANNOT_ADD_GLOBAL_ROLE_TO_USER_ERROR_MESSAGE = "Cannot add global role to user. Role already assigned as a tenant role.";
    public static final String ERROR_CANNOT_ADD_ROLE_TO_USER_ON_TENANT_ERROR_MESSAGE = "Cannot add role to user on tenant. Role already assigned globally.";

    public static final String ROLE_ID_NOT_FOUND_ERROR_MESSAGE = "Role with ID %s not found.";

    public static final String USER_NOT_FOUND_ERROR_MESSAGE = "User with ID %s not found.";

    public static final String LIST_USERS_FOR_TENANT_PARAM_ERROR_MESSAGE = "'roleId' and 'contactId' query parameters are mutually exclusive. Please supply one or the other.";

    public static final String UPDATE_USER_CANNOT_UPDATE_HIGHER_LEVEL_USER_ERROR_MESSAGE = "Cannot update user with same or higher access level";
    public static final String USERNAME_CANNOT_BE_UPDATED_ERROR_MESSAGE = "A user's username cannot be updated.";
    public static final String ERROR_CANNOT_UPDATE_USER_WITH_HIGHER_ACCESS = "Cannot update user with same or higher access level";

    public static final String ERROR_SWITCH_RCN_ON_DOMAIN_CONTAINING_RCN_TENANT ="The domain cannot contain an RCN tenant. Remove the RCN tenant from the domain first.";
    public static final String ERROR_SWITCH_RCN_ON_DOMAIN_MISSING_RCN = "Destination RCN required.";

    public static final String ERROR_DELETE_ROLE_WITH_USERS_ASSIGNED = "Deleting the role associated with one or more users is not allowed";
    public static final String ERROR_DELETE_ROLE_WITH_GROUPS_ASSIGNED = "Deleting the role associated with one or more groups is not allowed";

    public static final String GRANTING_ROLES_TO_USER_ERROR_MESSAGE = "Error granting roles to user with ID %s.";

    public static final String METADATA_NOT_FOUND_ERROR_MESSAGE = "No metadata found for identity provider '%s'.";

    public static final String PREFIXED_IDENTITY_ROLE_ERROR_MESSAGE = "Role prefixed with 'identity:' cannot be deleted";
    public static final String IDENTITY_USER_TYPE_ROLE_ERROR_MESSAGE = "Identity user type roles cannot be deleted";

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
    private JAXBObjectFactories jaxbObjectFactories;

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
    private UserGroupService userGroupService;

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
    private Paginator<Application> applicationPaginator;

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

    @Autowired
    @Qualifier("tokenRevocationService")
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private ProvisionedUserSourceFederationHandler provisionedUserSourceFederationHandler;

    @Autowired
    TenantTypeService tenantTypeService;

    @Autowired
    TenantTypeConverterCloudV20 tenantTypeConverter;

    @Autowired
    IdentityProviderDefaultPolicyPropertyValidator defaultPolicyPropertyValidator;

    @Autowired
    private Paginator<com.rackspace.idm.domain.entity.TenantType> tenantTypePaginator;

    @Autowired
    private RoleAssignmentConverter roleAssignmentConverter;

    @Autowired
    private RuleService ruleService;

    private com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory raxAuthObjectFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();

    private LogoutResponseMarshaller marshaller = new LogoutResponseMarshaller();

    private Map<String, JAXBElement<Extension>> extensionMap;

    private JAXBElement<Extensions> currentExtensions;

    private static final Logger logger = LoggerFactory.getLogger(DefaultCloud20Service.class);

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, EndpointTemplate endpoint) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(Integer.toString(endpoint.getId()));
            tenant.getBaseUrlIds().add(String.valueOf(endpoint.getId()));
            this.tenantService.updateTenant(tenant);
            return Response.ok(
                    jaxbObjectFactories.getOpenStackIdentityV2Factory().createEndpoint(endpointConverterCloudV20.toEndpoint(baseUrl)).getValue());
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
            ObjectFactory openStackIdentityExtKscatalogV1Factory = jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory();
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
            return Response.ok(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory().createEndpointTemplate(value).getValue());
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
            org.openstack.docs.identity.api.v2.ObjectFactory openStackIdentityV2Factory = jaxbObjectFactories.getOpenStackIdentityV2Factory();
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
            if (role.getAssignmentType() != null && RoleAssignmentEnum.fromValue(role.getAssignmentType()) == RoleAssignmentEnum.GLOBAL) {
                throw new ForbiddenException(ERROR_CANNOT_ASSIGN_GLOBAL_ONLY_ROLES_VIA_TENANT_ASSIGNMENT);
            }

            if (IdentityUserTypeEnum.isIdentityUserTypeRoleName(role.getName())) {
                throw new ForbiddenException("Cannot add specified role to tenants on users.");
            }

            TenantRole existingTenantRole = tenantService.getTenantRoleForUserById(user, roleId);
            if (existingTenantRole != null && existingTenantRole.getTenantIds().isEmpty()) {
                throw new BadRequestException(ERROR_CANNOT_ADD_ROLE_TO_USER_ON_TENANT_ERROR_MESSAGE);
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

            // NOTE: At this point we can't do a contract change, but this should've been a HTTP 204 No Content.
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
            org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory openStackIdentityExtKsadmnV1Factory = jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory();
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

            validator20.validateTenantType(tenant);

            // Our implementation has the id and the name the same
            tenant.setId(tenant.getName());
            final Tenant savedTenant = this.tenantConverterCloudV20.fromTenant(tenant);

            if (identityConfig.getReloadableConfig().shouldSetDefaultTenantTypeOnCreation() &&
                    savedTenant.getTypes().isEmpty()) {
                String inferredType = tenantService.inferTenantTypeForTenantId(savedTenant.getTenantId());
                if (inferredType != null) {
                    savedTenant.getTypes().add(inferredType);
                }
            }

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
            org.openstack.docs.identity.api.v2.ObjectFactory openStackIdentityV2Factory = jaxbObjectFactories.getOpenStackIdentityV2Factory();
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

            BaseUser retrievedUser = identityUserService.getEndUserById(userId);

            if (!authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null)) {
                user.setContactId(null);

                // Only service and identity admins can update a federated user.
                if (retrievedUser != null && retrievedUser instanceof FederatedUser) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }

            if (retrievedUser == null) {
                String errMsg = String.format(USER_NOT_FOUND_ERROR_MESSAGE, userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            if (!userId.equals(user.getId()) && user.getId() != null) {
                throw new BadRequestException(ID_MISMATCH);
            }

            if (StringUtils.isNotBlank(user.getContactId())) {
                validator20.validateStringMaxLength("contactId", user.getContactId(), Validator20.MAX_LENGTH_64);
            }

            if (retrievedUser instanceof FederatedUser) {
                FederatedUser federatedUserDO = new FederatedUser();
                federatedUserDO.setUniqueId(retrievedUser.getUniqueId());

                // Allowed attributes for update
                if (StringUtils.isNotBlank(user.getContactId())) {
                    federatedUserDO.setContactId(user.getContactId());
                }

                identityUserService.updateFederatedUser(federatedUserDO);
            } else {
                // Update provisioned user
                BaseUser caller = userService.getUserByScopeAccess(scopeAccessByAccessToken);

                if (user.getPassword() != null) {
                    validator.validatePasswordForCreateOrUpdate(user.getPassword());
                }

                boolean isDisabled = retrievedUser.isDisabled();

                if (user.getUsername() != null) {
                    if (!StringUtils.equalsIgnoreCase(retrievedUser.getUsername(), user.getUsername()) && !userService.isUsernameUnique(user.getUsername())) {
                        throw new DuplicateUsernameException("User with username: '" + user.getUsername() + "' already exists.");
                    }
                }

                boolean isUpdatingSelf = caller.getId().equals(userId);
                IdentityUserTypeEnum callerType = authorizationService.getIdentityTypeRoleAsEnum(caller);

                // Just identity admins and service admins can update 'tokenFormat', but only when ae tokens are enabled
                if (!(IdentityUserTypeEnum.SERVICE_ADMIN == callerType || IdentityUserTypeEnum.IDENTITY_ADMIN == callerType) ||
                        !identityConfig.getReloadableConfig().getFeatureAETokensDecrypt()) {
                    user.setTokenFormat(null);
                }

                if (!isUpdatingSelf) {
                    precedenceValidator.verifyCallerPrecedenceOverUser(caller, retrievedUser);
                }

                if (callerType.isDomainBasedAccessLevel() && !StringUtils.equals(caller.getDomainId(), retrievedUser.getDomainId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }

                if (StringUtils.isNotBlank(user.getUsername()) &&
                        !retrievedUser.getUsername().equals(user.getUsername()) &&
                        !identityConfig.getReloadableConfig().isUsernameUpdateAllowed()) {
                    throw new ForbiddenException(USERNAME_CANNOT_BE_UPDATED_ERROR_MESSAGE);
                }

                if (!StringUtils.isBlank(user.getUsername())) {
                    validator.isUsernameValid(user.getUsername());
                }

                if (user.isEnabled() != null && !user.isEnabled() && isUpdatingSelf) {
                    throw new BadRequestException("User cannot enable/disable his/her own account.");
                }

                User userDO = this.userConverterCloudV20.fromUser(user);
                if (user.isEnabled() == null) {
                    userDO.setEnabled(((User) retrievedUser).getEnabled());
                }
                if (userDO.isDisabled() && !isDisabled) {
                    atomHopperClient.asyncPost((User) retrievedUser, AtomHopperConstants.DISABLED);
                } else if (!userDO.isDisabled() && isDisabled) {
                    atomHopperClient.asyncPost((User) retrievedUser, AtomHopperConstants.ENABLED);
                }

                Boolean updateRegion = true;
                if (userDO.getRegion() != null) {
                    if (userDO.getRegion().equals(((User) retrievedUser).getRegion())) {
                        updateRegion = false;
                    }
                }
                userDO.setId(retrievedUser.getId());
                if (StringUtils.isBlank(user.getUsername())) {
                    userDO.setUsername(retrievedUser.getUsername());
                }
                if (userDO.getRegion() != null && updateRegion) {
                    defaultRegionService.validateDefaultRegion(userDO.getRegion(), (User) retrievedUser);
                }
                userService.updateUser(userDO);

                atomHopperClient.asyncPost(userDO, AtomHopperConstants.UPDATE);
            }

            EndUser endUser = identityUserService.getEndUserById(userId);
            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(endUser)).getValue());
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
    public ResponseBuilder grantRolesToUser(HttpHeaders httpHeaders, String authToken, String userId, RoleAssignments roleAssignments) {
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            // Verify target user exists
            User user = userService.checkAndGetUserById(userId);

            // Verify caller has access to modify target user
            authorizationService.verifyEffectiveCallerHasManagementAccessToUser(caller, user);

            if (roleAssignments == null) {
                throw new BadRequestException("Must supply a set of assignments");
            }

            // Get caller type to validate role access
            IdentityUserTypeEnum callerUserType = authorizationService.getIdentityTypeRoleAsEnum(caller);

            userService.replaceRoleAssignmentsOnUser(user, roleAssignments, callerUserType.getLevelAsInt());

            // Retrieve the first 1000 assigned roles on the user
            PaginatorContext<TenantRole> tenantRolePage = userService.getRoleAssignmentsOnUser(user, new PaginationParams(0, 1000));

            return Response.ok(roleAssignmentConverter.toRoleAssignmentsWeb(tenantRolePage.getValueList()));
        } catch (Exception ex) {
            String errMsg = String.format(GRANTING_ROLES_TO_USER_ERROR_MESSAGE, userId);
            logger.error(errMsg);
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserManagedLevelAccess(scopeAccessByAccessToken);

            ClientRole cRole = checkAndGetClientRole(roleId);
            if (cRole.getAssignmentType() != null && RoleAssignmentEnum.fromValue(cRole.getAssignmentType()) == RoleAssignmentEnum.TENANT) {
                throw new ForbiddenException(ERROR_CANNOT_ASSIGN_TENANT_ONLY_ROLES_VIA_GLOBAL_ASSIGNMENT);
            }

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, cRole);

            if (user.getId().equals(caller.getId())) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            TenantRole tenantRole = tenantService.getTenantRoleForUserById(user, roleId);
            if (tenantRole != null && !tenantRole.getTenantIds().isEmpty()) {
                throw new BadRequestException(ERROR_CANNOT_ADD_GLOBAL_ROLE_TO_USER_ERROR_MESSAGE);
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
            // NOTE: At this point we can't do a contract change, but this should've been a HTTP 204 No Content.
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
            NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthRackerPwd.getTransactionName());
            PasswordCredentialsBase creds = (PasswordCredentialsBase) authenticationRequest.getCredential().getValue();
            creds.setUsername(creds.getUsername().trim());
            validator20.validatePasswordCredentials(creds);
            Domain domainDO = domainConverterCloudV20.fromDomain(domain);
            UserAuthenticationResult result = authenticationService.authenticateDomainUsernamePassword(creds.getUsername(), creds.getPassword(), domainDO);
            racker = (Racker) result.getUser();
            authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSWORD);
        } else if (authenticationRequest.getCredential().getValue() instanceof RsaCredentials) {
            NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthRackerRsa.getTransactionName());
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

    @Override
    public Response.ResponseBuilder authenticateForForgotPassword(HttpHeaders httpHeaders, ForgotPasswordCredentials forgotPasswordCredentials) {
        try {
            //this will throw exception if auth does NOT succeed
            AuthenticationRequest authRequestAdapter = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticationRequest();
            authRequestAdapter.setCredential(raxAuthObjectFactory.createForgotPasswordCredentials(forgotPasswordCredentials));

            AuthResponseTuple auth = authWithForgotPasswordCredentials.authenticateForAuthResponse(authRequestAdapter);

            Response.ResponseBuilder builder = Response.noContent();
            return builder.header(GlobalConstants.X_USER_NAME, forgotPasswordCredentials.getUsername());
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

            return Response.noContent().header(GlobalConstants.X_USER_NAME, user.getUsername());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) {
        return authenticateInternal(httpHeaders, authenticationRequest, false);
    }

    @Override
    public Response.ResponseBuilder authenticateApplyRcnRoles(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) {
        return authenticateInternal(httpHeaders, authenticationRequest, true);
    }


    public Response.ResponseBuilder authenticateInternal(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest, boolean applyRcnRoles) {
        NewRelic.setTransactionName(null, NewRelicTransactionNames.V2Auth.getTransactionName());

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
                // Racker Auth
                if (authenticationRequest.getScope() != null) {
                    // Scoped tokens not supported here
                    throw new ForbiddenException(SETUP_MFA_SCOPE_FORBIDDEN);
                }

                AuthenticateResponse auth = authenticateFederatedDomain(authenticationRequest, domain);
                return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createAccess(auth).getValue());
            }

            if (authenticationRequest.getCredential() != null && authenticationRequest.getCredential().getValue() instanceof PasscodeCredentials) {
                NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthMfaSecond.getTransactionName());

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
                NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthWithToken.getTransactionName());
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
                    String tname = applyRcnRoles ? NewRelicTransactionNames.V2AuthWithPwdRcn.getTransactionName() : NewRelicTransactionNames.V2AuthWithPwd.getTransactionName();
                    NewRelic.setTransactionName(null, tname);
                    userAuthenticationFactor = authWithPasswordCredentials;
                    canUseMfaWithCredential = true;
                } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                    NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthWithApi.getTransactionName());
                    userAuthenticationFactor = authWithApiKeyCredentials;
                }
                else {
                    throw new BadRequestException("Unknown credential type");
                }

                UserAuthenticationResult authResult = userAuthenticationFactor.authenticate(authenticationRequest);

                if (canUseMfaWithCredential && ((User)authResult.getUser()).isMultiFactorEnabled()) {
                    NewRelic.setTransactionName(null, NewRelicTransactionNames.V2AuthMfaFirst.getTransactionName());
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

            if (applyRcnRoles) {
                return authenticateResponseService.buildAuthResponseForAuthenticateApplyRcn(authResponseTuple, authenticationRequest);
            } else {
                return authenticateResponseService.buildAuthResponseForAuthenticate(authResponseTuple, authenticationRequest);
            }
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder authenticateFederated(HttpHeaders httpHeaders, byte[] samlResponseBytes, boolean applyRcnRoles) {
        //determine API version
        List<String> identityVersionHeaderVals = httpHeaders.getRequestHeader(GlobalConstants.HEADER_IDENTITY_API_VERSION);
        if (CollectionUtils.isEmpty(identityVersionHeaderVals)) {
            identityVersionHeaderVals = ImmutableList.of(GlobalConstants.FEDERATION_API_V1_0);
        }

        String apiVersion = identityVersionHeaderVals.get(0);
        SamlAuthResponse samlAuthResponse = null;
        try {
            org.opensaml.saml.saml2.core.Response samlResponse = samlUnmarshaller.unmarshallResponse(samlResponseBytes);
            if (GlobalConstants.FEDERATION_API_V1_0.equalsIgnoreCase(apiVersion)) {
                samlAuthResponse = federatedIdentityService.processSamlResponse(samlResponse);
            } else if (GlobalConstants.FEDERATION_API_V2_0.equalsIgnoreCase(apiVersion)) {
                samlAuthResponse = federatedIdentityService.processV2SamlResponse(samlResponse, applyRcnRoles);
            } else {
                exceptionHandler.badRequestExceptionResponse(String.format("Unsupported %s version", GlobalConstants.HEADER_IDENTITY_API_VERSION));
            }
            AuthenticateResponse response = authConverterCloudV20.toAuthenticationResponse(samlAuthResponse);

            // Do not expose the core contact ID through federated auth
            response.getUser().setContactId(null);

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createAccess(response).getValue());
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
            org.opensaml.saml.saml2.core.LogoutRequest logoutRequest = samlUnmarshaller.unmarshallLogoutRequest(samlLogoutRequestBytes);
            logoutResponse = federatedIdentityService.processLogoutRequest(logoutRequest);
        } catch (BadRequestException ex) {
            logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(null, StatusCode.REQUESTER, ex.getMessage(), ex);
        } catch (Exception ex) {
            logoutResponse = SamlLogoutResponseUtil.createErrorLogoutResponse(null, StatusCode.RESPONDER, "Encountered an exception processing federation logout request", ex);
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
        Element element = marshaller.marshall(logoutResponse);
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

            IdentityProperty defaultPolicyProperty = federatedIdentityService.checkAndGetDefaultMappingPolicyProperty();
            newProvider.setPolicy(defaultPolicyProperty.getValue());
            newProvider.setPolicyFormat(defaultPolicyProperty.getValueType().toUpperCase());

            if (newProvider.getEnabled() == null) {
                newProvider.setEnabled(true);
            }

            federatedIdentityService.addIdentityProvider(newProvider);
            atomHopperClient.asyncPostIdpEvent(newProvider, EventType.CREATE);
            ResponseBuilder builder = Response.created(uriInfo.getRequestUriBuilder().path(newProvider.getProviderId()).build());
            return builder.entity(identityProviderConverterCloudV20.toIdentityProvider(newProvider));
        } catch (DuplicateException de) {
            return exceptionHandler.conflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addIdentityProviderUsingMetadata(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, byte[] metadata) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            // Verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()));

            verifyUserIsNotInDefaultDomain(caller);

            verifyUserIsNotInRaxRestrictedGroup(caller);

            IdentityProvider identityProvider = identityProviderConverterCloudV20.toIdentityProvider(metadata, caller.getDomainId());

            validator20.validateIdentityProviderMetadataForCreation(identityProvider, caller.getDomainId());

            // Increment a digit at end of name until a unique name is found
            int count = 2;
            String uniqueName = identityProvider.getName();
            while (federatedIdentityService.getIdentityProviderByName(uniqueName) != null) {
                uniqueName = String.format("%s_%d", identityProvider.getName(), count++);
            }
            identityProvider.setName(uniqueName);

            com.rackspace.idm.domain.entity.IdentityProvider newProvider =
                    identityProviderConverterCloudV20.fromIdentityProvider(identityProvider);

            IdentityProperty defaultPolicyProperty = federatedIdentityService.checkAndGetDefaultMappingPolicyProperty();
            newProvider.setPolicy(defaultPolicyProperty.getValue());
            newProvider.setPolicyFormat(defaultPolicyProperty.getValueType().toUpperCase());

            newProvider.setEnabled(true);

            federatedIdentityService.addIdentityProvider(newProvider);
            atomHopperClient.asyncPostIdpEvent(newProvider, EventType.CREATE);
            ResponseBuilder builder = Response.created(uriInfo.getRequestUriBuilder().path(newProvider.getProviderId()).build());
            return builder.entity(identityProviderConverterCloudV20.toIdentityProvider(newProvider));
        } catch (DuplicateException de) {
            return exceptionHandler.conflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateIdentityProvider(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String providerId, IdentityProvider provider) {
        try {
            // Verify token exists and valid
            ScopeAccess sa = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            // Verify user has appropriate role
            if(identityConfig.getReloadableConfig().getEnableExternalUserIdpManagement()) {
                authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                        IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                        IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                        IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                        IdentityRole.RCN_ADMIN.getRoleName()));
            } else {
                authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName());
            }

            com.rackspace.idm.domain.entity.IdentityProvider existingProvider = federatedIdentityService.checkAndGetIdentityProviderWithMetadataById(providerId);

            boolean isUserAdmin = authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName());
            boolean isUserManage = authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_MANAGER.getRoleName());
            boolean isRcnAdmin = authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.RCN_ADMIN.getRoleName());
            boolean isIdentityProviderManager = authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName());

            // TODO: Refactor this so the fields a given user type can update aren't spread across multiple classes

            if (isUserAdmin || isUserManage) {
                verifyDomainUserHasAccessToIdentityProviderMetadata(existingProvider, caller);
                validator20.validateIdentityProviderForUpdateForUserAdminOrUserManage(provider, existingProvider);
            } else if (isRcnAdmin) {
                verifyDomainUserHasAccessToIdentityProviderMetadata(existingProvider, caller);
                validator20.validateIdentityProviderForUpdateForRcnAdmin(provider, existingProvider);
            } else {
                validator20.validateIdentityProviderForUpdateForIdentityProviderManager(provider, existingProvider);
            }

            // Copy over the only attributes allowed to be updated
            if (provider.getName() != null) {
                existingProvider.setName(provider.getName());
            }

            if (provider.getDescription() != null) {
                existingProvider.setDescription(provider.getDescription());
            }

            if (isIdentityProviderManager && existingProvider.getXmlMetadata() == null && provider.getAuthenticationUrl() != null) {
                existingProvider.setAuthenticationUrl(provider.getAuthenticationUrl());
            }

            if (provider.getEmailDomains() != null) {
                EmailDomains suppliedEmailDomains = provider.getEmailDomains();
                if (!suppliedEmailDomains.getEmailDomain().isEmpty()) {
                    Set<String> emailDomains = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                    emailDomains.addAll(suppliedEmailDomains.getEmailDomain());
                    existingProvider.setEmailDomains(new ArrayList<>(emailDomains));
                }
            }

            boolean revokeAllIdpTokens = false;
            if (provider.isEnabled() != null && IdentityProviderFederationTypeEnum.DOMAIN == existingProvider.getFederationTypeAsEnum()) {
                if (existingProvider.getEnabled() != null && existingProvider.getEnabled() && !provider.isEnabled()) {
                    revokeAllIdpTokens = true;
                }
                existingProvider.setEnabled(provider.isEnabled());
            }

            List<String> existingProviderApprovedDomainIds = existingProvider.getApprovedDomainIds() != null ? existingProvider.getApprovedDomainIds() : Collections.EMPTY_LIST ;

            List<String> suppliedProviderApprovedDomainIds = null;
            if (provider.getApprovedDomainIds() != null && CollectionUtils.isNotEmpty(provider.getApprovedDomainIds().getApprovedDomainId())) {
                // Remove duplicates within supplied list by adding to a hashset
                Set<String> approvedDomainIds = new LinkedHashSet<>(provider.getApprovedDomainIds().getApprovedDomainId());
                suppliedProviderApprovedDomainIds = new ArrayList<>(approvedDomainIds);

                if (isIdentityProviderManager || isRcnAdmin) {
                    existingProvider.setApprovedDomainIds(suppliedProviderApprovedDomainIds);
                }
            }

            federatedIdentityService.updateIdentityProvider(existingProvider); //update
            if (revokeAllIdpTokens) {
                tokenRevocationService.revokeAllTokensForIdentityProvider(existingProvider.getProviderId());
            }
            atomHopperClient.asyncPostIdpEvent(existingProvider, EventType.UPDATE);

            if (isIdentityProviderManager || isRcnAdmin) {
                /*
                 Only delete fed users for the IDP if the existingProviderApprovedDomainIds contains one or more domainIds
                 that were not included in the new set of domainIds
                */
                if (suppliedProviderApprovedDomainIds != null && CollectionUtils.isNotEmpty(suppliedProviderApprovedDomainIds)
                        && !CollectionUtils.isEqualCollection(existingProviderApprovedDomainIds, suppliedProviderApprovedDomainIds)) {
                    Iterable<FederatedUser> federatedUsersForDeletion = identityUserService.getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(
                            suppliedProviderApprovedDomainIds, providerId);
                    for (FederatedUser federatedUser : federatedUsersForDeletion) {
                        // Delete federated user
                        provisionedUserSourceFederationHandler.deleteFederatedUser(federatedUser);
                    }
                }
            }
            ResponseBuilder builder = Response.ok(uriInfo.getRequestUriBuilder().path(existingProvider.getProviderId()).build());
            return builder.entity(identityProviderConverterCloudV20.toIdentityProvider(existingProvider));
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateIdentityProviderUsingMetadata(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String identityProviderId, byte[] metadata) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            // Verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()));

            verifyUserIsNotInRaxRestrictedGroup(caller);

            com.rackspace.idm.domain.entity.IdentityProvider identityProvider = federatedIdentityService.checkAndGetIdentityProviderWithMetadataById(identityProviderId);

            verifyDomainUserHasAccessToIdentityProviderMetadata(identityProvider, caller);

            IdentityProvider idpRequest = identityProviderConverterCloudV20.toIdentityProvider(metadata, caller.getDomainId());
            com.rackspace.idm.domain.entity.IdentityProvider idpUpdates = identityProviderConverterCloudV20.fromIdentityProvider(idpRequest);

            if (!identityProvider.getUri().equals(idpUpdates.getUri())) {
                throw new BadRequestException("The issuer for the IDP specified in the request does not mach the issuer for the IDP being updated.");
            }

            validator20.validateIdentityProviderAuthenticationUrl(idpRequest);

            validator20.validateIdentityProviderPublicCertificates(idpRequest);

            // Set only the attributes that we allow to be updated through metadata
            identityProvider.setAuthenticationUrl(idpUpdates.getAuthenticationUrl());
            identityProvider.setUserCertificates(idpUpdates.getUserCertificates());
            identityProvider.setXmlMetadata(idpUpdates.getXmlMetadata());

            federatedIdentityService.updateIdentityProvider(identityProvider);
            atomHopperClient.asyncPostIdpEvent(identityProvider, EventType.UPDATE);

            // We do not expose the IDP's metadata within the IDP. Use the get metadata call to get the metadata
            identityProvider.setXmlMetadata(null);

            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().
                    createIdentityProvider(identityProviderConverterCloudV20.toIdentityProvider(identityProvider)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private void verifyUserIsNotInDefaultDomain(BaseUser user) {
        // Verify domain for user
        if (StringUtils.isBlank(user.getDomainId()) || user.getDomainId().equals(identityConfig.getReloadableConfig().getGroupDefaultDomainId())
                || user.getDomainId().equals(identityConfig.getReloadableConfig().getTenantDefaultDomainId())) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    private void verifyUserIsNotInRaxRestrictedGroup(BaseUser user) {
        for (Group group : userService.getGroupsForUser(user.getId())) {
            if (group.getName().equalsIgnoreCase(GlobalConstants.RAX_STATUS_RESTRICTED_GROUP_NAME)) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }
        }
    }

    /**
     * This method does some basic checks on the user to verify that the user has access to the IDP's metadata. This method
     * assumes that the user is a "domain user" (a user-admin/manager or an rcn:admin user).
     *
     * The current restrictions that we do for the user and the IDP are
     * 1) The IDP just be a DOMAIN IDP
     * 2) The IDP must have one and only one approved domain
     * 3) The user must be in that domain (identity-admin/manager)
     * 4) OR the user must be an rcn:admin for the RCN that the IDP is approved for
     * @param identityProvider
     * @param user
     */
    private void verifyDomainUserHasAccessToIdentityProviderMetadata(com.rackspace.idm.domain.entity.IdentityProvider identityProvider, BaseUser user) {
        List<String> approvedDomainIds = identityProvider.getApprovedDomainIds();

        if(!identityProvider.getFederationTypeAsEnum().equals(IdentityProviderFederationTypeEnum.DOMAIN)) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        verifyIdpHasOneAndOnlyOneApprovedDomainId(identityProvider);

        if (!approvedDomainIds.get(0).equals(user.getDomainId())) {
            // Verify domain within the same RCN as the IDP's approvedDomainId if user has the "rcn:admin" role
            if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.RCN_ADMIN.getRoleName())) {
                Domain callerDomain = domainService.getDomain(user.getDomainId());
                Domain idpApprovedDomain = domainService.getDomain(approvedDomainIds.get(0));

                if (!assertDomainsBelongToRcn(callerDomain, idpApprovedDomain)) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            } else {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }
        }
    }

    private void verifyIdpHasOneAndOnlyOneApprovedDomainId(com.rackspace.idm.domain.entity.IdentityProvider idp) {
        List<String> approvedDomainIds = idp.getApprovedDomainIds();
        if (approvedDomainIds == null || approvedDomainIds.size() != 1) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    @Override
    public ResponseBuilder getIdentityProvider(HttpHeaders httpHeaders, String authToken, String providerId) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            // Verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                    IdentityRole.IDENTITY_PROVIDER_READ_ONLY.getRoleName(),
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()));

            verifyUserIsNotInDefaultDomain(caller);
            verifyUserIsNotInRaxRestrictedGroup(caller);

            com.rackspace.idm.domain.entity.IdentityProvider existingProvider = federatedIdentityService.checkAndGetIdentityProviderWithMetadataById(providerId);

            if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()))) {
                verifyDomainUserHasAccessToIdentityProviderMetadata(existingProvider, caller);
            }

            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProvider(identityProviderConverterCloudV20.toIdentityProvider(existingProvider)).getValue());
        } catch (SizeLimitExceededException ex) {
            throw new BadRequestException(ex.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getIdentityProviders(HttpHeaders httpHeaders, String authToken, IdentityProviderSearchParams identityProviderSearchParams) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            // Verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                    IdentityRole.IDENTITY_PROVIDER_READ_ONLY.getRoleName(),
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()));

            verifyUserIsNotInDefaultDomain(caller);
            verifyUserIsNotInRaxRestrictedGroup(caller);

            // TODO: Move all filter logic to methods within the federatedIdentityService consuming an IdentityProviderSearchCriteria object

            // emailDomain query param must be mutually exclusive from other query params.
            if (StringUtils.isNotBlank(identityProviderSearchParams.getEmailDomain())
                    && identityProviderSearchParams.getSearchParamsMap().size() != 1) {
                throw new BadRequestException(FEDERATION_LIST_IDP_EMAIL_DOMAIN_WITH_OTHER_PARAMS_ERROR_MSG);
            }

            IdentityProviderTypeFilterEnum idpFilter = null;
            if (StringUtils.isNotBlank(identityProviderSearchParams.idpType)) {
                idpFilter = IdentityProviderTypeFilterEnum.parseIdpTypeFilter(identityProviderSearchParams.idpType);
                if(idpFilter == null) {
                    throw new BadRequestException(String.format(FEDERATION_IDP_TYPE_ERROR_MESSAGE, IdentityProviderTypeFilterEnum.EXPLICIT.name()));
                }
            }

            //prevent use of domain and tenant filter at the same time
            if (StringUtils.isNotBlank(identityProviderSearchParams.approvedDomainId) && StringUtils.isNotBlank(identityProviderSearchParams.approvedTenantId)) {
                throw new BadRequestException(FEDERATION_IDP_FILTER_CONFLICT_ERROR_MESSAGE);
            }

            if (StringUtils.isNotBlank(identityProviderSearchParams.approvedTenantId)) {
                //verify that the tenant exists if trying to filter by tenant
                Tenant tenantForFilter = tenantService.getTenant(identityProviderSearchParams.approvedTenantId);
                //return empty list if the tenant does not exist
                if (tenantForFilter == null) {
                    return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProviders(identityProviderConverterCloudV20.toIdentityProviderList(new ArrayList<com.rackspace.idm.domain.entity.IdentityProvider>())).getValue());
                }
                //do not allow for tenants assigned to the default domain to be used in the filter
                if(StringUtils.isBlank(tenantForFilter.getDomainId()) ||
                        identityConfig.getReloadableConfig().getTenantDefaultDomainId().equals(tenantForFilter.getDomainId())) {
                    throw new BadRequestException(FEDERATION_IDP_FILTER_TENANT_NO_DOMAIN_ERROR_MESSAGE);
                }
                identityProviderSearchParams.approvedDomainId = tenantForFilter.getDomainId();
            }

            //return an empty list if trying to filter by a domain that does not exist
            if (StringUtils.isNotBlank(identityProviderSearchParams.approvedDomainId)) {
                Domain domain = domainService.getDomain(identityProviderSearchParams.approvedDomainId);
                if (domain == null) {
                    return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProviders(identityProviderConverterCloudV20.toIdentityProviderList(new ArrayList<com.rackspace.idm.domain.entity.IdentityProvider>())).getValue());
                }
            }

            List<com.rackspace.idm.domain.entity.IdentityProvider> providerEntities = new ArrayList<>();
            if (StringUtils.isNotBlank(identityProviderSearchParams.emailDomain)) {
                com.rackspace.idm.domain.entity.IdentityProvider identityProvider = federatedIdentityService.getIdentityProviderByEmailDomain(identityProviderSearchParams.emailDomain);
                if (identityProvider != null) {
                    providerEntities = Collections.singletonList(identityProvider);
                }
            } else if (StringUtils.isNotBlank(identityProviderSearchParams.name)) {
                com.rackspace.idm.domain.entity.IdentityProvider identityProvider = null;
                if (StringUtils.isNotBlank(identityProviderSearchParams.approvedDomainId) && IdentityProviderTypeFilterEnum.EXPLICIT.equals(idpFilter)) {
                    identityProvider = federatedIdentityService.getIdentityProviderExplicitlyApprovedForDomain(identityProviderSearchParams.name, identityProviderSearchParams.approvedDomainId);
                } else if (StringUtils.isNotBlank(identityProviderSearchParams.approvedDomainId)) {
                    identityProvider = federatedIdentityService.getIdentityProviderApprovedForDomain(identityProviderSearchParams.name, identityProviderSearchParams.approvedDomainId);
                } else if (IdentityProviderTypeFilterEnum.EXPLICIT.equals(idpFilter)) {
                    identityProvider = federatedIdentityService.getIdentityProviderExplicitlyApprovedForAnyDomain(identityProviderSearchParams.name);
                } else {
                    identityProvider = federatedIdentityService.getIdentityProviderByName(identityProviderSearchParams.name);
                }

                if (identityProvider != null) {
                    // issuer is case sensitive
                    if (StringUtils.isNotBlank(identityProviderSearchParams.issuer) && !identityProvider.getUri().equals(identityProviderSearchParams.issuer)) {
                        providerEntities = new ArrayList<>();
                    } else {
                        providerEntities = Collections.singletonList(identityProvider);
                    }
                }
            } else if (StringUtils.isNotBlank(identityProviderSearchParams.issuer)) {
                com.rackspace.idm.domain.entity.IdentityProvider identityProvider = null;
                if (StringUtils.isNotBlank(identityProviderSearchParams.approvedDomainId) && IdentityProviderTypeFilterEnum.EXPLICIT.equals(idpFilter)) {
                    identityProvider = federatedIdentityService.getIdentityProviderExplicitlyApprovedForDomainByIssuer(identityProviderSearchParams.issuer, identityProviderSearchParams.approvedDomainId);
                } else if (StringUtils.isNotBlank(identityProviderSearchParams.approvedDomainId)) {
                    identityProvider = federatedIdentityService.getIdentityProviderApprovedForDomainByIssuer(identityProviderSearchParams.issuer, identityProviderSearchParams.approvedDomainId);
                } else if (IdentityProviderTypeFilterEnum.EXPLICIT.equals(idpFilter)) {
                    identityProvider = federatedIdentityService.getIdentityProviderExplicitlyApprovedForAnyDomainByIssuer(identityProviderSearchParams.issuer);
                } else {
                    identityProvider = federatedIdentityService.getIdentityProviderByIssuer(identityProviderSearchParams.issuer);
                }

                if (identityProvider != null) {
                    providerEntities = Collections.singletonList(identityProvider);
                }
            } else if (StringUtils.isNotBlank(identityProviderSearchParams.approvedDomainId) && IdentityProviderTypeFilterEnum.EXPLICIT.equals(idpFilter)) {
                providerEntities = federatedIdentityService.findIdentityProvidersExplicitlyApprovedForDomain(identityProviderSearchParams.approvedDomainId);
            } else if (StringUtils.isNotBlank(identityProviderSearchParams.approvedDomainId)) {
                providerEntities = federatedIdentityService.findIdentityProvidersApprovedForDomain(identityProviderSearchParams.approvedDomainId);
            } else if (IdentityProviderTypeFilterEnum.EXPLICIT.equals(idpFilter)) {
                providerEntities = federatedIdentityService.findIdentityProvidersExplicitlyApprovedForAnyDomain();
            } else {
                providerEntities = federatedIdentityService.findAllIdentityProviders();
            }

            /*
            If the user is authorized to call the service based on having the identity:user-admin, identity:user-manage,
            or rcn:admin roles, the IDPs returned must be further filtered based on caller.
             */
            if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()))) {
                List<com.rackspace.idm.domain.entity.IdentityProvider> filteredProviderEntities = new ArrayList<>();
                for (com.rackspace.idm.domain.entity.IdentityProvider idp : providerEntities) {
                    try {
                        verifyDomainUserHasAccessToIdentityProviderMetadata(idp, caller);
                        filteredProviderEntities.add(idp);
                    } catch (ForbiddenException ex) {
                        String infoMsg = String.format("User with ID %s does not have access to Identity Provider with ID %s.",
                                                       caller.getId(), idp.getProviderId());
                        logger.info(infoMsg);
                    }
                }
                providerEntities = filteredProviderEntities;
            }

            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().createIdentityProviders(identityProviderConverterCloudV20.toIdentityProviderList(providerEntities)).getValue());
        } catch (SizeLimitExceededException ex) {
            return exceptionHandler.exceptionResponse(new ForbiddenException(ex.getMessage())); //translate size limit to forbidden
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getIdentityProvidersMetadata(HttpHeaders httpHeaders, String authToken, String providerId) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            verifyUserIsNotInDefaultDomain(caller);

            com.rackspace.idm.domain.entity.IdentityProvider identityProvider;
            if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()))) {

                verifyUserIsNotInRaxRestrictedGroup(caller);

                identityProvider = federatedIdentityService.checkAndGetIdentityProvider(providerId);

                verifyDomainUserHasAccessToIdentityProviderMetadata(identityProvider, caller);

            } else {
                authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                        IdentityRole.IDENTITY_PROVIDER_READ_ONLY.getRoleName(),
                        IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName()));
                federatedIdentityService.checkAndGetIdentityProvider(providerId);
            }

            identityProvider = federatedIdentityService.getIdentityProviderWithMetadataById(providerId);
            if (identityProvider.getXmlMetadata() == null) {
                throw new NotFoundException(String.format(METADATA_NOT_FOUND_ERROR_MESSAGE, providerId));
            }

            Document xmlDocument;
            try {
                xmlDocument = identityProviderConverterCloudV20.getXMLDocument(identityProvider.getXmlMetadata());
            } catch (BadRequestException ex) {
                String errMsg = String.format("Failed to convert stored metadata for Identity provider %s to XML.", providerId);
                logger.error(errMsg);
                throw new UnrecoverableIdmException(errMsg);
            }
            return Response.ok(xmlDocument);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private boolean assertDomainsBelongToRcn(Domain... domains) {
        Set<String> rcn = new HashSet<>();
        for(Domain domain : domains) {
            if (domain == null || domain.getRackspaceCustomerNumber() == null ) {
                return false;
            } else {
                rcn.add(domain.getRackspaceCustomerNumber());
            }
        }
        return rcn.size() == 1;
    }

    @Override
    public ResponseBuilder deleteIdentityProvider(HttpHeaders httpHeaders, String authToken, String providerId) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()));

            verifyUserIsNotInDefaultDomain(caller);
            verifyUserIsNotInRaxRestrictedGroup(caller);

            com.rackspace.idm.domain.entity.IdentityProvider existingProvider = federatedIdentityService.checkAndGetIdentityProvider(providerId);

            if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList((
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName()),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()))) {

                verifyDomainUserHasAccessToIdentityProviderMetadata(existingProvider, caller);
            }

            federatedIdentityService.deleteIdentityProviderById(providerId);
            if (existingProvider != null) {
                atomHopperClient.asyncPostIdpEvent(existingProvider, EventType.DELETE);
            }
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
            com.rackspace.idm.domain.entity.IdentityProvider provider = federatedIdentityService.checkAndGetIdentityProviderWithMetadataById(identityProviderId);

            if (ArrayUtils.isNotEmpty(provider.getXmlMetadata())) {
                throw new ForbiddenException(FEDERATION_IDP_CANNOT_MANUALLY_UPDATE_CERTS_ON_METADATA_IDP_MESSAGE, ErrorCodes.ERROR_CODE_IDP_CANNOT_MANUALLY_UPDATE_CERTS_ON_METADATA_IDP);
            }

            validator20.validatePublicCertificateForIdentityProvider(publicCertificate, provider);

            //set the cert on the provider and save
            byte[] certBytes = Base64.decodeBase64(publicCertificate.getPemEncoded());
            if(provider.getUserCertificates() == null) {
                provider.setUserCertificates(new ArrayList<byte[]>());
            }
            provider.getUserCertificates().add(certBytes);
            federatedIdentityService.updateIdentityProvider(provider);
            atomHopperClient.asyncPostIdpEvent(provider, EventType.UPDATE);

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
            com.rackspace.idm.domain.entity.IdentityProvider provider = federatedIdentityService.checkAndGetIdentityProviderWithMetadataById(identityProviderId);

            if (ArrayUtils.isNotEmpty(provider.getXmlMetadata())) {
                throw new ForbiddenException(FEDERATION_IDP_CANNOT_MANUALLY_UPDATE_CERTS_ON_METADATA_IDP_MESSAGE, ErrorCodes.ERROR_CODE_IDP_CANNOT_MANUALLY_UPDATE_CERTS_ON_METADATA_IDP);
            }

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
            atomHopperClient.asyncPostIdpEvent(provider, EventType.UPDATE);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateIdentityProviderPolicy(HttpHeaders httpHeaders, String authToken, String identityProviderId, String policy) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()));

            verifyUserIsNotInDefaultDomain(caller);
            verifyUserIsNotInRaxRestrictedGroup(caller);

            com.rackspace.idm.domain.entity.IdentityProvider existingProvider = federatedIdentityService.checkAndGetIdentityProvider(identityProviderId);

            if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList((
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName()),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()))) {

                verifyDomainUserHasAccessToIdentityProviderMetadata(existingProvider, caller);
            }

            IdpPolicyFormatEnum type = IdpPolicyFormatEnum.fromValue(httpHeaders.getMediaType().toString());

            if (type == null) {
                throw new UnsupportedMediaTypeException("Unsupported MediaType for policy");
            }

            validator20.validateIdpPolicy(policy, type);

            existingProvider.setPolicy(policy.getBytes(StandardCharsets.UTF_8));
            existingProvider.setPolicyFormat(type.name());

            federatedIdentityService.updateIdentityProvider(existingProvider);
            atomHopperClient.asyncPostIdpEvent(existingProvider, EventType.UPDATE);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getIdentityProviderPolicy(HttpHeaders httpHeaders, String authToken, String identityProviderId) {
        try {
            //verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            //verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                    IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName(),
                    IdentityRole.IDENTITY_PROVIDER_READ_ONLY.getRoleName(),
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()));

            verifyUserIsNotInDefaultDomain(caller);
            verifyUserIsNotInRaxRestrictedGroup(caller);

            com.rackspace.idm.domain.entity.IdentityProvider existingProvider = federatedIdentityService.checkAndGetIdentityProvider(identityProviderId);

            if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList((
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName()),
                    IdentityUserTypeEnum.USER_MANAGER.getRoleName(),
                    IdentityRole.RCN_ADMIN.getRoleName()))) {

                verifyDomainUserHasAccessToIdentityProviderMetadata(existingProvider, caller);
            }

            // Return the default IDP policy if identity provider does not have a policy
            String body;
            MediaType contentType;
            if (existingProvider.getPolicy() == null) {
                IdentityProperty defaultPolicyProperty = federatedIdentityService.checkAndGetDefaultMappingPolicyProperty();
                body = new String(defaultPolicyProperty.getValue());
                contentType = IdpPolicyFormatEnum.toMediaType(IdpPolicyFormatEnum.valueOf(defaultPolicyProperty.getValueType().toUpperCase()));
            } else {
                body = new String(existingProvider.getPolicy());
                // By default the policyFormat is set to JSON if not set on the IDP.
                contentType = IdpPolicyFormatEnum.toMediaType(IdpPolicyFormatEnum.valueOf(existingProvider.getPolicyFormat()));
            }

            Set<String> idpPolicyFormats = IdpPolicyFormatEnum.fromMediaTypes(httpHeaders.getAcceptableMediaTypes());
            if (!httpHeaders.getAcceptableMediaTypes().contains(contentType)) {
                String errMsg = String.format(FEDERATION_IDP_POLICY_TYPE_NOT_FOUND_ERROR_MESSAGE,
                        idpPolicyFormats, identityProviderId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            return Response.ok(body).type(contentType);
        } catch (Exception ex) {
            if(httpHeaders.getAcceptableMediaTypes().contains(GlobalConstants.TEXT_YAML_TYPE)) {
                // Force exceptions to Content-Type 'application/json' for Accept 'text/yaml'
                return exceptionHandler.exceptionResponse(ex).type(MediaType.APPLICATION_JSON_TYPE);
            }

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
            if (CollectionUtils.isNotEmpty(ruleService.findEndpointAssignmentRulesForEndpointTemplateId(endpointTemplateId))) {
                logger.info("Endpoint template with id {} deletion prevented due to being part of an assignment rule.", endpointTemplateId);
                throw new ForbiddenException(ERROR_CANNOT_DELETE_ENDPOINT_TEMPLATE_IN_ASSIGNMENT_RULE_MESSAGE);
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

            if (IdentityUserTypeEnum.isIdentityUserTypeRoleName(role.getName())) {
                throw new ForbiddenException(IDENTITY_USER_TYPE_ROLE_ERROR_MESSAGE);
            }
            if (tenantService.getCountOfTenantRolesByRoleIdForProvisionedUsers(roleId) > 0 || tenantService.getCountOfTenantRolesByRoleIdForFederatedUsers(roleId) > 0) {
                throw new ForbiddenException(ERROR_DELETE_ROLE_WITH_USERS_ASSIGNED);
            }
            if (userGroupService.countGroupsWithRoleAssignment(roleId) > 0) {
                throw new ForbiddenException(ERROR_DELETE_ROLE_WITH_GROUPS_ASSIGNED);
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
            EndUser user = identityUserService.checkAndGetUserById(userId);

            if (!(user instanceof User) && !(user instanceof FederatedUser)) {
                String errMsg = String.format("User %s not found", userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

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
            identityUserService.deleteUser(user);

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
                        jaxbObjectFactories.getOpenStackIdentityV2Factory().createIdentityFault(fault))
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
            if (userTypeEnum != null && userTypeEnum != IdentityUserTypeEnum.USER_MANAGER) {
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
                    jaxbObjectFactories.getOpenStackIdentityV2Factory().createEndpoint(endpointConverterCloudV20.toEndpoint(baseUrl)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()
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
                    extensionMap.put(e.getAlias().trim().toUpperCase(), jaxbObjectFactories.getOpenStackCommonV1Factory().createExtension(e));
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

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory()
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
            SecretQA secrets = jaxbObjectFactories.getRackspaceIdentityExtKsqaV1Factory().createSecretQA();

            secrets.setAnswer(user.getSecretAnswer());
            secrets.setQuestion(user.getSecretQuestion());
            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtKsqaV1Factory().createSecretQA(secrets).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Application client = applicationService.checkAndGetApplication(serviceId);
            return Response.ok(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory().createService(serviceConverterCloudV20.toService(client)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Tenant tenant = tenantService.checkAndGetTenant(tenantsId);
            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createTenant(tenantConverterCloudV20.toTenant(tenant)).getValue());
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

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory()
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
                    return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createUser(this.userConverterCloudV20.toUser(caller)).getValue());
                } else {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }
            authorizationService.verifyUserManagedLevelAccess(scopeAccessByAccessToken);
            EndUser user = this.identityUserService.getEndUserById(userId);
            if (user == null) {
                String errMsg = String.format(USER_NOT_FOUND_ERROR_MESSAGE, userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }
            setEmptyUserValues(user);
            if (authorizationService.authorizeUserManageRole(scopeAccessByAccessToken) ||
                    authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
                authorizationService.verifyDomain(caller, user);
            }

            org.openstack.docs.identity.api.v2.User userResponse = this.userConverterCloudV20.toUser(user);
            if (user instanceof User && identityConfig.getReloadableConfig().isIncludePasswordExpirationDateForGetUserResponsesEnabled()) {
                DateTime pwdExpiration = userService.getPasswordExpiration((User) user);
                if (pwdExpiration != null) {
                    userResponse.setPasswordExpiration(DatatypeFactory.newInstance().newXMLGregorianCalendar(pwdExpiration.toGregorianCalendar()));
                }
            }

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createUser(userResponse).getValue());
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

    /**
     * This feature was developed, but never enabled as it hasn't been fully tested by QE.
     *
     * It is no more efficient than the existing v1.1 version of the same
     * name. The purpose was simply to allow users to use a 'v2.0' branded service rather than a 'v1.1'. Don't enable
     * this service unless for a very good reason...
     *
     * @param httpHeaders
     * @param authToken
     * @param tenantId
     * @return
     * @deprecated
     */
    @Deprecated
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

                return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createUser(jaxbUser).getValue());
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

            /*
                By time we get here we know the specified user exists and the caller is a User. Only check left is to
                make sure the caller has access to the specified user.
             */
            List<User> userList = new ArrayList<>();
            userList.add(user);
            Iterable<? extends EndUser> result = filterOutUsersInaccessibleByCaller(userList, requester);
            if (!result.iterator().hasNext()) {
                // If the user was filtered, the caller doesn't have access to that user
                throw new ForbiddenException(DefaultAuthorizationService.NOT_AUTHORIZED_MSG);
            }

            org.openstack.docs.identity.api.v2.User userResponse = userConverterCloudV20.toUser(user);

            if (identityConfig.getReloadableConfig().isIncludePasswordExpirationDateForGetUserResponsesEnabled()) {
                // Manually set the pwd expiration on the user.
                // Calculating the pwd expiration of a user requires the domain (for the pwd policy) and thus
                // an extra query for each unique domain. Thus, the pwd expiration should not be returned for user lists!
                DateTime pwdExpiration = userService.getPasswordExpiration(user);
                if (pwdExpiration != null) {
                    userResponse.setPasswordExpiration(DatatypeFactory.newInstance().newXMLGregorianCalendar(pwdExpiration.toGregorianCalendar()));
                }
            }

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createUser(userResponse).getValue());

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

            Iterable<? extends EndUser> filteredUsers;
            filteredUsers = filterOutUsersInaccessibleByCaller(users, caller);

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(filteredUsers)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }


    /**
     * Returns an Iterable that contains the subset of the provides users for which the specified caller can operate on.
     *
     * Note - this currently would return all user-admins in a domain for a user-admin. This is pre-existing and expected
     * as a domain should only ever have one and only one user-admin. In the case that a domain does have multiple user-
     * admins, all should be returned.
     *
     * @param users
     * @param caller
     * @return
     */
    private Iterable<? extends EndUser> filterOutUsersInaccessibleByCaller(Iterable<? extends EndUser> users, EndUser caller) {
        Iterable<? extends EndUser> result;

        IdentityUserTypeEnum callerType = authorizationService.getIdentityTypeRoleAsEnum(caller);
        if (callerType.isDomainBasedAccessLevel()) {
            List<EndUser> domainUsers = new ArrayList<EndUser>();

            // Filter results based on domain
            for (EndUser user : users) {
                if (authorizationService.hasSameDomain(caller, user)) {
                    if (caller.getId().equals(user.getId())) {
                        // Can always see self regardless of role
                        domainUsers.add(user);
                    } else if (callerType == IdentityUserTypeEnum.USER_MANAGER) {
                        // Can see all domain users except user-admins and other user-managers
                        if (!authorizationService.hasUserAdminRole(user)
                                && !authorizationService.hasUserManageRole(user)) {
                            domainUsers.add(user);
                        }
                    } else if (callerType == IdentityUserTypeEnum.USER_ADMIN) {
                        // Can see all domain users - including other user-admins for now
                        domainUsers.add(user);
                    }
                }
            }
            result = domainUsers;
        } else {
            result = users;
        }

        return result;
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
            JAXBElement<? extends CredentialType> creds = jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(userCreds);

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

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory()
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

            CredentialListType creds = jaxbObjectFactories.getOpenStackIdentityV2Factory().createCredentialListType();

            if (!StringUtils.isBlank(user.getApiKey())) {
                ApiKeyCredentials userCreds = new ApiKeyCredentials();
                userCreds.setApiKey(user.getApiKey());
                userCreds.setUsername(user.getUsername());
                creds.getCredential().add(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(userCreds));
            }

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createCredentials(creds).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
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
            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory()
                    .createEndpoints(this.endpointConverterCloudV20.toEndpointListFromBaseUrls(baseUrls)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId, boolean applyRcnRoles) {

        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            boolean sameToken = StringUtils.equals(authToken, tokenId);

            // Skip authorization checks if the token being used is the same token to list endpoints for token.
            if (!sameToken) {
                authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.GET_TOKEN_ENDPOINTS_GLOBAL.getRoleName());
            }

            ScopeAccess sa = checkAndGetToken(tokenId);
            boolean isImpersonationToken = false;
            if (sa instanceof ImpersonatedScopeAccess) {
                isImpersonationToken = true;
                ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) sa;
                String impersonatedTokenId = impersonatedScopeAccess.getImpersonatingToken();
                sa = scopeAccessService.getScopeAccessByAccessToken(impersonatedTokenId);
            }

            //do not allow for any type of scoped token to have endpoints listed for it
            if(sa == null || StringUtils.isNotBlank(sa.getScope())) {
                throw new NotFoundException("Valid token not found");
            }

            BaseUser baseUser = userService.getUserByScopeAccess(sa, false);
            ServiceCatalogInfo scInfo;
            if (applyRcnRoles) {
                scInfo = scopeAccessService.getServiceCatalogInfoApplyRcnRoles(baseUser);
            } else {
                scInfo = scopeAccessService.getServiceCatalogInfo(baseUser);
            }

            EndpointList list;
            if (authorizationService.restrictTokenEndpoints(scInfo)
                    && (!isImpersonationToken
                        || !identityConfig.getReloadableConfig().shouldDisplayServiceCatalogForSuspendedUserImpersonationTokens())) {
                //terminator is in effect. All tenants disabled so blank endpoint list
                list = jaxbObjectFactories.getOpenStackIdentityV2Factory().createEndpointList();
            } else {
                list = endpointConverterCloudV20.toEndpointList(scInfo.getUserEndpoints());
            }

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createEndpoints(list).getValue());
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
                    jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()
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
                    .entity(jaxbObjectFactories.getOpenStackIdentityV2Factory()
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
                    jaxbObjectFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                    String userId, boolean applyRcnRoles) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            boolean self = caller.getId().equals(user.getId());

            if (!self) {
                precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            }

            List<TenantRole> roles;

            if (applyRcnRoles) {
                roles = this.tenantService.getEffectiveTenantRolesForUserOnTenantApplyRcnRoles(user, tenant);
            } else {
                roles = this.tenantService.getEffectiveTenantRolesForUserOnTenant(user, tenant);
            }

            return Response.ok(
                    jaxbObjectFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }

    }

    @Override
    public ResponseBuilder listEffectiveRolesForUser(HttpHeaders httpHeaders, String authToken, String userId, ListEffectiveRolesForUserParams params) {
        try {
            // Authorization Restrictions
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN);

            User targetUser = userService.checkAndGetUserById(userId);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

            boolean self = caller.getId().equalsIgnoreCase(targetUser.getId());

            if (!self) {
                precedenceValidator.verifyCallerPrecedenceOverUser(caller, targetUser);
            }

            SourcedRoleAssignments assignments = tenantService.getSourcedRoleAssignmentsForUser(targetUser);
            return Response.ok(roleAssignmentConverter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(assignments));
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    // KSADM Extension Role Methods

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String name, Integer marker, Integer limit) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Iterable<Application> clients;
            String linkHeader = null;

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
                PaginatorContext<Application> context = this.applicationService.getOpenStackServices(marker, limit);
                linkHeader = applicationPaginator.createLinkHeader(uriInfo, context);
                clients = context.getValueList();
            }

            ResponseBuilder responseBuilder = Response.ok(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory().createServices(serviceConverterCloudV20.toServiceList(clients)).getValue());
            if (linkHeader != null) {
                responseBuilder = responseBuilder.header(org.springframework.http.HttpHeaders.LINK, linkHeader);
            }

            return responseBuilder;

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, boolean applyRcnRoles, Integer marker, Integer limit) {
        try {
            ScopeAccess access = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(access);

            //safe cast as only enduser would pass verify check
            EndUser user = (EndUser) userService.getUserByScopeAccess(access);

            List<Tenant> tenants;
            if (applyRcnRoles) {
                tenants = tenantService.getTenantsForUserByTenantRolesApplyRcnRoles(user);
            } else {
                tenants = this.tenantService.getTenantsForUserByTenantRoles(user);
            }

            return Response.ok(
                    jaxbObjectFactories.getOpenStackIdentityV2Factory().createTenants(tenantConverterCloudV20.toTenantList(tenants)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId, boolean applyRcnRoles) {
        try {
            // Verify token is valid and user is enabled
            ScopeAccess callersScopeAccess = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            EndUser user =  identityUserService.checkAndGetEndUserById(userId);

            // TODO: Modify this so a federated user can consume this service as the caller (just call retrieve effective caller from request context)
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

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

            List<TenantRole> roles;
            if (applyRcnRoles) {
                roles = tenantService.getEffectiveGlobalRolesForUserApplyRcnRoles(user);
            } else {
                roles = tenantService.getEffectiveGlobalRolesForUser(user);
            }
            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());
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
    public ResponseBuilder listUserGlobalRolesByServiceId(HttpHeaders httpHeaders, String authToken,
                                                          String userId, String serviceId, boolean applyRcnRoles) {
        try {
            // Verify token is valid and user is enabled
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN);

            EndUser user =  identityUserService.checkAndGetEndUserById(userId);

            List<TenantRole> roles;
            if (applyRcnRoles) {
                roles= tenantService.getEffectiveGlobalRolesForUserApplyRcnRoles(user, serviceId);
            } else {
                roles= tenantService.getEffectiveGlobalRolesForUser(user, serviceId);
            }

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());

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

            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups(cloudGroups).getValue());
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
            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup(cloudGroup).getValue());
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
            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().createAccess(auth).getValue());
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
            validator20.validateDomainForCreation(domain);
            Domain savedDomain = this.domainConverterCloudV20.fromDomain(domain);
            this.domainService.addDomain(savedDomain);
            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            String domainId = savedDomain.getDomainId();
            URI build = requestUriBuilder.path(domainId).build();
            com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory();
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
        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory();
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain value = this.domainConverterCloudV20.toDomain(domain);

        if (requesterIdentityRole.isDomainBasedAccessLevel()) {
            value.setDescription(null);
            value.setName(null);
        }

        return Response.ok(objectFactory.createDomain(value).getValue());
    }

    @Override
    public ResponseBuilder updateDomain(String authToken, String domainId, com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            BaseUser caller = userService.getUserByScopeAccess(scopeAccess);
            authorizationService.verifyUserManagedLevelAccess(scopeAccess);

            IdentityUserTypeEnum callersUserType = authorizationService.getIdentityTypeRoleAsEnum(caller);
            if (IdentityUserTypeEnum.IDENTITY_ADMIN == callersUserType) {
                List<User> superAdmins = domainService.getDomainSuperAdmins(domainId);
                if (containsServiceAdmin(superAdmins)) {
                    throw new ForbiddenException("Cannot modify a domain containing a service admin");
                }
                if (containsIdentityAdmin(superAdmins) && caller.getDomainId() != null && !caller.getDomainId().equals(domainId)) {
                    throw new ForbiddenException("Cannot modify a domain containing an identity admin when you are not in the domain");
                }
            } else if (IdentityUserTypeEnum.USER_ADMIN == callersUserType || IdentityUserTypeEnum.USER_MANAGER == callersUserType) {
                if (!caller.getDomainId().equals(domainId)) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
                // If user-admin or user-manage level access, only allow to update the sessionInactivityTimeout and ignore others attributes.
                com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain updateDomain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
                if (domain.getSessionInactivityTimeout() != null) {
                    updateDomain.setSessionInactivityTimeout(domain.getSessionInactivityTimeout());
                }
                domain = updateDomain;
            }

            Domain domainDO = domainService.checkAndGetDomain(domainId);
            validator20.validateDomainForUpdate(domain, domainId);

            Boolean shouldExpireAllTokens = false;

            if (StringUtils.isNotBlank(domain.getDescription())) {
                domainDO.setDescription(domain.getDescription());
            }
            if (StringUtils.isNotBlank(domain.getName())) {
                domainDO.setName(domain.getName());
            }
            if (domain.getSessionInactivityTimeout() != null) {
                domainDO.setSessionInactivityTimeout(domain.getSessionInactivityTimeout().toString());
            }
            if (identityConfig.getReloadableConfig().isUpdateDomainRcnOnUpdateDomainAllowed()
                    && StringUtils.isNotBlank(domain.getRackspaceCustomerNumber())) {
                domainDO.setRackspaceCustomerNumber(domain.getRackspaceCustomerNumber());
            }
            if (domain.isEnabled() != null) {
                shouldExpireAllTokens = domainDO.getEnabled() && !domain.isEnabled();
                domainDO.setEnabled(domain.isEnabled());
            }

            this.domainService.updateDomain(domainDO);

            if (shouldExpireAllTokens) {
                domainService.expireAllTokenInDomain(domainDO.getDomainId());
            }

            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory().createDomain(domainConverterCloudV20.toDomain(domainDO)).getValue());
        } catch (Exception ex) {
            if (identityConfig.getReloadableConfig().forceStandardV2ExceptionsEndUserServices()) {
                return exceptionHandler.exceptionResponse(ex);
            } else {
                throw ex;
            }
        }
    }

    @Override
    public ResponseBuilder updateDomainPasswordPolicy(HttpHeaders httpHeaders, String authToken, String domainId, String policy) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            // Verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN);

            Domain domain = domainService.getDomain(domainId); // Don't do checkandget cause want to return 403 if domain doesn't exist
            IdentityUserTypeEnum callerType = requestContextHolder.getRequestContext().getEffectiveCallersUserType();
            if (callerType == null || domain == null) {
                throw new ForbiddenException("Forbidden.");
            } else if (callerType.isDomainBasedAccessLevel()) {
                BaseUser user = requestContextHolder.getRequestContext().getEffectiveCaller();
                if (!domain.getDomainId().equalsIgnoreCase(user.getDomainId())) {
                    throw new ForbiddenException("Forbidden.");
                }
            }

            PasswordPolicy pwdPolicy = null;
            try {
                pwdPolicy = PasswordPolicy.fromJson(policy);
                if (pwdPolicy == null) {
                    throw new BadRequestException("The supplied password policy is invalid.");
                }
                if (pwdPolicy.calculateEffectivePasswordHistoryRestriction() > identityConfig.getReloadableConfig().getPasswordHistoryMax()) {
                    throw new BadRequestException(String.format("Invalid Password history restriction. Must be between 0-%s. 0 means to ignore history.", identityConfig.getReloadableConfig().getPasswordHistoryMax()));
                }
            } catch (InvalidPasswordPolicyException e) {
                logger.debug(String.format("The supplied password policy '%s' for domainId '%s' is invalid", policy, domainId), e);
                throw new BadRequestException("The supplied password policy is invalid. Please check your syntax and try again.");
            }

            domain.setPasswordPolicy(pwdPolicy);
            domainService.updateDomain(domain);

            String body = "{}";
            if (pwdPolicy != null) {
                body = pwdPolicy.toJson();
            }

            return Response.ok(body);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteDomainPasswordPolicy(HttpHeaders httpHeaders, String authToken, String domainId) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            // Verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN);

            Domain domain = domainService.getDomain(domainId);
            IdentityUserTypeEnum callerType = requestContextHolder.getRequestContext().getEffectiveCallersUserType();
            if (callerType == null || domain == null) {
                throw new ForbiddenException("Forbidden.");
            } else if (callerType.isDomainBasedAccessLevel()) {
                BaseUser user = requestContextHolder.getRequestContext().getEffectiveCaller();
                if (!domain.getDomainId().equalsIgnoreCase(user.getDomainId())) {
                    throw new ForbiddenException("Forbidden.");
                }
            }

            domainService.deleteDomainPasswordPolicy(domainId);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getDomainPasswordPolicy(HttpHeaders httpHeaders, String authToken, String domainId) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            // Verify user has appropriate role
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN);

            Domain domain = domainService.getDomain(domainId);
            IdentityUserTypeEnum callerType = requestContextHolder.getRequestContext().getEffectiveCallersUserType();
            if (callerType == null || domain == null) {
                throw new ForbiddenException("Forbidden.");
            } else if (callerType.isDomainBasedAccessLevel()) {
                BaseUser user = requestContextHolder.getRequestContext().getEffectiveCaller();
                if (!domain.getDomainId().equalsIgnoreCase(user.getDomainId())) {
                    throw new ForbiddenException("Forbidden.");
                }
            }

            PasswordPolicy policy = domain.getPasswordPolicy();

            if (policy != null) {
                return Response.ok(policy.toJson());
            } else {
                return exceptionHandler.notFoundExceptionResponse("A password policy is not set on this domain");
            }
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder changeUserPassword(HttpHeaders httpHeaders, ChangePasswordCredentials changePasswordCredentials) {
        try {
            if (StringUtils.isEmpty(changePasswordCredentials.getPassword()) || StringUtils.isEmpty(changePasswordCredentials.getNewPassword())
                    || StringUtils.isEmpty(changePasswordCredentials.getUsername())) {
                throw new BadRequestException("Username, password, and new password are required");
            }
            if (changePasswordCredentials.getPassword().equals(changePasswordCredentials.getNewPassword())) {
                throw new BadRequestException("Must supply a new password that is different from the current password");
            }

            PasswordCredentialsRequiredUsername cred = jaxbObjectFactories.getOpenStackIdentityV2Factory().createPasswordCredentialsRequiredUsername();
            cred.setPassword(changePasswordCredentials.getPassword());
            cred.setUsername(changePasswordCredentials.getUsername());

            AuthenticationRequest authRequestAdapter = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticationRequest();
            authRequestAdapter.setCredential(jaxbObjectFactories.getOpenStackIdentityV2Factory().createPasswordCredentials(cred));

            User userToUpdate = null;
            try {
                AuthResponseTuple auth = authWithPasswordCredentials.authenticateForAuthResponse(authRequestAdapter);
                userToUpdate = (User) auth.getUser();
            } catch (UserPasswordExpiredException e) {
                // If UserPasswordExpiredException is thrown, the user's password was expired.
                /*
                 TODO user wasn't validated to see if user was disabled or domain was disabled. Should
                  we allow a disabled user to update their expired password?
                  */
                userToUpdate = e.getUser();
            }

            // Retrieve a fresh user so we don't inadvertently update unexpected things
            User user = userService.checkAndGetUserById(userToUpdate.getId());
            validator.validatePasswordForCreateOrUpdate(changePasswordCredentials.getNewPassword());

            user.setUserPassword(changePasswordCredentials.getNewPassword());
            user.setPassword(changePasswordCredentials.getNewPassword());
            this.userService.updateUser(user);

            return Response.noContent().header(GlobalConstants.X_USER_NAME, user.getUsername());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
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

    @Override
    public ResponseBuilder deleteDomain(String authToken, String domainId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            String defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId();
            if (defaultDomainId.equals(domainId)) {
                throw new BadRequestException(GlobalConstants.ERROR_MSG_DELETE_DEFAULT_DOMAIN);
            }

            Domain domain = domainService.checkAndGetDomain(domainId);
            if (Boolean.TRUE.equals(domain.getEnabled())) {
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

            // Delete domain's user groups.
            for (UserGroup userGroup : userGroupService.getGroupsForDomain(domainId)) {
                userGroupService.deleteGroup(userGroup);
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
            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createTenants(tenantConverterCloudV20.toTenantList(tenants)).getValue());
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

        return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
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
        return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createEndpoints(list).getValue());
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
    public ResponseBuilder getAccessibleDomains(UriInfo uriInfo, String authToken, Integer marker, Integer limit, String rcn) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            if (this.authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken) ||
                    this.authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken)) {

                PaginatorContext<Domain> domainContext;
                if (StringUtils.isNotBlank(rcn)) {
                    domainContext = this.domainService.getDomainsByRCN(rcn, marker, limit);
                } else {
                    domainContext = this.domainService.getDomains(marker, limit);
                }
                String linkHeader = this.domainPaginator.createLinkHeader(uriInfo, domainContext);

                Domains domains = new Domains();
                domains.getDomain().addAll(domainContext.getValueList());
                com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains domainsObj = domainConverterCloudV20.toDomains(domains);

                return Response.status(200).header("Link", linkHeader).entity(raxAuthObjectFactory.createDomains(domainsObj).getValue());

            } else {
                User user = (User) userService.getUserByScopeAccess(scopeAccessByAccessToken);
                if (StringUtils.isNotBlank(rcn)) {
                    return getAccessibleDomainsForUserInRCN(authToken, user.getId(), rcn);
                } else {
                    return getAccessibleDomainsForUser(authToken, user.getId());
                }
            }
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getAccessibleDomainsForUser(String authToken, String userId) {
        return getAccessibleDomainsForUserInRCN(authToken, userId, null);
    }

    public ResponseBuilder getAccessibleDomainsForUserInRCN(String authToken, String userId, String rcn) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);

            CloudUserAccessibility cloudUserAccessibility = getUserAccessibility(scopeAccessByAccessToken);

            User user = userService.checkAndGetUserById(userId);
            Domains domains = cloudUserAccessibility.getAccessibleDomainsByUser(user);

            domains = cloudUserAccessibility.addUserDomainToDomains(user, domains);
            domains = cloudUserAccessibility.removeDuplicateDomains(domains);

            if (StringUtils.isNotBlank(rcn)) {
                domains = cloudUserAccessibility.removeNonRcnDomains(domains, rcn);
            }

            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains domainsObj = domainConverterCloudV20.toDomains(domains);

            return Response.ok().entity(raxAuthObjectFactory.createDomains(domainsObj).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    public CloudUserAccessibility getUserAccessibility(ScopeAccess scopeAccess) {
        if (this.authorizationService.authorizeCloudUser(scopeAccess)) {
            return new CloudDefaultUserAccessibility(tenantService, domainService,
                    authorizationService, userService, config, jaxbObjectFactories.getOpenStackIdentityV2Factory(), scopeAccess);
        }
        if (this.authorizationService.authorizeCloudUserAdmin(scopeAccess)) {
            return new CloudUserAdminAccessibility(tenantService, domainService,
                    authorizationService, userService, config, jaxbObjectFactories.getOpenStackIdentityV2Factory(), scopeAccess);
        }
        if (this.authorizationService.authorizeCloudIdentityAdmin(scopeAccess)) {
            return new CloudIdentityAdminAccessibility(tenantService, domainService,
                    authorizationService, userService, config, jaxbObjectFactories.getOpenStackIdentityV2Factory(), scopeAccess);
        }
        if (this.authorizationService.authorizeCloudServiceAdmin(scopeAccess)) {
            return new CloudServiceAdminAccessibility(tenantService, domainService,
                    authorizationService, userService, config, jaxbObjectFactories.getOpenStackIdentityV2Factory(), scopeAccess);
        }
        return new CloudUserAccessibility(tenantService, domainService,
                authorizationService, userService, config, jaxbObjectFactories.getOpenStackIdentityV2Factory(), scopeAccess);
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

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createEndpoints(list).getValue());
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

            return Response.status(200).header("Link", linkHeader).entity(jaxbObjectFactories.getOpenStackIdentityV2Factory()
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
                .entity(jaxbObjectFactories.getOpenStackIdentityV2Factory()
                        .createUsers(userConverterCloudV20.toUserList(admins)).getValue());
    }

    @Override
    public ResponseBuilder addTenantType(UriInfo uriInfo, String authToken, TenantType tenantType) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN);
            tenantTypeService.createTenantType(tenantTypeConverter.fromTenantType(tenantType));
            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            URI build = requestUriBuilder.path(tenantType.getName()).build();
            return Response.created(build).entity(tenantType);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getTenantType(UriInfo uriInfo, String authToken, String tenantTypeName) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN);
            TenantType tenantType = tenantTypeConverter.toTenantType(tenantTypeService.checkAndGetTenantType(tenantTypeName));
            return Response.ok().entity(tenantType);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listTenantTypes(UriInfo uriInfo, String authToken, Integer marker, Integer limit) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN);
            PaginatorContext<com.rackspace.idm.domain.entity.TenantType> tenantTypeContext = tenantTypeService.listTenantTypes(marker, limit);
            TenantTypes tenantTypes = tenantTypeConverter.toTenantType(tenantTypeContext.getValueList());
            String linkHeader = tenantTypePaginator.createLinkHeader(uriInfo, tenantTypeContext);

            return Response.ok().header("Link", linkHeader).entity(tenantTypes);
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteTenantType(String authToken, String tenantTypeId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN);
            com.rackspace.idm.domain.entity.TenantType tenantType = tenantTypeService.checkAndGetTenantType(tenantTypeId);
            tenantTypeService.deleteTenantType(tenantType);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
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
        boolean isValidImpersonatee = false;
        List<TenantRole> tenantRolesForUser = tenantService.getGlobalRolesForUser(user);
        for (TenantRole role : tenantRolesForUser) {
            String name = role.getName();
            if (name.equals(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) || name.equals(IdentityUserTypeEnum.USER_ADMIN.getRoleName())) {
                isValidImpersonatee = true;
            }
        }
        for (TenantRole role : tenantRolesForUser) {
            String name = role.getName();
            if (name.equals(IdentityRole.IDENTITY_INTERNAL.getRoleName())) {
                isValidImpersonatee = false;
            }
        }
        return isValidImpersonatee;
    }

    @Override
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();
            if (!caller.getId().equals(userId)) {
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
            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroups(cloudGroups).getValue());

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
            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup(cloudGroup).getValue());
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
                    .entity(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup(groupKs).getValue());
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

            return Response.ok().entity(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory().createGroup(groupKs).getValue());
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

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(users.getValueList())).getValue());
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

            // If default user & NOT user-manage
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken)
                    && !authorizationService.authorizeUserManageRole(scopeAccessByAccessToken)) {
                List<EndUser> users = new ArrayList<EndUser>();
                // At this point, we know that the user is not a racker and can cast to an EndUser
                users.add((EndUser) caller);
                return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory()
                        .createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
            }

            // This proves caller is an EndUser (e.g. not a Racker)
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

            Iterable<? extends EndUser> filteredUsers;

            // If role is identity:user-manage then we need to filter out the identity:user-admin and other user-managers
            filteredUsers = filterOutUsersInaccessibleByCaller(paginatorContext.getValueList(), (EndUser) caller);

            String linkHeader = endUserPaginator.createLinkHeader(uriInfo, paginatorContext);

            return Response.status(200)
                    .header("Link", linkHeader)
                    .entity(jaxbObjectFactories.getOpenStackIdentityV2Factory()
                            .createUsers(this.userConverterCloudV20.toUserList(filteredUsers)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String tenantId, ListUsersForTenantParams params) {
        try {
            ScopeAccess scopeAccess = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN);
            requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            /*
            This check will verify the caller has a role on the tenant (or user is identity/service admin). Ultimately
            this means that a user-admin for a domain can list the users for any tenant to which they have access -
            regardless of whether the tenant is in the user-admin's domain or overall RCN. Issue https://jira.rax.io/browse/CID-1285
            was created for this.
             */
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            // TODO: Could optimize this more. The previous call already retrieves the tenant for user-admins.
            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            if(StringUtils.isNotBlank(params.roleId) && StringUtils.isNotBlank(params.contactId)) {
                throw new BadRequestException(LIST_USERS_FOR_TENANT_PARAM_ERROR_MESSAGE);
            }

            // ContactId search ignores any values supplied for marker and limit
            PaginatorContext<User> pageContext;
            if (StringUtils.isNotBlank(params.contactId)) {
                // Blank out pagination params to default 0-1000
                List<User> userResultSet = tenantService.getEnabledUsersWithContactIdForTenant(tenantId, params.contactId);
                PaginationParams overriddenPagination = new PaginationParams();
                pageContext = new PaginatorContext<>();
                pageContext.update(userResultSet, overriddenPagination.getEffectiveMarker(), overriddenPagination.getEffectiveLimit());
            } else {
                // Verify specified role exists
                ImmutableClientRole limitByRole = null;
                if (StringUtils.isNotBlank(params.getRoleId())) {
                    limitByRole = applicationService.getCachedClientRoleById(params.getRoleId());
                    if (limitByRole == null) {
                        String errMsg = String.format(ROLE_ID_NOT_FOUND_ERROR_MESSAGE, params.getRoleId());
                        logger.warn(errMsg);
                        throw new NotFoundException(errMsg);
                    }
                }
                pageContext = tenantService.getEnabledUsersForTenantWithRole(tenant, params.getRoleId(), params.getPaginationRequest());
            }

            String linkHeader = userPaginator.createLinkHeader(uriInfo, pageContext);
            return Response.status(200)
                    .header("Link", linkHeader)
                    .entity(jaxbObjectFactories.getOpenStackIdentityV2Factory()
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

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory()
                    .createUser(this.userConverterCloudV20.toUser(userDO)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    public void setGroupService(GroupService cloudGroupService) {
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

            validator20.validateTenantType(tenant);

            Tenant tenantDO = tenantService.checkAndGetTenant(tenantId);

            /*
            Update those fields that are allowed to be updated via updateTenant. Note - the tenant 'name' is NOT allowed
            to be updated as it must always match the tenant id.
             */
            tenantDO.setDescription(tenant.getDescription());
            tenantDO.setDisplayName(tenant.getDisplayName());
            tenantDO.setEnabled(tenant.isEnabled());

            if (tenant.getTypes() != null) {
                tenantDO.getTypes().clear();
                tenantDO.getTypes().addAll(tenant.getTypes().getType());
            }

            this.tenantService.updateTenant(tenantDO);

            return Response.ok(
                    jaxbObjectFactories.getOpenStackIdentityV2Factory().createTenant(tenantConverterCloudV20.toTenant(tenantDO)).getValue());

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

            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(creds).getValue());

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

            return Response.ok(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(creds).getValue());

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

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createCredential(creds).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    // Core Admin Token Methods
    private ResponseBuilder validateTokenInternal(HttpHeaders httpHeaders, String authToken, String tokenId, String tenantId, boolean applyRcnRoles) {
        try {
            NewRelic.setTransactionName(null, NewRelicTransactionNames.V2Validate.getTransactionName());
            final boolean sameToken = StringUtils.equals(authToken, tokenId);

            // User can validate his own token (B-80571:TK-165775).
            ScopeAccess callerToken = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            if (!sameToken) {
                //TODO: This token can be a Racker, Service or User of Proper Level
                authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.VALIDATE_TOKEN_GLOBAL.getRoleName());
            }

            if (StringUtils.isNotBlank(callerToken.getScope())) {
                throw new ForbiddenException("Cannot use scoped tokens to validate tokens.");
            }

            if (!StringUtils.deleteWhitespace(tokenId).equals(tokenId))  {
                throw new NotFoundException("Token not found.");
            }

            final ScopeAccess sa = checkAndGetToken(tokenId); // Throws not found exception if token can't not be decrypted

            /*
             Scoped tokens can not currently be validated through the v2 validate call. This is because external systems
             have been accustomed to a 200 meaning the token is valid for anything, whereas scoped tokens have limited
             functionality (currently within Identity only) and should not be considered valid user tokens in general.
              */
            if (sa.isAccessTokenExpired(new DateTime()) || StringUtils.isNotBlank(sa.getScope())) {
                throw new NotFoundException("Token not found.");
            }

            setNewRelicTransactionNameForValidateToken(sa, applyRcnRoles);

            AuthenticateResponse authenticateResponse;
            if (sa instanceof RackerScopeAccess) {
                authenticateResponse = authenticateResponseService.buildAuthResponseForValidateToken((RackerScopeAccess) sa);
            } else if (sa instanceof UserScopeAccess) {
                if (applyRcnRoles) {
                    authenticateResponse = authenticateResponseService.buildAuthResponseForValidateTokenApplyRcnRoles((UserScopeAccess) sa, tenantId);
                } else {
                    authenticateResponse = authenticateResponseService.buildAuthResponseForValidateToken((UserScopeAccess) sa, tenantId);
                }
            } else {
                if (applyRcnRoles) {
                    authenticateResponse = authenticateResponseService.buildAuthResponseForValidateTokenApplyRcnRoles((ImpersonatedScopeAccess) sa, tenantId);
                } else {
                    authenticateResponse = authenticateResponseService.buildAuthResponseForValidateToken((ImpersonatedScopeAccess) sa, tenantId);
                }
            }

            return Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createAccess(authenticateResponse).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private void setNewRelicTransactionNameForValidateToken(ScopeAccess token, boolean applyRcnRoles) {
        boolean isFederatedToken = token.getAuthenticatedBy().contains(AuthenticatedByMethodEnum.FEDERATION.getValue());

        String transactionName = null;
        if (token instanceof RackerScopeAccess) {
            if (isFederatedToken) {
                transactionName = NewRelicTransactionNames.V2ValidateFederatedRacker.getTransactionName();
            } else {
                transactionName = NewRelicTransactionNames.V2ValidateRacker.getTransactionName();
            }
        } else if (token instanceof UserScopeAccess) {
            if (isFederatedToken) {
                transactionName = NewRelicTransactionNames.V2ValidateFederatedDomain.getTransactionName();
            } else if (applyRcnRoles) {
                transactionName = NewRelicTransactionNames.V2ValidateDomainRcn.getTransactionName();
            } else {
                transactionName = NewRelicTransactionNames.V2ValidateDomain.getTransactionName();
            }
        } else if (token instanceof ImpersonatedScopeAccess) {
            transactionName =  NewRelicTransactionNames.V2ValidateImpersonation.getTransactionName();
        }
        if (StringUtils.isNotEmpty(transactionName)) {
            NewRelic.setTransactionName(null, transactionName);
        }
    }

    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String tenantId) {
        return validateTokenInternal(httpHeaders, authToken, tokenId, tenantId, false);
    }

    @Override
    public ResponseBuilder validateTokenApplyRcnRoles(HttpHeaders httpHeaders, String authToken, String tokenId, String tenantId) {
        return validateTokenInternal(httpHeaders, authToken, tokenId, tenantId, true);
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

    @Override
    public ResponseBuilder modifyDomainAdministrator(String authToken, String domainId, DomainAdministratorChange domainAdministratorChange) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            /*
             Verifies the user exists (else NotFoundException), is enabled (else NotAuthorizedException) AND has the
             specified role (else ForbiddenException). Does not validate domain is enabled, because when disabled all
             tokens for domain are immediately revoked.
             */
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.IDENTITY_DOMAIN_ADMIN_CHANGE.getRoleName());
            requestContextHolder.getRequestContext().getEffectiveCaller();

            // Verify both promote and demote users are specified
            if (StringUtils.isBlank(domainAdministratorChange.getPromoteUserId())
                    || StringUtils.isBlank(domainAdministratorChange.getDemoteUserId())) {
                throw new BadRequestException("Both promote and demote userIds must be provided", ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE);
            }

            // Verify promote and demote users are distinct
            if (domainAdministratorChange.getPromoteUserId().equalsIgnoreCase(domainAdministratorChange.getDemoteUserId())) {
                throw new ForbiddenException("Must specify different users to promote and demote", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
            }

            // Retrieve the 2 users being changed and verify they exist (else ForbiddenException)
            EndUser promotingEndUser = identityUserService.checkAndGetEndUserById(domainAdministratorChange.getPromoteUserId());
            EndUser demotingEndUser = identityUserService.checkAndGetEndUserById(domainAdministratorChange.getDemoteUserId());

            if (!(promotingEndUser instanceof User) || !(demotingEndUser instanceof User)) {
                throw new ForbiddenException("Both the promote and demote users must be Rackspace managed users", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
            }

            User promoteUser = (User) promotingEndUser;
            User demoteUser = (User) demotingEndUser;

            // Belong to same domain AND the provided domain in the path is the same as the users
            if (StringUtils.isBlank(domainId) || StringUtils.isBlank(promoteUser.getDomainId()) || StringUtils.isBlank(demoteUser.getDomainId())
                    || !promoteUser.getDomainId().equalsIgnoreCase(demoteUser.getDomainId())
                    || !domainId.equalsIgnoreCase(promoteUser.getDomainId())) {
                throw new ForbiddenException("Both the promote and demote users must belong to the same domain as the domain in the url", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
            }

            // Both users must be enabled
            if (promoteUser.isDisabled() || demoteUser.isDisabled()) {
                throw new ForbiddenException("Both the promote and demote users must be enabled", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
            }

            // Verify user to promote does NOT have the user-admin role.
            List<TenantRole> promoteUserRoles = tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(promoteUser);
            TenantRole promoteUserAdminRole = null;
            for (TenantRole tenantRole : promoteUserRoles) {
                if (IdentityUserTypeEnum.USER_ADMIN.getRoleName().equalsIgnoreCase(tenantRole.getName())) {
                    promoteUserAdminRole = tenantRole;
                    break;
                }
            }

            if (promoteUserAdminRole != null) {
                throw new ForbiddenException("Promote user is already an admin", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
            }

            // Verify user to demote has the user-admin role.
            List<TenantRole> demoteUserRoles = tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(demoteUser);
            TenantRole demoteUserAdminRole = null;
            for (TenantRole tenantRole : demoteUserRoles) {
                if (IdentityUserTypeEnum.USER_ADMIN.getRoleName().equalsIgnoreCase(tenantRole.getName())) {
                    demoteUserAdminRole = tenantRole;
                    break;
                }
            }

            if (demoteUserAdminRole == null) {
                throw new ForbiddenException("Demote user is not an admin", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
            }

            // Verify users have same propagating roles by creating map keyed by roleId w/ tenants it's assigned
            Map<String, Set<String>> promotePropRoles = new HashMap<>();
            for (TenantRole tenantRole : promoteUserRoles) {
                if (tenantRole.getPropagate()) {
                    promotePropRoles.put(tenantRole.getRoleRsId(), tenantRole.getTenantIds());
                }
            }

            Map<String, Set<String>> demotePropRoles = new HashMap<>();
            for (TenantRole tenantRole : demoteUserRoles) {
                if (tenantRole.getPropagate()) {
                    demotePropRoles.put(tenantRole.getRoleRsId(), tenantRole.getTenantIds());
                }
            }

            if (!promotePropRoles.equals(demotePropRoles)) {
                throw new ForbiddenException("The promote and demote users must have the same propagating roles", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
            }

            logger.info(String.format("Domain admin change for domain '%s'. Promoting user '%s' to user-admin and demoting user '%s'"
                    , promoteUser.getDomainId(), domainAdministratorChange.getPromoteUserId(), domainAdministratorChange.getDemoteUserId()));

            ClientRole userAdminRole = null;
            ClientRole userDefaultRole = null;
            if(identityConfig.getReloadableConfig().getCacheRolesWithoutApplicationRestartFlag()) {
                userAdminRole = applicationService.getCachedClientRoleByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()).asClientRole();
                userDefaultRole = applicationService.getCachedClientRoleByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()).asClientRole();
            } else {
                userAdminRole = authorizationService.getCachedIdentityRoleByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()).asClientRole();
                userDefaultRole = authorizationService.getCachedIdentityRoleByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()).asClientRole();
            }
            /*
             Modify the user to promote by first adding the user-admin role and then iterating through user's other roles
              and removing any roles assignable by user-admin and user classification roles
             */
            assignRoleToUser(promoteUser, userAdminRole);
            List<TenantRole> rolesDeletedFromPromotedUser = deleteUserClassificationAndRbacTenantRoles(promoteUserRoles);

            /*
             Modify the user to demote by first adding the user default role and then iterating through user's other roles
             and removing any roles assignable by user-admin and user classification roles
             */
            assignRoleToUser(demoteUser, userDefaultRole);
            List<TenantRole> rolesDeletedFromDemotedUser = deleteUserClassificationAndRbacTenantRoles(demoteUserRoles);

            // Send feed event changes for users
            atomHopperClient.asyncPost(promoteUser, AtomHopperConstants.ROLE);
            atomHopperClient.asyncPost(demoteUser, AtomHopperConstants.ROLE);

            if (logger.isInfoEnabled()) {
                try {
                    String promotedRemovedRoleNames = "";
                    for (TenantRole tenantRole : rolesDeletedFromPromotedUser) {
                        promotedRemovedRoleNames += tenantRole.getName() + ",";
                    }

                    String demotedRemovedRoleNames = "";
                    for (TenantRole tenantRole : rolesDeletedFromDemotedUser) {
                        demotedRemovedRoleNames += tenantRole.getName() + ",";
                    }

                    logger.info(String.format("Domain admin change for domain '%s'. Promoted user '%s' to user-admin and removed roles '%s'. Demoted user '%s' and removed roles '%s'"
                            , promoteUser.getDomainId(), domainAdministratorChange.getPromoteUserId(), promotedRemovedRoleNames, domainAdministratorChange.getDemoteUserId(), demotedRemovedRoleNames));
                } catch (Exception ex) {
                    // Eat. Just trying to log
                    logger.error("Error logging results of domain administrator change", ex);
                }
            }

            return Response.noContent();

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder switchDomainRcn(String authToken, String domainId, String destinationRcn) {
        try {
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);
            authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.DOMAIN_RCN_SWITCH.getRoleName());
            requestContextHolder.getRequestContext().getEffectiveCaller();

            if (StringUtils.isBlank(destinationRcn)) {
                throw new BadRequestException(ERROR_SWITCH_RCN_ON_DOMAIN_MISSING_RCN);
            }

            validator20.validateDomainRcn(destinationRcn);

            Domain domain = domainService.checkAndGetDomain(domainId);

            if (destinationRcn.equals(domain.getRackspaceCustomerNumber())) {
                return Response.noContent();
            }

            if (tenantService.countTenantsWithTypeInDomain(GlobalConstants.TENANT_TYPE_RCN, domainId) > 0) {
                throw new BadRequestException(ERROR_SWITCH_RCN_ON_DOMAIN_CONTAINING_RCN_TENANT);
            }

            // Note: only loading provisioned users at this time. Federated users are currently not able to get RCN roles.
            Iterable<User> users = domainService.getUsersByDomainId(domain.getDomainId());

            Iterable<ClientRole> rcnClientRoles = applicationService.getClientRolesByRoleType(RoleTypeEnum.RCN);
            Collection<String> rcnRoleIds = CollectionUtils.collect(rcnClientRoles, new Transformer<ClientRole, String>() {
                @Override
                public String transform(ClientRole clientRole) {
                    return clientRole.getId();
                }
            });

            // For each user, list all RCN roles assigned to the user and delete them
            for (User user : users) {
                Iterable<TenantRole> rcnTenantRoles = tenantService.getTenantRolesForUserWithId(user, rcnRoleIds);

                if (rcnTenantRoles != null && rcnTenantRoles.iterator().hasNext()) {
                    for (TenantRole rcnTenantRole : rcnTenantRoles) {
                        tenantService.deleteTenantRole(rcnTenantRole);
                    }

                    atomHopperClient.asyncPost(user, AtomHopperConstants.ROLE);
                }

            }

            domain.setRackspaceCustomerNumber(destinationRcn);
            domainService.updateDomain(domain);

            return Response.noContent();
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    /**
     * Given a list of tenantRoles, delete all tenant roles that:
     * <ol>
     *     <li>Are a user classification role</li>
     *     <li>Are assignable by a identity:user-manage user</li>
     * </ol>
     *
     * Return the list of roles that were removed.
     *
     * @return
     */
    private List<TenantRole> deleteUserClassificationAndRbacTenantRoles(List<TenantRole> tenantRoles) {
        List<TenantRole> tenantRolesDeleted = new ArrayList<>();
        for (TenantRole tenantRole : tenantRoles) {
            ImmutableClientRole icr = applicationService.getCachedClientRoleById(tenantRole.getRoleRsId());
            if (IdentityUserTypeEnum.isIdentityUserTypeRoleName(tenantRole.getName())
                    || icr.getRsWeight() == RoleLevelEnum.LEVEL_1000.getLevelAsInt()) {
                tenantRolesDeleted.add(tenantRole);
                tenantService.deleteTenantRole(tenantRole);
            }
        }
        return tenantRolesDeleted;
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
            jaxbCreds = jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials((ApiKeyCredentials) creds);
        }
        if ((PasswordCredentialsBase.class).isAssignableFrom(creds.getClass())) {
            jaxbCreds = jaxbObjectFactories.getOpenStackIdentityV2Factory().createCredential(creds);
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

    public void setJaxbObjectFactories(JAXBObjectFactories jaxbObjectFactories) {
        this.jaxbObjectFactories = jaxbObjectFactories;
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

