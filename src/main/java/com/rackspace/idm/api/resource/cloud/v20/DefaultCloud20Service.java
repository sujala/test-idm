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
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.validation.Validator;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.resource.pagination.Paginator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Domains;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.validation.PrecedenceValidator;
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

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
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
    public static final String USER_AND_USER_ID_MIS_MATCHED = "User and UserId mis-matched";
    public static final int MAX_GROUP_NAME = 200;
    public static final int MAX_GROUP_DESC = 1000;
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
    private DelegateCloud20Service delegateCloud20Service;

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
    private DomainsConverterCloudV20 domainsConverterCloudV20;

    @Autowired
    private PolicyConverterCloudV20 policyConverterCloudV20;

    @Autowired
    private PoliciesConverterCloudV20 policiesConverterCloudV20;

    @Autowired
    private DomainService domainService;

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

    private com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory raxAuthObjectFactory = new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory();

    private Map<String, JAXBElement<Extension>> extensionMap;

    private JAXBElement<Extensions> currentExtensions;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String CLOUD_AUTH_ROUTING = "useCloudAuth";

    public static final String GA_SOURCE_OF_TRUTH = "gaIsSourceOfTruth";

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, EndpointTemplate endpoint) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(endpoint.getId());
            if (baseUrl.getGlobal()) {
                throw new BadRequestException("Cannot add a global endpoint to this tenant.");
            }
            tenant.addBaseUrlId(String.valueOf(endpoint.getId()));
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

            if (role == null) {
                String errMsg = "role cannot be null";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(role.getServiceId())) { // We now default to an application for all roles not specifying one
                role.setServiceId(config.getString("cloudAuth.clientId"));
            }

            if (StringUtils.isBlank(role.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            if (StringUtils.startsWithIgnoreCase(role.getName(), "identity:")) {
                authorizationService.verifyServiceAdminLevelAccess(tokenScopeAccess);
            }

            Application service = applicationService.checkAndGetApplication(role.getServiceId());

            ClientRole clientRole = roleConverterCloudV20.toClientRoleFromRole(role, service.getClientId());

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
            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            if (authorizationService.authorizeCloudUserAdmin(scopeAccess)) {
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

            TenantRole tenantrole = new TenantRole();
            tenantrole.setName(role.getName());
            tenantrole.setClientId(role.getClientId());
            tenantrole.setRoleRsId(role.getId());
            tenantrole.setUserId(user.getId());
            tenantrole.setTenantIds(new String[]{tenant.getTenantId()});

            this.tenantService.addTenantRoleToUser(user, tenantrole);

            return Response.ok();

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service) {
        try {
            ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(authToken);
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
            client.setRCN(getRackspaceCustomerId());

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
            Tenant savedTenant = this.tenantConverterCloudV20.toTenantDO(tenant);

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
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, UserForCreate userForCreate) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserAdminLevelAccess(scopeAccessByAccessToken);
            validator.validate20User(userForCreate);

            String password = userForCreate.getPassword();
            boolean emptyPassword = StringUtils.isBlank(password);

            if (password != null) {
                validator.validatePasswordForCreateOrUpdate(userForCreate.getPassword());
            } else {
                password = Password.generateRandom(false).getValue();
                userForCreate.setPassword(password);
            }

            User userDO = this.userConverterCloudV20.toUserDO(userForCreate);
            User caller = userService.getUserByScopeAccess(scopeAccessByAccessToken);

            //if caller is a user-admin, give user same mosso and nastId and verifies that it has less then 100 subusers
            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken);
            boolean callerIsIdentityAdmin = authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken);
            boolean callerIsServiceAdmin = authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken);

            if (callerIsUserAdmin) {
                //TODO pagination index and offset
                Users users;
                String domainId = caller.getDomainId();
                if (domainId == null) {
                    throw new BadRequestException("User-Admin does not have a Domain");
                }
                FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParamName.DOMAIN_ID, domainId)};
                users = userService.getAllUsers(filters);
                int numSubUsers = config.getInt("numberOfSubUsers");
                if (users != null && users.getUsers() != null && users.getUsers().size() > numSubUsers) {
                    String errMsg = String.format("User cannot create more than %d sub-accounts.", numSubUsers);
                    throw new BadRequestException(errMsg);
                }
                userDO.setMossoId(caller.getMossoId());
                userDO.setNastId(caller.getNastId());

                // If creating sub-user, set DomainId of caller
                assignUserToCallersDomain(caller, userDO);
            }

            String domainId = userDO.getDomainId();
            if (domainId != null) {
                domainId = domainId.trim();
            }

            if (StringUtils.isEmpty(domainId) && callerIsUserAdmin) {
                throw new BadRequestException("A Domain ID must be specified.");
            } else if (callerIsServiceAdmin && (!StringUtils.isEmpty(userDO.getDomainId()))) {
                throw new BadRequestException("Identity-admin cannot be created with a domain");
            } else if (callerIsIdentityAdmin) {
                if (StringUtils.isEmpty(domainId)) {
                    throw new BadRequestException("User-admin cannot be created without a domain");
                }
                domainService.createNewDomain(userDO.getDomainId());
            }

            if (callerIsIdentityAdmin || callerIsUserAdmin) {
                if (userDO.getRegion() != null) {
                    defaultRegionService.validateDefaultRegion(userDO.getRegion());
                }
            }
            userService.addUser(userDO);
            assignProperRole(httpHeaders, authToken, scopeAccessByAccessToken, userDO);

            //after user is created and caller is a user admin, add tenant roles to default user
            if (callerIsUserAdmin) {
                tenantService.addCallerTenantRolesToUser(caller, userDO);

                if (caller != null) {
                    List<Group> groups = groupService.getGroupsForUser(caller.getId());

                    for (Group group :groups) {
                        groupService.addGroupToUser(group.getGroupId(), userDO.getId());
                    }
                }
            }

            UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();
            String id = userDO.getId();
            URI build = requestUriBuilder.path(id).build();

            org.openstack.docs.identity.api.v2.ObjectFactory openStackIdentityV2Factory = objFactories.getOpenStackIdentityV2Factory();
            org.openstack.docs.identity.api.v2.User value = userConverterCloudV20.toUser(userDO);

            //Will only print password if not provided
            if (emptyPassword) {
                value.getOtherAttributes().put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"),
                        password);
            }
            ResponseBuilder created = Response.created(build);
            return created.entity(openStackIdentityV2Factory.createUser(value).getValue());
        } catch (DuplicateException de) {
            return exceptionHandler.conflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    void assignDefaultRegionToDomainUser(User userDO) {
        if (userDO.getRegion() == null) {
            userDO.setRegion("default");
        }
    }

    void assignProperRole(HttpHeaders httpHeaders, String authToken, ScopeAccess scopeAccessByAccessToken, User userDO) {
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
        if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
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
            if (!userId.equals(user.getId()) && user.getId() != null) {
                throw new BadRequestException("Id in url does not match id in body.");
            }

            //if caller is default user, usedId must match callers user id
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken)) {
                User caller = userService.getUserByAuthToken(authToken);
                if (!caller.getId().equals(retrievedUser.getId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }
            //if user admin, verify domain
            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken);
            if (callerIsUserAdmin) {
                User caller = userService.getUserByAuthToken(authToken);
                authorizationService.verifyDomain(caller, retrievedUser);
            }

            if (!StringUtils.isBlank(user.getUsername())) {
                validator.isUsernameValid(user.getUsername());
            }

            if (!user.isEnabled()) {
                User caller = userService.getUserByAuthToken(authToken);
                if (caller.getId().equals(userId)) {
                    throw new BadRequestException("User cannot enable/disable his/her own account.");
                }
            }

            User userDO = this.userConverterCloudV20.toUserDO(user);
            if (userDO.isDisabled()) {
                atomHopperClient.asyncPost(retrievedUser, AtomHopperConstants.DISABLED);
            }
            Boolean updateRegion = true;
            if (userDO.getRegion() != null && retrievedUser != null) {
                if (userDO.getRegion().equals(retrievedUser.getRegion())) {
                    updateRegion = false;
                }
            }
            retrievedUser.copyChanges(userDO);
            ScopeAccess scopeAccessForUserBeingUpdated = scopeAccessService.getScopeAccessByUserId(userId);
            if (userDO.getRegion() != null && updateRegion) {
                defaultRegionService.validateDefaultRegion(userDO.getRegion(), scopeAccessForUserBeingUpdated);
            }
            userService.updateUser(retrievedUser, false);
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(retrievedUser)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    User getUser(ScopeAccess scopeAccessByAccessToken) {
        String uid = scopeAccessByAccessToken.getLDAPEntry().getAttributeValue(LdapRepository.ATTR_UID);
        return userService.getUser(uid);
    }

    void assignUserToCallersDomain(User caller, User userDO) {
        if (caller.getDomainId() == null) {
            throw new BadRequestException("User must belong to a domain to create a sub-user");
        }
        userDO.setDomainId(caller.getDomainId());
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

            if (credentials.getValue() instanceof PasswordCredentialsRequiredUsername) {
                PasswordCredentialsRequiredUsername userCredentials = (PasswordCredentialsRequiredUsername) credentials.getValue();
                validator20.validatePasswordCredentialsForCreateOrUpdate(userCredentials);
                user = userService.checkAndGetUserById(userId);
                if (!userCredentials.getUsername().equals(user.getUsername())) {
                    String errMsg = USER_AND_USER_ID_MIS_MATCHED;
                    logger.warn(errMsg);
                    throw new BadRequestException(errMsg);
                }
                user.setPassword(userCredentials.getPassword());
                userService.updateUser(user, false);
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
                userService.updateUser(user, false);
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
            authorizationService.verifyUserAdminLevelAccess(scopeAccessByAccessToken);

            ClientRole cRole = checkAndGetClientRole(roleId);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, cRole);

            checkForMultipleIdentityRoles(user, cRole);

            if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
                if (!caller.getDomainId().equalsIgnoreCase(user.getDomainId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
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

    ResponseBuilder authenticateFederatedDomain(HttpHeaders httpHeaders,
                                                        AuthenticationRequest authenticationRequest,
                                                        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain) {
        // ToDo: Validate Domain
        if(!domain.getName().toUpperCase().equals("RACKSPACE")){
            throw new BadRequestException("Invalid domain specified");
        }// The below is only for Racker Auth for now....

        AuthenticateResponse auth;
        User user = null;
        UserScopeAccess usa = null;
        RackerScopeAccess rsa = null;
        if (authenticationRequest.getCredential().getValue() instanceof PasswordCredentialsRequiredUsername) {
            PasswordCredentialsRequiredUsername creds = (PasswordCredentialsRequiredUsername) authenticationRequest.getCredential().getValue();
            creds.setUsername(creds.getUsername().trim());
            validator20.validatePasswordCredentials(creds);
            Domain domainDO = domainConverterCloudV20.toDomainDO(domain);
            UserAuthenticationResult result = authenticationService.authenticateDomainUsernamePassword(creds.getUsername(), creds.getPassword(), domainDO);
            user = result.getUser();
            user.setId(((Racker) result.getUser()).getRackerId());
        } else if (authenticationRequest.getCredential().getValue() instanceof RsaCredentials) {
            RsaCredentials creds = (RsaCredentials) authenticationRequest.getCredential().getValue();
            creds.setUsername(creds.getUsername().trim());
            validator20.validateUsername(creds.getUsername());
            Domain domainDO = domainConverterCloudV20.toDomainDO(domain);
            UserAuthenticationResult result = authenticationService.authenticateDomainRSA(creds.getUsername(), creds.getTokenKey(), domainDO);
            user = result.getUser();
            user.setId(((Racker) result.getUser()).getRackerId());
        }
        rsa = (RackerScopeAccess)scopeAccessService.getValidRackerScopeAccessForClientId(user.getUniqueId(), user.getId(), getCloudAuthClientId());

        usa = new UserScopeAccess();
        usa.setUsername(rsa.getRackerId());
        usa.setAccessTokenExp(rsa.getAccessTokenExp());
        usa.setAccessTokenString(rsa.getAccessTokenString());

        List<TenantRole> roleList = tenantService.getTenantRolesForUser(user);
        //Add Racker eDir Roles
        List<String> rackerRoles = userService.getRackerRoles(user.getId());
        if (rackerRoles != null) {
            for (String r : rackerRoles) {
                TenantRole t = new TenantRole();
                t.setName(r);
                roleList.add(t);
            }
        }
        List tenantEndpoints = new ArrayList();
        auth = authConverterCloudV20.toAuthenticationResponse(user, usa, roleList, tenantEndpoints);

        // removing serviceId from response for now
        auth = removeServiceIdFromAuthResponse(auth);

        return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(auth).getValue());
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) {
        try {
            AuthResponseTuple authResponseTuple = new AuthResponseTuple();
            if (authenticationRequest.getCredential() == null && authenticationRequest.getToken() == null) {
                throw new BadRequestException("Invalid request body: unable to parse Auth data. Please review XML or JSON formatting.");
            }
            if (!StringUtils.isBlank(authenticationRequest.getTenantName()) && !StringUtils.isBlank(authenticationRequest.getTenantId())) {
                throw new BadRequestException("Invalid request. Specify tenantId OR tenantName, not both.");
            }
            // Check for domain in request
            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = checkDomainFromAuthRequest(authenticationRequest);
            if(domain != null) {
                return authenticateFederatedDomain(httpHeaders, authenticationRequest, domain);
            }

            if (authenticationRequest.getToken() != null) {
                authResponseTuple = authWithToken.authenticate(authenticationRequest);
            } else if (authenticationRequest.getCredential().getValue() instanceof PasswordCredentialsRequiredUsername) {
                authResponseTuple = authWithPasswordCredentials.authenticate(authenticationRequest);
            } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                authResponseTuple = authWithApiKeyCredentials.authenticate(authenticationRequest);
            }

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

            AuthenticateResponse auth = buildAuthResponse(authResponseTuple.getUserScopeAccess(), authResponseTuple.getImpersonatedScopeAccess(), authResponseTuple.getUser(), authenticationRequest);
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createAccess(auth).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
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
            if (!StringUtils.isBlank(tenantId)) {
                convertedToken.setTenant(convertTenantEntityToApi(tenantService.getTenant(tenantId)));
                for (OpenstackEndpoint endpoint : endpoints) {
                    if (tenantId.equals(endpoint.getTenantId())) {
                        tenantEndpoints.add(endpoint);
                    }
                }
            }
            if (!StringUtils.isBlank(tenantName)) {
                convertedToken.setTenant(convertTenantEntityToApi(tenantService.getTenantByName(tenantName)));
                for (OpenstackEndpoint endpoint : endpoints) {
                    if (tenantName.equals(endpoint.getTenantName())) {
                        tenantEndpoints.add(endpoint);
                    }
                }
            }
            auth = authConverterCloudV20.toAuthenticationResponse(user, userScopeAccess, roles, tenantEndpoints);
            auth.setToken(convertedToken);
        } else {
            auth = authConverterCloudV20.toAuthenticationResponse(user, userScopeAccess, roles, endpoints);
        }

        // removing serviceId from response for now
        auth = removeServiceIdFromAuthResponse(auth);
        return auth;
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
    public ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String tenantId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            ScopeAccess sa = checkAndGetToken(tokenId);
            User user = userService.getUserByAuthToken(tokenId);

            if (!StringUtils.isBlank(tenantId)) {
                UserScopeAccess usa = (UserScopeAccess) sa;
                List<TenantRole> roles = this.tenantService.getTenantRolesForUser(user);
                if (!tenantService.isTenantIdContainedInTenantRoles(tenantId, roles)) {
                    throw new NotFoundException();
                }
            }
            return Response.ok();
        } catch (BadRequestException bre) {
            return exceptionHandler.badRequestExceptionResponse(bre.getMessage());
        } catch (NotAuthorizedException nae) {
            return Response.ok().status(Status.UNAUTHORIZED);
        } catch (ForbiddenException fe) {
            return Response.ok().status(Status.FORBIDDEN);
        } catch (NotFoundException nfe) {
            return Response.ok().status(Status.NOT_FOUND);
        } catch (Exception ex) {
            return Response.ok().status(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, String endpointId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            Tenant tenant = tenantService.checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = endpointService.checkAndGetEndpointTemplate(endpointId);
            tenant.removeBaseUrlId(String.valueOf(baseUrl.getBaseUrlId()));
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
            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            if (authorizationService.authorizeCloudUserAdmin(scopeAccess)) {
                if (!caller.getDomainId().equals(user.getDomainId())) {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }

            ClientRole role = checkAndGetClientRole(roleId);

            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, role);

            TenantRole tenantrole = new TenantRole();
            tenantrole.setClientId(role.getClientId());
            tenantrole.setRoleRsId(role.getId());
            tenantrole.setUserId(user.getId());
            tenantrole.setTenantIds(new String[]{tenant.getTenantId()});

            this.tenantService.deleteTenantRoleForUser(user, tenantrole);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
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
            authorizationService.verifyUserAdminLevelAccess(scopeAccessByAccessToken);
            User user = userService.checkAndGetUserById(userId);
            //is same domain?
            if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
                User caller = userService.getUserByAuthToken(authToken);
                authorizationService.verifyDomain(caller, user);
            }
            if (authorizationService.hasUserAdminRole(user) && userService.hasSubUsers(userId)) {
                throw new BadRequestException("Please delete sub-users before deleting last user-admin for the account");
            }
            userService.softDeleteUser(user);

            atomHopperClient.asyncPost(user, AtomHopperConstants.DELETED);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserFromSoftDeleted(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(authToken);
            authorizationService.verifyServiceAdminLevelAccess(scopeAccess);

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
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            if (credentialType.equals(JSONConstants.PASSWORD_CREDENTIALS)) {
                IdentityFault fault = new IdentityFault();
                fault.setCode(HttpServletResponse.SC_NOT_IMPLEMENTED);
                fault.setMessage("Method not implemented");
                return Response.ok(
                        objFactories.getOpenStackIdentityV2Factory().createIdentityFault(fault))
                        .status(HttpServletResponse.SC_NOT_IMPLEMENTED);
            }

            if (!credentialType.equals(JSONConstants.APIKEY_CREDENTIALS)) {
                throw new BadRequestException("unsupported credential type");
            }

            User user = userService.checkAndGetUserById(userId);

            if (user.getApiKey() == null) {
                throw new NotFoundException("Credential type RAX-KSKEY:apiKeyCredentials was not found for User with Id: " + user.getId());
            }

            user.setApiKey("");

            userService.updateUser(user, false);

            return Response.noContent();

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserAdminLevelAccess(scopeAccessByAccessToken);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByAuthToken(authToken);

            if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
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

            if (user.getId().equals(caller.getId()) && StringUtils.startsWithIgnoreCase(role.getName(), "identity:")) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            precedenceValidator.verifyCallerPrecedenceOverUser(caller, user);
            precedenceValidator.verifyCallerRolePrecedence(caller, role);

            this.tenantService.deleteGlobalRole(role);
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

            if (!tenant.containsBaseUrlId(endpointId)) {
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
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            ClientRole role = checkAndGetClientRole(roleId);
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
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken)) {
                if (caller.getId().equals(userId)) {
                    return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUser(this.userConverterCloudV20.toUser(caller)).getValue());
                } else {
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }
            authorizationService.verifyUserAdminLevelAccess(scopeAccessByAccessToken);
            User user = this.userService.getUserById(userId);
            if (user == null) {
                String errMsg = String.format("User with id: '%s' was not found", userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }
            setEmptyUserValues(user);
            if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
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

            User requester = userService.getUserByScopeAccess(requesterScopeAccess);

            if (authorizationService.authorizeCloudUserAdmin(requesterScopeAccess)) {
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
    public ResponseBuilder getUserPasswordCredentials(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(scopeAccessByAccessToken);

            User user = this.userService.getUserById(userId);
            User caller = getUser(scopeAccessByAccessToken);

            if (user == null) {
                String errMsg = String.format("User with id: %s does not exist", userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            boolean callerIsServiceAdmin = authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken);
            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken);

            if (!callerIsServiceAdmin && !caller.getId().equals(userId)) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }

            if (StringUtils.isBlank(user.getPassword())) {
                throw new NotFoundException("User doesn't have password credentials");
            }
            PasswordCredentialsRequiredUsername userCreds = new PasswordCredentialsRequiredUsername();
            userCreds.setPassword(user.getPassword());
            userCreds.setUsername(user.getUsername());
            JAXBElement<? extends CredentialType> creds = objFactories.getOpenStackIdentityV2Factory().createCredential(userCreds);

            return Response.ok(creds.getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(scopeAccessByAccessToken);

            User user = this.userService.getUserById(userId);
            User caller = getUser(scopeAccessByAccessToken);

            if (user == null) {
                String errMsg = String.format("User with id: %s does not exist", userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            boolean callerIsDefaultUser = authorizationService.authorizeCloudUser(scopeAccessByAccessToken);
            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken);

            if (callerIsUserAdmin) {
                authorizationService.verifyDomain(caller, user);
            } else if (callerIsDefaultUser && !caller.getId().equals(userId)) {
                throw new ForbiddenException(NOT_AUTHORIZED);
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
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, String marker, Integer limit) {
        try {
            ScopeAccess callerScopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(callerScopeAccess);

            User user = userService.checkAndGetUserById(userId);
            User caller = userService.getUserByScopeAccess(callerScopeAccess);

            if (isUserAdmin(caller) || isDefaultUser(caller)) {
                authorizationService.verifySelf(caller, user);
            }
            CredentialListType creds = objFactories.getOpenStackIdentityV2Factory().createCredentialListType();

            if (authorizationService.authorizeCloudServiceAdmin(callerScopeAccess)) {
                if (!StringUtils.isBlank(user.getPassword())) {
                    PasswordCredentialsRequiredUsername userCreds = new PasswordCredentialsRequiredUsername();
                    userCreds.setPassword(user.getPassword());
                    userCreds.setUsername(user.getUsername());
                    creds.getCredential().add(objFactories.getOpenStackIdentityV2Factory().createPasswordCredentials(userCreds));
                }
            }

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
                    baseUrls.add(this.endpointService.getBaseUrlById(baseUrlId));
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

            List<CloudBaseUrl> baseUrls = null;

            if (StringUtils.isBlank(serviceId)) {
                baseUrls = this.endpointService.getBaseUrls();
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
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String serviceId, String marker, String limit) {
        try {
            authorizationService.verifyUserAdminLevelAccess(getScopeAccessForValidToken(authToken));

            int offset = validateOffset(marker);
            int resultSize = validateLimit(limit);

            PaginatorContext<ClientRole> context;
            User caller = userService.getUserByAuthToken(authToken);
            ClientRole userIdentityRole = applicationService.getUserIdentityRole(caller, getCloudAuthClientId(), getIdentityRoleNames());

            if (StringUtils.isBlank(serviceId)) {
                context = this.applicationService.getAvailableClientRolesPaged(offset, resultSize, userIdentityRole.getRsWeight());
            } else {
                context = this.applicationService.getAvailableClientRolesPaged(serviceId, offset, resultSize, userIdentityRole.getRsWeight());
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
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) {

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

            List<TenantRole> roles = this.tenantService.getTenantRolesForUserOnTenant(user, tenant);

            return Response.ok(
                    objFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }

    }

    // KSADM Extension Role Methods

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) {

        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

            List<Application> clients = this.applicationService.getOpenStackServices();

            return Response.ok(
                    objFactories.getOpenStackIdentityExtKsadmnV1Factory().createServices(serviceConverterCloudV20.toServiceList(clients)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) {
        try {
            ScopeAccess access = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserLevelAccess(access);
            User user = userService.getUserByScopeAccess(access);

            List<Tenant> tenants = this.tenantService.getTenantsForUserByTenantRoles(user);

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
            if (!authorizationService.authorizeCloudServiceAdmin(callersScopeAccess)
                    && !authorizationService.authorizeCloudIdentityAdmin(callersScopeAccess)) {
                //is either a user-admin or default user
                if (authorizationService.authorizeCloudUser(callersScopeAccess)) {
                    authorizationService.verifySelf(caller, user);
                } else {
                    authorizationService.verifyDomain(caller, user);
                }
            }
            List<TenantRole> roles = tenantService.getGlobalRolesForUser(user);
            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());
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
    public ResponseBuilder listGroups(HttpHeaders httpHeaders, String authToken, String groupName, String marker, Integer limit) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            List<Group> groups = groupService.getGroups(marker, limit);

            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups cloudGroups = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups();

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
            authorizationService.verifyRackerOrIdentityAdminAccess(getScopeAccessForValidToken(authToken));
            validator20.validateImpersonationRequest(impersonationRequest);

            String impersonatingToken = "";
            String impersonatingUsername = impersonationRequest.getUser().getUsername();

            if (StringUtils.isBlank(impersonatingUsername)) {
                throw new BadRequestException("Username for user to be impersonated must be provided");
            }

            User user = userService.getUser(impersonatingUsername);
            if (user == null && isCloudAuthRoutingEnabled()) {
                logger.info("Impersonation call - calling cloud auth to get user");
                // Get from cloud.
                impersonatingToken = delegateCloud20Service.impersonateUser(impersonatingUsername, config.getString("ga.username"), config.getString("ga.password"));
            } else {
                if(user == null){
                    throw new NotFoundException(String.format("User %s not found", impersonatingUsername));
                }
                if (!isValidImpersonatee(user)) {
                    throw new BadRequestException("User cannot be impersonated; No valid impersonation roles assigned");
                }

                UserScopeAccess impAccess = (UserScopeAccess) scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(user.getUniqueId(), getCloudAuthClientId());

                if (impAccess.isAccessTokenExpired(new DateTime())) {
                    UserScopeAccess scopeAccess;
                    if (!user.isEnabled()) {
                        logger.info("Impersonating a disabled user");
                        scopeAccess = scopeAccessService.updateExpiredUserScopeAccess(impAccess, true); // only set token for hour
                    } else {
                        scopeAccess = scopeAccessService.updateExpiredUserScopeAccess(impAccess, false); // set for full default 24
                    }
                    impersonatingToken = scopeAccess.getAccessTokenString();
                } else {
                    impersonatingToken = impAccess.getAccessTokenString();
                }
            }

            if (StringUtils.isBlank(impersonatingToken) || StringUtils.isBlank(impersonatingUsername)) {
                throw new BadRequestException("Invalid user");
            }

            ScopeAccess sa = checkAndGetToken(authToken);
            ScopeAccess usa;
            //impersonator is a service user
            if (sa instanceof UserScopeAccess) {
                UserScopeAccess userSa = (UserScopeAccess) sa;
                User impersonator = this.userService.getUserById(userSa.getUserRsId());
                usa = scopeAccessService.addImpersonatedScopeAccess(impersonator, getCloudAuthClientId(), impersonatingToken, impersonationRequest);
            }
            //impersonator is a Racker
            else if (sa instanceof RackerScopeAccess) {
                RackerScopeAccess rackerSa = (RackerScopeAccess) sa;
                Racker racker = this.userService.getRackerByRackerId(rackerSa.getRackerId());
                usa = scopeAccessService.addImpersonatedScopeAccess(racker, getCloudAuthClientId(), impersonatingToken, impersonationRequest);
            } else {
                throw new NotAuthorizedException("User does not have access");
            }

            ImpersonationResponse auth = authConverterCloudV20.toImpersonationResponse(usa);
            return Response.ok(objFactories.getRackspaceIdentityExtRaxgaV1Factory().createAccess(auth).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listDefaultRegionServices(String authToken) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        List<Application> openStackServices = applicationService.getOpenStackServices();
        DefaultRegionServices defaultRegionServices = raxAuthObjectFactory.createDefaultRegionServices();
        if (openStackServices != null) {
            for (Application application : openStackServices) {
                Boolean useForDefaultRegion = application.getUseForDefaultRegion();
                if (useForDefaultRegion != null && useForDefaultRegion) {
                    defaultRegionServices.getServiceName().add(application.getName());
                }
            }
        }
        return Response.ok(defaultRegionServices);
    }

    @Override
    public ResponseBuilder setDefaultRegionServices(String authToken, DefaultRegionServices defaultRegionServices) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        List<String> serviceNames = defaultRegionServices.getServiceName();
        List<Application> openStackServices = applicationService.getOpenStackServices();

        for (String serviceName : serviceNames) {
            boolean found = false;
            for (Application application : openStackServices) {
                if (serviceName.equals(application.getName())) {
                    found = true;
                }
            }
            if (!found) {
                throw new BadRequestException("Service " + serviceName + " does not exist.");
            }

        }
        for (Application application : openStackServices) {
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
            Domain savedDomain = this.domainConverterCloudV20.toDomainDO(domain);
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

        domainDO.setDescription(domain.getDescription());
        domainDO.setName(domain.getName());
        domainDO.setEnabled(domain.isEnabled());
        domainDO.setName(domain.getName());
        this.domainService.updateDomain(domainDO);
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
            Users users = domainService.getUsersByDomainId(domainId);
            if (!users.getUsers().isEmpty()) {
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
    public ResponseBuilder getUsersByDomainId(String authToken, String domainId, String enabled) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        domainService.checkAndGetDomain(domainId);
        Users users;
        if (enabled == null) {
            users = domainService.getUsersByDomainId(domainId);
        } else {
            users = domainService.getUsersByDomainId(domainId, Boolean.valueOf(enabled));
        }

        return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(users.getUsers())).getValue());
    }

    @Override
    public ResponseBuilder addUserToDomain(String authToken, String domainId, String userId) throws IOException, JAXBException {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
        Domain domain = domainService.checkAndGetDomain(domainId);
        if (!domain.isEnabled()) {
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
        this.userService.updateUser(userDO, false);
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
        com.rackspace.idm.domain.entity.Policies savedPolicies = policyService.getPolicies(cloudBaseUrl.getPolicyList());

        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
        Policies policies = policiesConverterCloudV20.toPolicies(savedPolicies);
        return Response.ok().entity(objectFactory.createPolicies(policies).getValue());
    }

    @Override
    public ResponseBuilder updatePoliciesForEndpointTemplate(String authToken, String endpointTemplateId, Policies policies) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        CloudBaseUrl cloudBaseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);
        cloudBaseUrl.getPolicyList().clear();

        for (Policy policy : policies.getPolicy()) {
            String policyId = policy.getId();
            this.policyService.getPolicy(policyId);
            cloudBaseUrl.getPolicyList().add(policyId);
        }

        endpointService.updateBaseUrl(cloudBaseUrl);

        return Response.noContent();
    }

    @Override
    public ResponseBuilder addPolicyToEndpointTemplate(String authToken, String endpointTemplateId, String policyId) {
        authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));

        CloudBaseUrl cloudBaseUrl = endpointService.checkAndGetEndpointTemplate(endpointTemplateId);
        com.rackspace.idm.domain.entity.Policy policyEntity = this.policyService.getPolicy(policyId);

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
    public ResponseBuilder getAccessibleDomains(UriInfo uriInfo, String authToken, String marker, String limit) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            int offset = validateOffset(marker);
            int limitAsInt = validateLimit(limit);
            if (this.authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken) ||
                    this.authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken)) {
                PaginatorContext<Domain> domainContext = this.domainService.getDomains(offset, limitAsInt);
                String linkHeader = this.domainPaginator.createLinkHeader(uriInfo, domainContext);

                Domains domains = new Domains();
                domains.getDomain().addAll(domainContext.getValueList());
                com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains domainsObj = domainsConverterCloudV20.toDomains(domains);

                return Response.status(200).header("Link", linkHeader).entity(raxAuthObjectFactory.createDomains(domainsObj).getValue());

            } else {
                User user = userService.getUserByScopeAccess(scopeAccessByAccessToken);
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

            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains domainsObj = domainsConverterCloudV20.toDomains(domains);

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
            List<com.rackspace.idm.domain.entity.Capability> capabilitiesDO = capabilityService.getCapabilities(type, version);
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

    public ResponseBuilder listUsersWithRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String roleId, String marker, String limit) {
        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            ClientRole role = this.applicationService.getClientRoleById(roleId);

            if (role == null) {
                throw new NotFoundException(String.format("Role with id: %s not found", roleId));
            }

            FilterParam[] filters;
            boolean callerIsUserAdmin = authorizationService.authorizeCloudUserAdmin(scopeAccess);

            if (callerIsUserAdmin) {
                User caller = this.userService.getUserByScopeAccess(scopeAccess);
                if (caller.getDomainId() == null || StringUtils.isBlank(caller.getDomainId())) {
                    throw new BadRequestException("User-admin has no domain");
                }
                filters = setFilters(role.getId(), caller.getDomainId());
            } else {
                filters = setFilters(role.getId(), null);
            }

            int iMarker = validateOffset(marker);
            int iLimit = validateLimit(limit);

            PaginatorContext<User> userContext = this.userService.getUsersWithRole(filters, roleId, iMarker, iLimit);

            String linkHeader = this.userPaginator.createLinkHeader(uriInfo, userContext);

            return Response.status(200).header("Link", linkHeader).entity(objFactories.getOpenStackIdentityV2Factory()
                    .createUsers(this.userConverterCloudV20.toUserList(userContext.getValueList())).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    protected FilterParam[] setFilters(String roleId, String domainId) {
        if (domainId == null) {
            return new FilterParam[]{new FilterParam(FilterParamName.ROLE_ID, roleId)};
        }
        return new FilterParam[]{new FilterParam(FilterParamName.DOMAIN_ID, domainId),
                                    new FilterParam(FilterParamName.ROLE_ID, roleId)};
    }

    protected int validateOffset(String offsetString) {
        if (offsetString == null) {
            return 0;
        }
        try {
            if (!StringUtils.isEmpty(offsetString)) {
                int offset = Integer.parseInt(offsetString);
                if (offset < 0) {
                    throw new BadRequestException("Marker must be non negative");
                }
                return offset;
            } else {
                throw new BadRequestException("Marker cannot be blank if parameter is specified");
            }
        } catch (Exception ex) {
            throw new BadRequestException("Marker must be an integer");
        }
    }

    protected int validateLimit(String limitString) {
        if (limitString == null) {
            return config.getInt("ldap.paging.limit.default");
        }
        try {
            if (StringUtils.isBlank(limitString)) {
                throw new BadRequestException("Limit cannot be blank if parameter is specified");
            }

            int limit = Integer.parseInt(limitString);
            if (limit < 0) {
                throw new BadRequestException("Limit must be non negative");
            } else if (limit == 0) {
                return config.getInt("ldap.paging.limit.default");
            } else if (limit >= config.getInt("ldap.paging.limit.max")) {
                return config.getInt("ldap.paging.limit.max");
            } else {
                return limit;
            }
        } catch (Exception ex) {
            throw new BadRequestException("Limit must be an integer");
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
        List<com.rackspace.idm.domain.entity.Region> regions = this.cloudRegionService.getRegions(config.getString("cloud.region"));
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
            admins = domainService.getDomainAdmins(user.getDomainId(), true);
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
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
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
            List<Group> groups = groupService.getGroupsForUser(userId);
            if (groups.size() == 0) {
                Group defGroup = groupService.getGroupById(config.getInt("defaultGroupId"));
                groups.add(defGroup);
            }
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups cloudGroups = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups();
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
            validator20.validateGroupId(groupId);
            Group group = groupService.getGroupById(Integer.parseInt(groupId));
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
            validator20.validateGroupId(groupId);

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
            validator20.validateGroupId(groupId);
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
            authorizationService.verifyIdentityAdminLevelAccess(scopeAccess);
            validator20.validateGroupId(groupId);
            Group group = groupService.checkAndGetGroupById(Integer.parseInt(groupId));

            User user = userService.checkAndGetUserById(userId);
            boolean isDefaultUser = authorizationService.hasDefaultUserRole(user);
            boolean isUserAdmin = authorizationService.hasUserAdminRole(user);

            if (isDefaultUser) {
                throw new BadRequestException("Cannot add Sub-Users directly to a Group, must assign their Parent User.");
            } else if (isUserAdmin) {
                List<User> subUsers = userService.getSubUsers(user);

                for (User subUser : subUsers) {
                    groupService.addGroupToUser(Integer.parseInt(groupId), subUser.getId());
                }
            }

            groupService.addGroupToUser(Integer.parseInt(groupId), userId);
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
            validator20.validateGroupId(groupId);
            Group group = groupService.checkAndGetGroupById(Integer.parseInt(groupId));

            if (userId == null || userId.trim().isEmpty()) {
                throw new BadRequestException("Invalid user id");
            }

            User user = userService.checkAndGetUserById(userId);
            boolean isDefaultUser = authorizationService.hasDefaultUserRole(user);
            boolean isUserAdmin = authorizationService.hasUserAdminRole(user);

            if (isDefaultUser) {
                throw new BadRequestException("Cannot remove Sub-Users directly from a Group, must remove their Parent User.");
            }

            if (!groupService.isUserInGroup(userId, group.getGroupId())) {
                throw new NotFoundException("Group '" + group.getName() + "' is not assigned to user.");
            }

            if (isUserAdmin) {
                List<User> subUsers = userService.getSubUsers(user);

                for (User subUser : subUsers) {
                    groupService.deleteGroupFromUser(Integer.parseInt(groupId), subUser.getId());
                }
            }
            groupService.deleteGroupFromUser(Integer.parseInt(groupId), userId);
            return Response.noContent();
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder getUsersForGroup(HttpHeaders httpHeaders, String authToken, String groupId, String marker, String limit)  {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            validator20.validateGroupId(groupId);
            FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParamName.GROUP_ID, groupId)};
            String iMarker = validateMarker(marker);
            int iLimit = validateLimit(limit);
            groupService.checkAndGetGroupById(Integer.parseInt(groupId));
            Users users = groupService.getAllEnabledUsers(filters, iMarker, iLimit);

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(users.getUsers())).getValue());
        } catch (Exception e) {
            return exceptionHandler.exceptionResponse(e);
        }
    }

    String validateMarker(String marker) {
        String iMarker = "0";
        try {
            if (!StringUtils.isEmpty(marker)) {
                if (Integer.parseInt(marker) < 0) {
                    throw new BadRequestException("Marker must be non negative");
                }
                iMarker = marker;
            }
        } catch (Exception ex) {
            throw new BadRequestException("Marker must be a number");
        }
        return iMarker;
    }

    // KSADM Extension User methods
    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, String marker, String limit) {
        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            User caller = getUser(scopeAccessByAccessToken);

            //if default user
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken)) {
                List<User> users = new ArrayList<User>();
                users.add(caller);
                return Response.ok(objFactories.getOpenStackIdentityV2Factory()
                        .createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
            }
            authorizationService.verifyUserAdminLevelAccess(scopeAccessByAccessToken);

            int offset = validateOffset(marker);
            int limitAsInt = validateLimit(limit);

            PaginatorContext<User> userContext;
            if (authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken) ||
                    authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken)) {
                userContext = this.userService.getAllUsersPaged(null, offset, limitAsInt);
            } else {
                if (caller.getDomainId() != null) {
                    String domainId = caller.getDomainId();
                    FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParamName.DOMAIN_ID, domainId)};
                    userContext = this.userService.getAllUsersPaged(filters, offset, limitAsInt);
                } else {
                    throw new BadRequestException("User-admin has no domain");
                }
            }

            String linkHeader = userPaginator.createLinkHeader(uriInfo, userContext);

            return Response.status(200)
                    .header("Link", linkHeader)
                    .entity(objFactories.getOpenStackIdentityV2Factory()
                            .createUsers(this.userConverterCloudV20.toUserList(userContext.getValueList())).getValue());
        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) {

        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);

            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            List<User> users = this.tenantService.getUsersForTenant(tenant.getTenantId());

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createUsers(userConverterCloudV20.toUserList(users)).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                      String roleId, String marker, Integer limit) {

        try {
            ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
            authorizationService.verifyUserAdminLevelAccess(scopeAccess);
            authorizationService.verifyTokenHasTenantAccess(tenantId, scopeAccess);

            Tenant tenant = tenantService.checkAndGetTenant(tenantId);

            ClientRole role = checkAndGetClientRole(roleId);

            List<User> users = this.tenantService.getUsersWithTenantRole(tenant, role);

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

            userDO.setEnabled(user.isEnabled());

            if (userDO.isDisabled()) {
                atomHopperClient.asyncPost(userDO, AtomHopperConstants.DISABLED);
            }

            this.userService.updateUser(userDO, false);

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

            List<com.rackspace.idm.domain.entity.Question> questions = questionService.getQuestions();
            for(com.rackspace.idm.domain.entity.Question question : questions){
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

            this.userService.updateUser(user, false);

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
            this.userService.updateUser(user, false);

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
            this.userService.updateUser(credUser, false);

            return Response.ok(objFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(creds).getValue());

        } catch (Exception ex) {
            return exceptionHandler.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateUserPasswordCredentials(HttpHeaders httpHeaders, String authToken, String userId,
                                                         String credentialType, PasswordCredentialsRequiredUsername creds) {

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

            user.setPassword(creds.getPassword());
            this.userService.updateUser(user, false);

            return Response.ok(objFactories.getOpenStackIdentityV2Factory().createPasswordCredentials(creds).getValue());

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

            if (((HasAccessToken) sa).isAccessTokenExpired(new DateTime())) {
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

                access.setUser(userConverterCloudV20.toUserForAuthenticateResponse(racker, roleList));
            } else if (sa instanceof UserScopeAccess || sa instanceof ImpersonatedScopeAccess) {
                User impersonator;
                User user;
                List<TenantRole> roles;
                if (sa instanceof UserScopeAccess) {
                    UserScopeAccess usa = (UserScopeAccess) sa;
                    user = userService.getUserByScopeAccess(usa);
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
                    UserForAuthenticateResponse userForAuthenticateResponse = userConverterCloudV20.toUserForAuthenticateResponse(impersonator, impRoles);
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
        User user = userService.getUserByScopeAccess(scopeAccessByAccessToken);
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
        Policies policies = policiesConverterCloudV20.toPolicies(savedPolicies);
        return Response.ok().entity(objectFactory.createPolicies(policies).getValue());
    }

    @Override
    public ResponseBuilder addPolicy(UriInfo uriInfo, String authToken, Policy policy) {
        try {
            authorizationService.verifyIdentityAdminLevelAccess(getScopeAccessForValidToken(authToken));
            com.rackspace.idm.domain.entity.Policy savedPolicy = this.policyConverterCloudV20.toPolicyDO(policy);
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
            com.rackspace.idm.domain.entity.Policy policyEnt = this.policyService.getPolicy(policyId);
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
            policyService.checkAndGetPolicy(policyId);
            policyValidator.validatePolicyName(policy.getName());
            com.rackspace.idm.domain.entity.Policy updatePolicy = this.policyConverterCloudV20.toPolicyDO(policy);
            this.policyService.updatePolicy(updatePolicy, policyId);
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

    void checkForMultipleIdentityRoles(User user, ClientRole roleToAdd) {
        user.setRoles(tenantService.getGlobalRolesForUser(user));
        if (user.getRoles() == null || roleToAdd == null || !StringUtils.startsWithIgnoreCase(roleToAdd.getName(), "identity:")) {
            return;
        }

        for (TenantRole userRole : user.getRoles()) {
            if (StringUtils.startsWithIgnoreCase(userRole.getName(), "identity:")) {
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

    JAXBElement<? extends CredentialType> getJSONCredentials(String jsonBody) {

        JAXBElement<? extends CredentialType> jaxbCreds = null;

        CredentialType creds = JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString(jsonBody);

        if (creds instanceof ApiKeyCredentials) {
            jaxbCreds = objFactories.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials((ApiKeyCredentials) creds);
        }
        if (creds instanceof PasswordCredentialsRequiredUsername) {
            jaxbCreds = objFactories.getOpenStackIdentityV2Factory().createPasswordCredentials((PasswordCredentialsRequiredUsername) creds);
        }

        return jaxbCreds;
    }

    @SuppressWarnings("unchecked")
    JAXBElement<? extends CredentialType> getXMLCredentials(String body) {
        JAXBElement<? extends CredentialType> cred = null;
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
                    com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
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

    private AuthenticateResponse removeServiceIdFromAuthResponse(AuthenticateResponse auth) {
        if (auth.getUser() != null && auth.getUser().getRoles() != null) {
            for (Role r : auth.getUser().getRoles().getRole()) {
                r.setServiceId(null);
            }
        }
        return auth;
    }

    private List<String> getIdentityRoleNames() {
        List<String> names = new ArrayList<String>();
        names.add(config.getString("cloudAuth.userRole"));
        names.add(config.getString("cloudAuth.userAdminRole"));
        names.add(config.getString("cloudAuth.adminRole"));
        names.add(config.getString("cloudAuth.serviceAdminRole"));
        return names;
    }

    boolean isCloudAuthRoutingEnabled() {
        return config.getBoolean(CLOUD_AUTH_ROUTING);
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

    public void setDelegateCloud20Service(DelegateCloud20Service delegateCloud20Service) {
        this.delegateCloud20Service = delegateCloud20Service;
    }

    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
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

