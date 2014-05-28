package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ServiceApis;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForCredentialType;
import com.rackspace.idm.api.resource.pagination.Paginator;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Capability;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Domains;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.validation.PrecedenceValidator;
import com.rackspace.idm.validation.Validator;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
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
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:14 PM
 */
@Component
public class DefaultCloud20Service implements Cloud20Service {

    public static final String NOT_AUTHORIZED = "Not Authorized";
    public static final String ID_MISMATCH = "Id in url does not match id in body.";
    public static final String USER_AND_USER_ID_MIS_MATCHED = "User and UserId mis-matched";
    public static final String RBAC = "rbac";

    public static final String FEATURE_RETURN_FULL_SERVICE_CATALOG_WHEN_MOSSO_TENANT_SPECIFIED = "feature.return.full.service.catalog.when.mosso.tenant.specified.in.v2.auth";
    public static final boolean FEATURE_RETURN_FULL_SERVICE_CATALOG_WHEN_MOSSO_TENANT_SPECIFIED_DEFAULT_VALUE = false;

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
    private GroupService groupService;

    @Autowired
    private UserService userService;

    @Autowired
    private CloudGroupBuilder cloudGroupBuilder;

    @Autowired
    private CloudKsGroupBuilder cloudKsGroupBuilder;

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
    private PolicyConverterCloudV20 policyConverterCloudV20;

    @Autowired
    private DomainService domainService;

    @Autowired
    private FederatedIdentityService federatedIdentityService;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private PolicyValidator policyValidator;

    @Autowired
    private CapabilityService capabilityService;

    @Autowired
    private CapabilityConverterCloudV20 capabilityConverterCloudV20;

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
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @Autowired
    private RoleService roleService;

    private com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory raxAuthObjectFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();

    private Map<String, JAXBElement<Extension>> extensionMap;

    private JAXBElement<Extensions> currentExtensions;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, EndpointTemplate endpoint) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(Integer.toString(endpoint.getId()));
            if (baseUrl.getGlobal()) {
                throw new BadRequestException("Cannot add a global endpoint to this tenant.");
            }
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
            validator20.validateEndpointTemplate(endpoint);

            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            CloudBaseUrl baseUrl = this.endpointConverterCloudV20.toCloudBaseUrl(endpoint);
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

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role) {
        try {
            ScopeAccess tokenScopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyIdentityAdminLevelAccess(tokenScopeAccess);
            User caller = (User) userService.getUserByScopeAccess(tokenScopeAccess);

            validateRole(role);

            if (StringUtils.isBlank(role.getServiceId())) {
                String msg = String.format("Setting default service to role %s",role.getName());
                logger.warn(msg);
                role.setServiceId(config.getString("cloudAuth.globalRoles.clientId"));
            }

            if (!authorizationService.authorizeCloudServiceAdmin(tokenScopeAccess)) {
                /*if(role.getServiceId().equals(config.getString("cloudAuth.clientId"))
                        || role.getServiceId().equals(config.getString("idm.clientId"))) {
                    String errMsg = "Cannot add roles to identity/Foundation service accounts";
                    throw new ForbiddenException(errMsg);
                }*/
                if (StringUtils.startsWithIgnoreCase(role.getName(), "identity:")) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }

            Application service = applicationService.checkAndGetApplication(role.getServiceId());

            ClientRole clientRole = roleConverterCloudV20.fromRole(role, service.getClientId());
            isRoleWeightValid(clientRole.getRsWeight());
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

    private void isRoleWeightValid(int weight) {
        List<Object> validWeights = config.getList("cloudAuth.allowedRoleWeights");
        if (!validWeights.contains(Integer.toString(weight))) {
            String errMsg = String.format("Allowed values for Weight field: %s", StringUtils.join(validWeights, " "));
            throw new BadRequestException(errMsg);
        }
    }

    private void validateRole(Role role) {
        if (role == null) {
            String errMsg = "role cannot be null";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        if (StringUtils.isBlank(role.getName())) {
            String errMsg = "Expecting name";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
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
            client.setRcn(getRackspaceCustomerId());

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
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            if (StringUtils.isBlank(tenant.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            // Our implmentation has the id and the name the same
            tenant.setId(tenant.getName());
            Tenant savedTenant = this.tenantConverterCloudV20.fromTenant(tenant);

            this.tenantService.addTenant(savedTenant);

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
            authorizationService.verifyUserManagedLevelAccess(scopeAccessByAccessToken);
            User caller = (User) userService.getUserByScopeAccess(scopeAccessByAccessToken);

            boolean isCreateUserInOneCall = false;

            if (config.getBoolean("createUser.fullPayload.enabled") == false) {
                if (usr.getSecretQA() != null ||
                        usr.getGroups() != null ||
                        usr.getRoles() != null) {
                    throw new BadRequestException("Can't specify secret qa, groups, or roles in body");
                }
            } else {
                if (usr.getSecretQA() != null ||
                        usr.getGroups() != null ||
                        usr.getRoles() != null) {
                    // Only identity:admin should be able to create a user including roles, groups and secret QA.
                    if (!authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken)) {
                        throw new ForbiddenException(NOT_AUTHORIZED);
                    };
                    // Since the domainId is used as the mossoId in the Create User In One Call call
                    // we need to make sure it is an integer so it can be used as a mossoId
                    try {
                        Integer.parseInt(usr.getDomainId());
                    } catch (Exception ex) {
                        throw new BadRequestException("DomainId must be an integer");
                    }
                    // If secretQA, groups or roles are populated then it's a createUserInOneCall call
                    isCreateUserInOneCall = true;
                }
                if (usr.getSecretQA() != null) {
                    if (StringUtils.isBlank(usr.getSecretQA().getQuestion())) {
                        throw new BadRequestException("Missing secret question");
                    }
                    if (StringUtils.isBlank(usr.getSecretQA().getAnswer())) {
                        throw new BadRequestException("Missing secret answer");
                    }
                }
            }

            boolean passwordProvided = !StringUtils.isBlank(usr.getPassword());
            User user = this.userConverterCloudV20.fromUser(usr);
            precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, getRoleNames(user.getRoles()));
            userService.setUserDefaultsBasedOnCaller(user, caller, isCreateUserInOneCall);
            userService.addUserV20(user, isCreateUserInOneCall);

            org.openstack.docs.identity.api.v2.User userTO = this.userConverterCloudV20.toUser(user, true);

            if(passwordProvided) {
                userTO.setPassword(null);
            }

            // This hack is to ensure backward compatibility for the original create user call that did
            // not return roles or groups
            if (!isCreateUserInOneCall) {
                userTO.setRoles(null);
                userTO.setGroups(null);
            }

            ResponseBuilder builder = Response.created(uriInfo.getRequestUriBuilder().path(user.getId()).build());

            return builder.entity(userTO);

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private void checkMaxNumberOfUsersInDomain(Iterable<User> users) {
        int maxNumberOfUsersInDomain = config.getInt("maxNumberOfUsersInDomain");

        //TODO: this does not work if domain has multiple user admins
        int numberUsers = 0;
        for (User user : users) {
            numberUsers++;
            if (numberUsers >= maxNumberOfUsersInDomain) {
                String errMsg = String.format("User cannot create more than %d users in an account.", maxNumberOfUsersInDomain);
                throw new BadRequestException(errMsg);
            }
        }
    }

    void assignDefaultRegionToDomainUser(User userDO) {
        if (userDO.getRegion() == null) {
            userDO.setRegion("default");
        }
    }

    void assignProperRole(ScopeAccess scopeAccessByAccessToken, User userDO) {
        ClientRole role = null;

        //If caller is an Service admin, give user admin role
        if (authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken)) {
            role = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthIdentityAdminRole());
        }
        //if caller is an admin, give user user-admin role
        if (authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken)) {
            role = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserAdminRole());
        }
        //if caller is a user admin, give user default role
        if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken) ||
            authorizationService.authorizeUserManageRole(scopeAccessByAccessToken)) {
            role = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserRole());
        }

        assignRoleToUser(userDO, role);
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId, UserForCreate user) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(scopeAccessByAccessToken);

            if (user.getPassword() != null) {
                validator.validatePasswordForCreateOrUpdate(user.getPassword());
            }

            User retrievedUser = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

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
            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken);
            boolean callerHasUserManageRole = authorizationService.authorizeUserManageRole(scopeAccessByAccessToken);
            boolean callerIsSubUser = authorizationService.authorizeCloudUser(scopeAccessByAccessToken);

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

            if (!user.isEnabled() && isUpdatingSelf) {
                throw new BadRequestException("User cannot enable/disable his/her own account.");
            }

            User userDO = this.userConverterCloudV20.fromUser(user);
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
            ScopeAccess scopeAccessForUserBeingUpdated = scopeAccessService.getScopeAccessForUser(retrievedUser);
            if (userDO.getRegion() != null && updateRegion) {
                defaultRegionService.validateDefaultRegion(userDO.getRegion(), scopeAccessForUserBeingUpdated);
            }

            userService.updateUser(userDO);
            userDO = userService.getUserById(userDO.getId());
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(userDO)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    User getUser(ScopeAccess scopeAccessByAccessToken) {
        String uid = scopeAccessService.getUserIdForParent(scopeAccessByAccessToken);
        return userService.getUser(uid);
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

    AuthenticateResponse authenticateFederatedDomain(AuthenticationRequest authenticationRequest,
                                                     com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        // ToDo: Validate Domain
        if(!domain.getName().equalsIgnoreCase(GlobalConstants.RACKSPACE_DOMAIN)){
            throw new BadRequestException("Invalid domain specified");
        }// The below is only for Racker Auth for now....

        AuthenticateResponse auth;
        BaseUser user = null;
        UserScopeAccess usa;
        RackerScopeAccess rsa;
        List<String> authenticatedBy = new ArrayList<String>();
        if (authenticationRequest.getCredential().getValue() instanceof PasswordCredentialsBase) {
            PasswordCredentialsBase creds = (PasswordCredentialsBase) authenticationRequest.getCredential().getValue();
            creds.setUsername(creds.getUsername().trim());
            validator20.validatePasswordCredentials(creds);
            Domain domainDO = domainConverterCloudV20.fromDomain(domain);
            UserAuthenticationResult result = authenticationService.authenticateDomainUsernamePassword(creds.getUsername(), creds.getPassword(), domainDO);
            user = result.getUser();
            ((Racker)user).setRackerId(((Racker) result.getUser()).getRackerId());
            authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSWORD);
        } else if (authenticationRequest.getCredential().getValue() instanceof RsaCredentials) {
            RsaCredentials creds = (RsaCredentials) authenticationRequest.getCredential().getValue();
            creds.setUsername(creds.getUsername().trim());
            validator20.validateUsername(creds.getUsername());
            Domain domainDO = domainConverterCloudV20.fromDomain(domain);
            UserAuthenticationResult result = authenticationService.authenticateDomainRSA(creds.getUsername(), creds.getTokenKey(), domainDO);
            user = result.getUser();
            ((Racker)user).setRackerId(((Racker) result.getUser()).getRackerId());
            authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_RSAKEY);
        }
        rsa = scopeAccessService.getValidRackerScopeAccessForClientId((Racker) user, getCloudAuthClientId(), authenticatedBy);

        usa = new UserScopeAccess();
        usa.setUsername(rsa.getRackerId());
        usa.setAccessTokenExp(rsa.getAccessTokenExp());
        usa.setAccessTokenString(rsa.getAccessTokenString());
        usa.setAuthenticatedBy(authenticatedBy);

        List<TenantRole> roleList = tenantService.getTenantRolesForUser(user);
        //Add Racker eDir Roles
        List<String> rackerRoles = null;
        if (((Racker) user).getRackerId() != null) {
            rackerRoles = userService.getRackerRoles(((Racker) user).getRackerId());
        }
        if (rackerRoles != null) {
            for (String r : rackerRoles) {
                TenantRole t = new TenantRole();
                t.setName(r);
                roleList.add(t);
            }
        }

        return auth = authConverterCloudV20.toAuthenticationResponse(user, usa, roleList, new ArrayList());
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) {
    /*
    TODO: Refactor this method. It's getting messy. Wait till after MFA though to avoid making it too difficult to follow the mfa changes
     */
        try {
            AuthResponseTuple authResponseTuple = new AuthResponseTuple();
            if (authenticationRequest.getCredential() == null && authenticationRequest.getToken() == null) {
                throw new BadRequestException("Invalid request body: unable to parse Auth data. Please review XML or JSON formatting.");
            }
            if (!StringUtils.isBlank(authenticationRequest.getTenantName()) && !StringUtils.isBlank(authenticationRequest.getTenantId())) {
                throw new BadRequestException("Invalid request. Specify tenantId OR tenantName, not both.");
            }
            // Check for domain in request
            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = authenticationRequest.getDomain();
            if(domain != null) {
                AuthenticateResponse auth = authenticateFederatedDomain(authenticationRequest, domain);
                return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(auth).getValue());
            }

            if (multiFactorCloud20Service.isMultiFactorEnabled() && authenticationRequest.getCredential() != null && authenticationRequest.getCredential().getValue() instanceof PasscodeCredentials) {
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
                restrictTenantInAuthentication(authenticationRequest, authResponseTuple);
            }
            else if (authenticationRequest.getToken() != null) {
                //TODO: What do when MFA token is provided...Should it just be refreshed similar to one-factor tokens?
                authResponseTuple = authWithToken.authenticate(authenticationRequest);
                /*
                This call to restrictTenant (and its corresponding LDAP call) appears to be completely unnecessary as AuthWithToken performs similar calls (just with different
                 error message. Leaving in for now just to limit the changes for MFA and there are tests verifying the call is made
                 both from the AuthWithToken class AND this authenticate method.
                 */
                restrictTenantInAuthentication(authenticationRequest, authResponseTuple);
            }
            else {
                //2-factor only applies when using a userAuth method (apikey or password)
                UserAuthenticationFactor userAuthenticationFactor = null;
                if (authenticationRequest.getCredential().getValue() instanceof PasswordCredentialsBase) {
                    userAuthenticationFactor = authWithPasswordCredentials;
                } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                    userAuthenticationFactor = authWithApiKeyCredentials;
                }
                else {
                    throw new BadRequestException("Unknown credential type");
                }
                UserAuthenticationResult authResult = userAuthenticationFactor.authenticate(authenticationRequest);

                if (multiFactorCloud20Service.isMultiFactorEnabled() && ((User)authResult.getUser()).isMultiFactorEnabled()) {
                    return multiFactorCloud20Service.performMultiFactorChallenge(((User) authResult.getUser()).getId(), authResult.getAuthenticatedBy());
                } else {
                    authResponseTuple = userAuthenticationFactor.createScopeAccessForUserAuthenticationResult(authResult);
                    restrictTenantInAuthentication(authenticationRequest, authResponseTuple);
                }
            }
            AuthenticateResponse auth = buildAuthResponse(authResponseTuple.getUserScopeAccess(), authResponseTuple.getImpersonatedScopeAccess(), authResponseTuple.getUser(), authenticationRequest);
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(auth).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    private void restrictTenantInAuthentication(AuthenticationRequest authenticationRequest, AuthResponseTuple authResponseTuple) {
        if (!StringUtils.isBlank(authenticationRequest.getTenantName()) && !tenantService.hasTenantAccess(authResponseTuple.getUser(), authenticationRequest.getTenantName())) {
            String errMsg = "Tenant with Name/Id: '" + authenticationRequest.getTenantName() + "' is not valid for User '" + authResponseTuple.getUser().getUsername() + "' (id: '" + authResponseTuple.getUser().getId() + "')";
            logger.warn(errMsg);
            throw new NotAuthenticatedException(errMsg);
        }
        if (!StringUtils.isBlank(authenticationRequest.getTenantId()) && !tenantService.hasTenantAccess(authResponseTuple.getUser(), authenticationRequest.getTenantId())) {
            String errMsg = "Tenant with Name/Id: '" + authenticationRequest.getTenantId() + "' is not valid for User '" + authResponseTuple.getUser().getUsername() + "' (id: '" + authResponseTuple.getUser().getId() + "')";
            logger.warn(errMsg);
            throw new NotAuthenticatedException(errMsg);
        }
    }

    @Override
    public ResponseBuilder validateSamlResponse(HttpHeaders httpHeaders, org.opensaml.saml2.core.Response samlResponse) {
        AuthData authInfo = federatedIdentityService.generateAuthenticationInfo(samlResponse);
        AuthenticateResponse response = authConverterCloudV20.toAuthenticationResponse(authInfo);
        return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(response).getValue());
    }

    public AuthenticateResponse buildAuthResponse(UserScopeAccess userScopeAccess, ScopeAccess impersonatedScopeAccess, User user, AuthenticationRequest authenticationRequest) {
        AuthenticateResponse auth;
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(userScopeAccess);
        // Remove Admin URLs if non admin token
        if (!this.authorizationService.authorizeCloudServiceAdmin(userScopeAccess)) {
            stripEndpoints(endpoints);
        }
        //filter endpoints by tenant
        String tenantId = authenticationRequest.getTenantId();
        String tenantName = authenticationRequest.getTenantName();
        List<TenantRole> roles = tenantService.getTenantRolesForUser(user);

        org.openstack.docs.identity.api.v2.Token convertedToken = null;
        if (impersonatedScopeAccess != null) {
            convertedToken = tokenConverterCloudV20.toToken(impersonatedScopeAccess);
        } else {
            convertedToken = tokenConverterCloudV20.toToken(userScopeAccess);
        }

        //tenant was specified
        if (!StringUtils.isBlank(tenantId) || !StringUtils.isBlank(tenantName)) {
            List<OpenstackEndpoint> tenantEndpoints = new ArrayList<OpenstackEndpoint>();
            Tenant tenant;

            if (!StringUtils.isBlank(tenantId)) {
                tenant = tenantService.getTenant(tenantId);
            } else {
                tenant = tenantService.getTenantByName(tenantName);
            }

            convertedToken.setTenant(convertTenantEntityToApi(tenant));

            if (shouldFilterServiceCatalogByTenant(tenant.getTenantId(), roles)) {
                for (OpenstackEndpoint endpoint : endpoints) {
                    if (tenant.getTenantId().equals(endpoint.getTenantId())) {
                        tenantEndpoints.add(endpoint);
                    }
                }
            } else {
                tenantEndpoints.addAll(endpoints);
            }

            auth = authConverterCloudV20.toAuthenticationResponse(user, userScopeAccess, roles, tenantEndpoints);
            auth.setToken(convertedToken);
        } else {
            auth = authConverterCloudV20.toAuthenticationResponse(user, userScopeAccess, roles, endpoints);
        }

        return auth;
    }

    private boolean shouldFilterServiceCatalogByTenant(String tenantId, List<TenantRole> roles) {
        // If the feature flag is false, then we should always filter the service catalog by tenant
        if (!config.getBoolean(FEATURE_RETURN_FULL_SERVICE_CATALOG_WHEN_MOSSO_TENANT_SPECIFIED, FEATURE_RETURN_FULL_SERVICE_CATALOG_WHEN_MOSSO_TENANT_SPECIFIED_DEFAULT_VALUE)) {
            return true;
        }

        // If the feature flag is true then we should filter the service catalog
        // when the tenant specified is NOT the mosso tenant
        return !isMossoTenant(tenantId, roles);
    }

    private boolean isMossoTenant(String tenantId, List<TenantRole> roles) {
        for (TenantRole role : roles) {
            if (role.getName().equals("compute:default")) {
                return role.getTenantIds().contains(tenantId);
            }
        }
        return tenantId.matches("\\d+");
    }

    User getUserByIdForAuthentication(String id) {
        User user = null;

        try {
            user = userService.checkAndGetUserById(id);
        } catch (NotFoundException e) {
            String errorMessage = String.format("Unable to authenticate user with credentials provided.");
            logger.warn(errorMessage);
            throw new NotAuthenticatedException(errorMessage, e);
        }
        return user;
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

            this.tenantService.deleteTenantRoleForUser(user, tenantRole);
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
            tenantService.deleteTenant(tenant.getTenantId());
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
    public ResponseBuilder deleteUserFromSoftDeleted(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));

            User user = checkAndGetSoftDeletedUser(userId);
            userService.deleteUser(user);
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
            User caller = getUser(scopeAccessByAccessToken);
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
            User user = this.userService.getUserById(userId);
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
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    void setEmptyUserValues(User user) {
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
            if (tenant.getBaseUrlIds() != null) {
                for (String id : tenant.getBaseUrlIds()) {
                    Integer baseUrlId = Integer.parseInt(id);
                    //ToDo: Do not add if in global list also
                    baseUrls.add(this.endpointService.getBaseUrlById(Integer.toString(baseUrlId)));
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
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            ScopeAccess sa = checkAndGetToken(tokenId);
            if (sa instanceof ImpersonatedScopeAccess) {
                ImpersonatedScopeAccess impersonatedScopeAccess = (ImpersonatedScopeAccess) sa;
                String impersonatedTokenId = impersonatedScopeAccess.getImpersonatingToken();
                sa = scopeAccessService.getScopeAccessByAccessToken(impersonatedTokenId);
            }

            if(sa == null) {
                throw new NotFoundException("Valid token not found");
            }
            List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(sa);
            EndpointList list = endpointConverterCloudV20.toEndpointList(endpoints);

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
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String serviceId, Integer marker, Integer limit) {
        try {
            authorizationService.verifyUserManagedLevelAccess(getScopeAccessForValidToken(authToken));

            PaginatorContext<ClientRole> context;
            User caller = userService.getUserByAuthToken(authToken);
            ClientRole userIdentityRole = applicationService.getUserIdentityRole(caller);

            if (StringUtils.isBlank(serviceId)) {
                context = this.applicationService.getAvailableClientRolesPaged(marker, limit, userIdentityRole.getRsWeight());
            } else {
                context = this.applicationService.getAvailableClientRolesPaged(serviceId, marker, limit, userIdentityRole.getRsWeight());
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
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, Integer marker, Integer limit) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Iterable<Application> clients = this.applicationService.getOpenStackServices();

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
            if (access instanceof FederatedToken) {
                //federated scope accesses has role / tenant information stored at the token level
                FederatedToken federatedToken = (FederatedToken)access;
                tenants = this.tenantService.getTenantsForFederatedTokenByTenantRoles(federatedToken);
            }
            else {
                User user = (User) userService.getUserByScopeAccess(access);
                tenants = this.tenantService.getTenantsForUserByTenantRoles(user);
            }

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
                String errMsg = "No Roles found User with id: " + userId;
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            User caller = getUser(callersScopeAccess);
            if (!(authorizationService.authorizeCloudServiceAdmin(callersScopeAccess)
                    || authorizationService.authorizeCloudIdentityAdmin(callersScopeAccess)
                    //user is requesting self
                    || user.getId().equals(caller.getId()))) {

                if(caller.getDomainId() == null) {
                    //caller is a user admin, user manage, or default user but with a null domain ID
                    //this is bad data, but protecting against it anyways
                    throw new ForbiddenException(NOT_AUTHORIZED);
                } else if(!caller.getDomainId().equals(user.getDomainId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }

                precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
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
            authorizationService.verifyRackerOrIdentityAdminAccess(callerScopeAccess);

            ImpersonatorType impersonatorType = null;
            BaseUser impersonator = null;
            if (callerScopeAccess instanceof RackerScopeAccess) {
                impersonatorType = ImpersonatorType.RACKER;
                validator20.validateImpersonationRequestForRacker(impersonationRequest);
                impersonator = this.userService.getRackerByRackerId(((RackerScopeAccess)callerScopeAccess).getRackerId());
                if(getCheckRackerImpersonateRole()){
                    List<String> rackerRoles = userService.getRackerRoles(((Racker)impersonator).getRackerId());
                    if(rackerRoles.isEmpty() || !rackerRoles.contains(getRackerImpersonateRole())){
                        throw new ForbiddenException("Missing RackImpersonation role needed for this operation.");
                    }
                }
            } else if (callerScopeAccess instanceof UserScopeAccess) {
                impersonatorType = ImpersonatorType.SERVICE;
                validator20.validateImpersonationRequestForService(impersonationRequest);
                impersonator = this.userService.getUserById(((UserScopeAccess)callerScopeAccess).getUserRsId());
            } else {
                //this shouldn't really happen do to verification that user is racker or identity admin, but just in case.
                // TODO: Should be a 403 rather than 401, but this is how it has been historically
                logger.warn(String.format("Invalid impersonation request. Unrecognized token type '%s'", callerScopeAccess));
                throw new NotAuthorizedException("User does not have access");
            }

            //validate the user being impersonated can be found and is allowed to be impersonated
            User user = userService.checkAndGetUserByName(impersonationRequest.getUser().getUsername());
            if (!isValidImpersonatee(user)) {
                throw new BadRequestException("User cannot be impersonated; No valid impersonation roles assigned");
            }

            ImpersonatedScopeAccess impersonatedToken = scopeAccessService.processImpersonatedScopeAccessRequest(impersonator, user, impersonationRequest, impersonatorType);
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
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        Domain domain = domainService.checkAndGetDomain(domainId);
        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain value = this.domainConverterCloudV20.toDomain(domain);
        return Response.ok(objectFactory.createDomain(value).getValue());
    }

    @Override
    public ResponseBuilder updateDomain(String authToken, String domainId, com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
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
            Iterable<User> users = domainService.getUsersByDomainId(domainId);
            if (users.iterator().hasNext()) {
                throw new BadRequestException("Cannot delete Domains which contain users");
            }
            Domain domain = domainService.checkAndGetDomain(domainId);
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
        Iterable<User> users;
        if (enabled == null) {
            users = domainService.getUsersByDomainId(domainId);
        } else {
            users = domainService.getUsersByDomainIdAndEnabledFlag(domainId, Boolean.valueOf(enabled));
        }

        return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
    }

    @Override
    public ResponseBuilder addUserToDomain(String authToken, String domainId, String userId) throws IOException, JAXBException {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        Domain domain = domainService.checkAndGetDomain(domainId);
        if (!domain.getEnabled()) {
            throw new ForbiddenException("Cannot add users to a disabled domain.");
        }

        User userDO = userService.checkAndGetUserById(userId);

        if (isServiceAdminOrIdentityAdmin(userDO)) {
            throw new ForbiddenException("Cannot add domains to admins or service-admins.");
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

    private boolean isServiceAdminOrIdentityAdmin(User userDO) {
        List<TenantRole> roles = tenantService.getGlobalRolesForUser(userDO);
        for (TenantRole role : roles) {
            if (role.getName().contains(config.getString("cloudAuth.adminRole"))) {
                return true;
            }
            if (role.getName().contains(config.getString("cloudAuth.serviceAdminRole"))) {
                return true;
            }
        }
        return false;
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
    public ResponseBuilder getPoliciesForEndpointTemplate(String authToken, String endpointTemplateId) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        CloudBaseUrl cloudBaseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);
        com.rackspace.idm.domain.entity.Policies savedPolicies = policyService.getPolicies(new ArrayList<String>(cloudBaseUrl.getPolicyList()));

        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
        Policies policies = policyConverterCloudV20.toPolicies(savedPolicies);
        return Response.ok().entity(objectFactory.createPolicies(policies).getValue());
    }

    @Override
    public ResponseBuilder updatePoliciesForEndpointTemplate(String authToken, String endpointTemplateId, Policies policies) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        CloudBaseUrl cloudBaseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);
        cloudBaseUrl.getPolicyList().clear();

        for (Policy policy : policies.getPolicy()) {
            String policyId = policy.getId();
            this.policyService.checkAndGetPolicy(policyId);
            cloudBaseUrl.getPolicyList().add(policyId);
        }

        endpointService.updateBaseUrl(cloudBaseUrl);

        return Response.noContent();
    }

    @Override
    public ResponseBuilder addPolicyToEndpointTemplate(String authToken, String endpointTemplateId, String policyId) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        CloudBaseUrl cloudBaseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);
        com.rackspace.idm.domain.entity.Policy policyEntity = this.policyService.checkAndGetPolicy(policyId);

        endpointService.addPolicyToEndpoint(cloudBaseUrl.getBaseUrlId(), policyEntity.getPolicyId());
        return Response.noContent();
    }

    @Override
    public ResponseBuilder deletePolicyToEndpointTemplate(String authToken, String endpointTemplateId, String policyId) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        CloudBaseUrl cloudBaseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);

        endpointService.deletePolicyToEndpoint(cloudBaseUrl.getBaseUrlId(), policyId);
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

    @Override
    public ResponseBuilder updateCapabilities(String authToken, Capabilities capabilities, String type, String version) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            com.rackspace.idm.domain.entity.Capabilities capabilitiesObj = capabilityConverterCloudV20.fromCapabilities(capabilities);
            capabilityService.updateCapabilities(capabilitiesObj.getCapability(),type, version);
            return Response.noContent();
        }catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getCapabilities(String authToken, String type, String version) {
        try{
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Iterable<Capability> capabilitiesDO = capabilityService.getCapabilities(type, version);
            Capabilities capabilities = capabilityConverterCloudV20.toCapabilities(capabilitiesDO).getValue();
            return Response.ok(capabilities);
        }catch (Exception ex){
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder removeCapabilities(String authToken, String type, String version) {
        try{
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            capabilityService.removeCapabilities(type, version);
            return Response.noContent();  //To change body of implemented methods use File | Settings | File Templates.
        }catch (Exception ex){
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getServiceApis(String authToken){
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        ServiceApis serviceApis = capabilityConverterCloudV20.toServiceApis(capabilityService.getServiceApis()).getValue();
        return Response.ok().entity(serviceApis);
    }

    public ResponseBuilder listUsersWithRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String roleId, Integer marker, Integer limit) {
        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            ClientRole role = this.applicationService.getClientRoleById(roleId);

            if (role == null) {
                throw new NotFoundException(String.format("Role with id: %s not found", roleId));
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
        ScopeAccess callerScopeAccess = getScopeAccessForValidToken(authToken);
        authorizationService.verifyUserLevelAccess(callerScopeAccess);

        User caller = getUser(callerScopeAccess);
        User user = userService.checkAndGetUserById(userId);

        boolean callerIsDefaultUser = authorizationService.authorizeCloudUser(callerScopeAccess);
        boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(callerScopeAccess);

        if (callerIsDefaultUser && (!caller.getId().equals(user.getId()))) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        } else if (callerIsUserAdmin && (!caller.getDomainId().equals(user.getDomainId()))) {
            throw new ForbiddenException(NOT_AUTHORIZED);
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

    public boolean isValidImpersonatee(User user) {
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
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups cloudGroups = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups();

            User user = userService.checkAndGetUserById(userId);

            Iterable<Group> groups = userService.getGroupsForUser(user.getId());

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
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Group group = groupService.checkAndGetGroupById(groupId);

            User caller = userService.getUserByAuthToken(authToken);
            User user = userService.checkAndGetUserById(userId);

            if (authorizationService.hasDefaultUserRole(user)) {
                throw new BadRequestException("Cannot add Sub-Users directly to a Group, must assign their Parent User.");
            }

            if (!userService.isUserInGroup(userId, group.getGroupId())) {

                precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);

                if (authorizationService.hasUserAdminRole(user)) {
                    List<User> subUsers = userService.getSubUsers(user);

                    for (User subUser : subUsers) {
                        userService.addGroupToUser(groupId, subUser.getId());
                        atomHopperClient.asyncPost(subUser, AtomHopperConstants.GROUP);
                    }
                }
                userService.addGroupToUser(groupId, userId);
                atomHopperClient.asyncPost(user, AtomHopperConstants.GROUP);
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

            User user = userService.checkAndGetUserById(userId);
            boolean isDefaultUser = authorizationService.hasDefaultUserRole(user);
            boolean isUserAdmin = authorizationService.hasUserAdminRole(user);

            if (isDefaultUser) {
                throw new BadRequestException("Cannot remove Sub-Users directly from a Group, must remove their Parent User.");
            }

            if (!userService.isUserInGroup(userId, group.getGroupId())) {
                throw new NotFoundException("Group '" + group.getName() + "' is not assigned to user.");
            }

            if (isUserAdmin) {
                List<User> subUsers = userService.getSubUsers(user);

                for (User subUser : subUsers) {
                    userService.deleteGroupFromUser(groupId, subUser.getId());
                    atomHopperClient.asyncPost(subUser, AtomHopperConstants.GROUP);
                }
            }
            userService.deleteGroupFromUser(groupId, userId);
            atomHopperClient.asyncPost(user, AtomHopperConstants.GROUP);
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
            PaginatorContext<User> users = userService.getUsersByGroupId(groupId, marker, limit);

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
            User caller = getUser(scopeAccessByAccessToken);

            //if default user & NOT user-manage
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken)
                    && !authorizationService.authorizeUserManageRole(scopeAccessByAccessToken)) {
                List<User> users = new ArrayList<User>();
                users.add(caller);
                return Response.ok(objFactories.getOpenStackIdentityV2Factory()
                        .createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
            }
            authorizationService.verifyUserManagedLevelAccess(scopeAccessByAccessToken);

            PaginatorContext<User> paginatorContext;
            if (authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken) ||
                    authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken)) {
                paginatorContext = this.userService.getAllEnabledUsersPaged(marker, limit);
            } else {
                if (caller.getDomainId() != null) {
                    String domainId = caller.getDomainId();
                    paginatorContext = this.userService.getAllUsersPagedWithDomain(domainId, marker, limit);
                } else {
                    throw new BadRequestException("User-admin has no domain");
                }
            }

            List<User> users = new ArrayList<User>();

            // If role is identity:user-manage then we need to filter out the identity:user-admin
            if (authorizationService.authorizeUserManageRole(scopeAccessByAccessToken)) {
                for (User user : paginatorContext.getValueList()) {
                    if (!authorizationService.hasUserAdminRole(user)) {
                        users.add(user);
                    }
                }
            } else {
                users = paginatorContext.getValueList();
            }

            String linkHeader = userPaginator.createLinkHeader(uriInfo, paginatorContext);

            return Response.status(200)
                    .header("Link", linkHeader)
                    .entity(objFactories.getOpenStackIdentityV2Factory()
                            .createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, Integer marker, Integer limit) {

        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);

            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            List<User> users = this.tenantService.getUsersForTenant(tenant.getTenantId(), marker, limit);

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUsers(userConverterCloudV20.toUserList(users)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                      String roleId, Integer marker, Integer limit) {

        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            ClientRole role = checkAndGetClientRole(roleId);

            List<User> users = this.tenantService.getUsersWithTenantRole(tenant, role, marker, limit);

            return Response.ok(objFactories.getOpenStackIdentityV2Factory()
                    .createUsers(this.userConverterCloudV20.toUserList(users)).getValue());

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
            tenantDO.setName(tenant.getName());

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
            //TODO: This token can be a Racker, Service or User of Proper Level
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            ScopeAccess sa = checkAndGetToken(tokenId);
            AuthenticateResponse access = objFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();
            access.setToken(this.tokenConverterCloudV20.toToken(sa));

            if (sa.isAccessTokenExpired(new DateTime())) {
                throw new NotFoundException("Token not found");
            }

            if (sa instanceof RackerScopeAccess) {
                RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) sa;
                Racker racker = userService.getRackerByRackerId(rackerScopeAccess.getRackerId());
                List<TenantRole> roleList = tenantService.getTenantRolesForUser(racker);

                //Add Racker eDir Roles
                List<String> rackerRoles = userService.getRackerRoles(racker.getRackerId());
                if (rackerRoles != null) {
                    for (String r : rackerRoles) {
                        TenantRole t = new TenantRole();
                        t.setName(r);
                        roleList.add(t);
                    }
                }

                access.setUser(userConverterCloudV20.toRackerForAuthenticateResponse(racker, roleList));
            }
            else if (sa instanceof FederatedToken) {
                FederatedToken federatedTokenInfo = (FederatedToken) sa;
                AuthData authData = federatedIdentityService.getAuthenticationInfo(federatedTokenInfo);
                validator20.validateTenantIdInRoles(tenantId, authData.getToken().getRoles());
                access = authConverterCloudV20.toAuthenticationResponse(authData);
            }
            else if (sa instanceof UserScopeAccess || sa instanceof ImpersonatedScopeAccess) {
                BaseUser impersonator;
                User user;
                List<TenantRole> roles;
                if (sa instanceof UserScopeAccess) {
                    UserScopeAccess usa = (UserScopeAccess) sa;
                    user = (User) userService.getUserByScopeAccess(usa);
                    roles = tenantService.getTenantRolesForUser(user);
                    validator20.validateTenantIdInRoles(tenantId, roles);
                    access.setToken(tokenConverterCloudV20.toToken(sa, roles));
                    access.setUser(userConverterCloudV20.toUserForAuthenticateResponse(user, roles));
                } else {
                    ImpersonatedScopeAccess isa = (ImpersonatedScopeAccess) sa;
                    impersonator = userService.getUserByScopeAccess(isa);
                    user = userService.getUser(isa.getImpersonatingUsername());
                    roles = tenantService.getTenantRolesForUser(user);
                    validator20.validateTenantIdInRoles(tenantId, roles);
                    access.setToken(tokenConverterCloudV20.toToken(isa, roles));
                    access.setUser(userConverterCloudV20.toUserForAuthenticateResponse(user, roles));
                    List<TenantRole> impRoles = this.tenantService.getGlobalRolesForUser(impersonator);
                    UserForAuthenticateResponse userForAuthenticateResponse = null;
                    if (impersonator instanceof User) {
                        userForAuthenticateResponse = userConverterCloudV20.toUserForAuthenticateResponse((User)impersonator, impRoles);
                    } else if (impersonator instanceof Racker) {
                        userForAuthenticateResponse = userConverterCloudV20.toRackerForAuthenticateResponse((Racker)impersonator, impRoles);
                    } else {
                        throw new IllegalStateException("Unrecognized type of user '" + user.getClass().getName() + "'");
                    }
                    com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
                    JAXBElement<UserForAuthenticateResponse> impersonatorJAXBElement = objectFactory.createImpersonator(userForAuthenticateResponse);
                    access.getAny().add(impersonatorJAXBElement);
                }
            }

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(access).getValue());

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

        if (authToken.equals(tokenId)) {
            scopeAccessService.expireAccessToken(tokenId);
            return Response.status(204);
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
    public ResponseBuilder getPolicies(String authToken) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        com.rackspace.idm.domain.entity.Policies savedPolicies = this.policyService.getPolicies();
        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
        Policies policies = policyConverterCloudV20.toPolicies(savedPolicies);
        return Response.ok().entity(objectFactory.createPolicies(policies).getValue());
    }

    @Override
    public ResponseBuilder addPolicy(UriInfo uriInfo, String authToken, Policy policy) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            com.rackspace.idm.domain.entity.Policy savedPolicy = this.policyConverterCloudV20.fromPolicy(policy);
            policyValidator.validatePolicyName(policy.getName());
            this.policyService.addPolicy(savedPolicy);
            String policyId = savedPolicy.getPolicyId();
            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            URI build = requestUriBuilder.path(policyId).build();
            return Response.created(build);
        } catch (DuplicateException de) {
            return exceptionHandler.conflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getPolicy(String authToken, String policyId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            com.rackspace.idm.domain.entity.Policy policyEnt = this.policyService.checkAndGetPolicy(policyId);
            com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
            Policy policy = policyConverterCloudV20.toPolicy(policyEnt);
            return Response.ok().entity(objectFactory.createPolicy(policy).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updatePolicy(String authToken, String policyId, Policy policy) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            policyValidator.validatePolicyName(policy.getName());
            policyService.checkAndGetPolicy(policyId);
            com.rackspace.idm.domain.entity.Policy updatePolicy = this.policyConverterCloudV20.fromPolicy(policy);
            this.policyService.updatePolicy(policyId, updatePolicy);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }

    }

    @Override
    public ResponseBuilder deletePolicy(String authToken, String policyId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            this.policyService.softDeletePolicy(policyId);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    ClientRole checkAndGetClientRole(String id) {
        ClientRole cRole = this.applicationService.getClientRoleById(id);
        if (cRole == null) {
            String errMsg = String.format("Role %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return cRole;
    }

    ScopeAccess checkAndGetToken(String tokenId) {
        ScopeAccess sa = this.scopeAccessService.getScopeAccessByAccessToken(tokenId);

        if (sa == null) {
            String errMsg = String.format("Token %s not found", tokenId);
            logger.warn(errMsg);
            throw new NotFoundException("Token not found.");
        }

        return sa;
    }

    User checkAndGetSoftDeletedUser(String id) {
        User user = this.userService.getSoftDeletedUser(id);

        if (user == null) {
            String errMsg = String.format("User %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException("User not found");
        }

        return user;
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

    void stripEndpoints(List<OpenstackEndpoint> endpoints) {
        for (int i = 0; i < endpoints.size(); i++) {
            for (CloudBaseUrl baseUrl : endpoints.get(i).getBaseUrls()) {
                baseUrl.setAdminUrl(null);
            }
        }
    }

    private Boolean getGenerateApiKeyUserForCreate(){
        return config.getBoolean("generate.apiKey.userForCreate");
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private String getCloudAuthUserAdminRole() {
        return config.getString("cloudAuth.userAdminRole");
    }

    private String getCloudAuthUserRole() {
        return config.getString("cloudAuth.userRole");
    }

    private String getCloudAuthIdentityAdminRole() {
        return config.getString("cloudAuth.adminRole");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }

    private String getRackerImpersonateRole(){
        return config.getString("racker.impersonate.role");
    }

    private Boolean getCheckRackerImpersonateRole(){
        return config.getBoolean("feature.restrict.impersonation.to.rackers.with.role.enabled", false);
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

    TenantForAuthenticateResponse convertTenantEntityToApi(Tenant tenant) {
        TenantForAuthenticateResponse tenantForAuthenticateResponse = new TenantForAuthenticateResponse();
        tenantForAuthenticateResponse.setId(tenant.getTenantId());
        tenantForAuthenticateResponse.setName(tenant.getName());
        return tenantForAuthenticateResponse;
    }

    private com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain checkDomainFromAuthRequest(AuthenticationRequest authenticationRequest) {
        if(authenticationRequest.getAny() != null && authenticationRequest.getAny().size() > 0) {
            for(int i=0;i<authenticationRequest.getAny().size();i++) {
                try {
                    com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = null;
                    if(authenticationRequest.getAny().get(i) instanceof com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain) {
                        domain = (com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain) authenticationRequest.getAny().get(i);
                        return domain;
                    }else if(((JAXBElement)authenticationRequest.getAny().get(i)).getValue() instanceof com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain) {
                        domain = (com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain) ((JAXBElement)authenticationRequest.getAny().get(i)).getValue();
                        return domain;
                    }
                } catch (Exception ex){

                }
            }
        }
        return null;
    }

    private List<String> getRoleNames(List<TenantRole> tenantRoles) {
        List<String> roleNames = new ArrayList<String> ();
        if (tenantRoles != null) {
            for (TenantRole tenantRole : tenantRoles) {
                roleNames.add(tenantRole.getName());
            }
        }

        return roleNames;
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

