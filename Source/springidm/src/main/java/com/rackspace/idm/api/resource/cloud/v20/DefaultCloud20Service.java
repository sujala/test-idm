package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationRequest;
import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationResponse;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:14 PM
 */
@Component
public class DefaultCloud20Service implements Cloud20Service {

    @Autowired
    private AuthConverterCloudV20 authConverterCloudV20;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ApplicationService clientService;

    @Autowired
    private Configuration config;

    @Autowired
    private EndpointConverterCloudV20 endpointConverterCloudV20;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

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
    private GroupService cloudGroupService;

    @Autowired
    private UserService userService;

    @Autowired
    private CloudGroupBuilder cloudGroupBuilder;

    @Autowired
    private CloudKsGroupBuilder cloudKsGroupBuilder;

    @Autowired
    private DelegateCloud20Service delegateCloud20Service;

    private HashMap<String, JAXBElement<Extension>> extensionMap;

    private JAXBElement<Extensions> currentExtensions;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, EndpointTemplate endpoint) {
        try {
            verifyServiceAdminLevelAccess(authToken);
            Tenant tenant = checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpoint.getId());
            if (baseUrl.getGlobal()) {
                throw new BadRequestException("Cannot add a global endpoint to this tenant.");
            }
            tenant.addBaseUrlId(String.valueOf(endpoint.getId()));
            this.tenantService.updateTenant(tenant);
            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoint(endpointConverterCloudV20.toEndpoint(baseUrl)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, EndpointTemplate endpoint) {
        try {
            verifyServiceAdminLevelAccess(authToken);
            CloudBaseUrl baseUrl = this.endpointConverterCloudV20.toCloudBaseUrl(endpoint);
            this.endpointService.addBaseUrl(baseUrl);
            return Response.created(uriInfo.getRequestUriBuilder().path(String.valueOf(baseUrl.getBaseUrlId())).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityExtKscatalogV1Factory()
                            .createEndpointTemplate(this.endpointConverterCloudV20.toEndpointTemplate(baseUrl)).getValue());
        } catch (BaseUrlConflictException buce) {
            return endpointTemplateConflictException(buce.getMessage());
        } catch (DuplicateException dex) {
            return endpointTemplateConflictException(dex.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Role role) {

        try {
            verifyServiceAdminLevelAccess(authToken);
            if (role == null) {
                String errMsg = "role cannot be null";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(role.getServiceId())) {
                String errMsg = "Expecting serviceId";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }


            if (StringUtils.isBlank(role.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }


            Application service = checkAndGetApplication(role.getServiceId());

            ClientRole clientRole = new ClientRole();
            clientRole.setClientId(service.getClientId());
            clientRole.setDescription(role.getDescription());
            clientRole.setName(role.getName());

            clientService.addClientRole(clientRole);

            return Response.created(uriInfo.getRequestUriBuilder().path(clientRole.getId()).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole(roleConverterCloudV20.toRoleFromClientRole(clientRole)).getValue());

        } catch (DuplicateException bre) {
            return roleConflictExceptionResponse(bre.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String userId, String roleId) {

        try {
            verifyUserAdminLevelAccess(authToken);
            verifyTokenHasTenantAccess(authToken, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            User user = checkAndGetUser(userId);

            ClientRole role = checkAndGetClientRole(roleId);

            TenantRole tenantrole = new TenantRole();
            tenantrole.setName(role.getName());
            tenantrole.setClientId(role.getClientId());
            tenantrole.setRoleRsId(role.getId());
            tenantrole.setUserId(user.getId());
            tenantrole.setTenantIds(new String[]{tenant.getTenantId()});

            this.tenantService.addTenantRoleToUser(user, tenantrole);

            return Response.ok();

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, Service service) {
        try {
            checkXAUTHTOKEN(authToken, true, null);
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

            this.clientService.add(client);
            service.setId(client.getClientId());
            return Response.created(
                    uriInfo.getRequestUriBuilder().path(service.getId()).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createService(service).getValue());

        } catch (DuplicateException de) {
            return serviceConflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken,
                                     org.openstack.docs.identity.api.v2.Tenant tenant) {

        try {
            verifyServiceAdminLevelAccess(authToken);

            if (StringUtils.isBlank(tenant.getName())) {
                String errMsg = "Expecting name";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            // Our implmentation has the id and the name the same
            tenant.setId(tenant.getName());
            Tenant savedTenant = this.tenantConverterCloudV20.toTenantDO(tenant);

            this.tenantService.addTenant(savedTenant);

            return Response.created(uriInfo.getRequestUriBuilder().path(savedTenant.getTenantId()).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                            .createTenant(this.tenantConverterCloudV20.toTenant(savedTenant)).getValue());

        } catch (DuplicateException de) {
            return tenantConflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken, UserForCreate user) {
        try {
            verifyUserAdminLevelAccess(authToken);
            validateUser(user);
            ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
            if (user.getPassword() != null) {
                validatePassword(user.getPassword());
            }
            User userDO = this.userConverterCloudV20.toUserDO(user);

            //if caller is a user-admin, give user same mosso and nastId and verifies that it has less then 100 subusers
            if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
                //TODO Pagination index and offset
                Users users;
                User caller = userService.getUserByAuthToken(authToken);
                String domainId = caller.getDomainId();
                FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParamName.DOMAIN_ID, domainId)};
                users = this.userService.getAllUsers(filters);
                int numSubUsers =  config.getInt("numberOfSubUsers");
                if(users.getUsers().size() > numSubUsers){
                    String errMsg = String.format("User cannot create more than %d sub-accounts." ,numSubUsers );
                    throw new BadRequestException(errMsg);
                }
                userDO.setMossoId(caller.getMossoId());
                userDO.setNastId(caller.getNastId());
            }
            setDomainId(scopeAccessByAccessToken, userDO);
            userService.addUser(userDO);
            assignProperRole(httpHeaders, authToken, scopeAccessByAccessToken, userDO);

            return Response.created(uriInfo.getRequestUriBuilder().path(userDO.getId()).build())
                    .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(userDO)).getValue());
        } catch (DuplicateException de) {
            return userConflictExceptionResponse(de.getMessage());
        } catch (DuplicateUsernameException due) {
            return userConflictExceptionResponse(due.getMessage());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    void assignProperRole(HttpHeaders httpHeaders, String authToken, ScopeAccess scopeAccessByAccessToken, User userDO) {
        //If caller is an identity admin, give user service-admin role
        if (authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken)) {
            ClientRole roleId = clientService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthServiceAdminRole());
            this.addUserRole(httpHeaders, authToken, userDO.getId(), roleId.getId());
        }
        //if caller is a service admin, give user user-admin role
        if (authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken)) {
            ClientRole roleId = clientService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserAdminRole());
            this.addUserRole(httpHeaders, authToken, userDO.getId(), roleId.getId());
        }
        //if caller is a user admin, give user default role
        if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
            ClientRole roleId = clientService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserRole());
            this.addUserRole(httpHeaders, authToken, userDO.getId(), roleId.getId());
        }

    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String authToken, String userId, UserForCreate user) throws IOException {
        try {
            verifyUserLevelAccess(authToken);
            if (user.getPassword() != null) {
                validatePassword(user.getPassword());
            }
            User retrievedUser = checkAndGetUser(userId);
            ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
            //if caller is default user, usedId must match callers user id
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken)) {
                User caller = userService.getUserByAuthToken(authToken);
                if (!caller.getId().equals(retrievedUser.getId())) {
                    throw new ForbiddenException("Not authorized.");
                }
            }
            //if user admin, verify domain
            if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
                User caller = userService.getUserByAuthToken(authToken);
                verifyDomain(retrievedUser, caller);
            }
            User userDO = this.userConverterCloudV20.toUserDO(user);
            if (userDO.isDisabled()) {
                this.scopeAccessService.expireAllTokensForUser(retrievedUser.getUsername());
            }
            retrievedUser.copyChanges(userDO);
            userService.updateUserById(retrievedUser, false);
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(retrievedUser)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    User getUser(ScopeAccess scopeAccessByAccessToken) {
        String uid = scopeAccessByAccessToken.getLDAPEntry().getAttributeValue(LdapRepository.ATTR_UID);
        return userService.getUser(uid);
    }

    void setDomainId(ScopeAccess scopeAccessByAccessToken, User userDO) {
        if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
            User caller = getUser(scopeAccessByAccessToken);
            //is userAdmin
            userDO.setDomainId(caller.getDomainId());
        }
    }

    void validateUser(org.openstack.docs.identity.api.v2.User user) {
        String username = user.getUsername();
        validateUsername(username);
        String email = user.getEmail();
        if (StringUtils.isBlank(email)) {
            String errorMsg = "Expecting valid email address";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        if (!email.matches("[a-zA-Z0-9_\\.\"]+@[a-zA-Z0-9_\\.]+\\.[a-zA-Z]+")) {
            String errorMsg = "Expecting valid email address";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }

    }

    void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            String errorMsg = "Expecting username";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        if (username.contains(" ")) {
            String errorMsg = "Username should not contain white spaces";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }
    }

    void validatePasswordCredentials(PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername) {
        String username = passwordCredentialsRequiredUsername.getUsername();
        String password = passwordCredentialsRequiredUsername.getPassword();
        validateUsername(username);
        if (StringUtils.isBlank(password)) {
            String errMsg = "Expecting password";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    void validatePassword(String password) {
        String errMsg = "Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter and one numeric character.";
        if (password.length() < 8) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[A-Z].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[a-z].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (!password.matches(".*[0-9].*")) {
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    void validateApiKeyCredentials(ApiKeyCredentials apiKeyCredentials) {
        String username = apiKeyCredentials.getUsername();
        String apiKey = apiKeyCredentials.getApiKey();
        validateUsername(username);
        if (StringUtils.isBlank(apiKey)) {
            String errMsg = "Expecting apiKey";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String body) throws IOException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            JAXBElement<? extends CredentialType> credentials;

            if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                credentials = getXMLCredentials(body);
            } else {
                credentials = getJSONCredentials(body);
            }

            User user;

            if (credentials.getDeclaredType().isAssignableFrom(PasswordCredentialsRequiredUsername.class)) {
                PasswordCredentialsRequiredUsername userCredentials = (PasswordCredentialsRequiredUsername) credentials.getValue();
                validatePasswordCredentials(userCredentials);
                validatePassword(userCredentials.getPassword());
                user = checkAndGetUser(userId);
                if (!userCredentials.getUsername().equals(user.getUsername())) {
                    String errMsg = "User and UserId mis-matched";
                    logger.warn(errMsg);
                    throw new BadRequestException(errMsg);
                }
                user.setPassword(userCredentials.getPassword());
                userService.updateUser(user, false);
            } else if (credentials.getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                ApiKeyCredentials userCredentials = (ApiKeyCredentials) credentials.getValue();
                validateApiKeyCredentials(userCredentials);
                user = checkAndGetUser(userId);
                if (!userCredentials.getUsername().equals(user.getUsername())) {
                    String errMsg = "User and UserId mis-matched";
                    logger.warn(errMsg);
                    throw new BadRequestException(errMsg);
                }
                user.setApiKey(userCredentials.getApiKey());
                userService.updateUser(user, false);
            }
            return Response.ok(credentials.getValue()).status(Status.CREATED);
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {

        try {
            verifyUserAdminLevelAccess(authToken);
            User user = checkAndGetUser(userId);
            ClientRole cRole = checkAndGetClientRole(roleId);
            ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
            if (!authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken)
                    && config.getString("cloudAuth.adminRole").equals(cRole.getName())) {
                throw new ForbiddenException("Not authorized.");
            }
            TenantRole role = new TenantRole();
            role.setClientId(cRole.getClientId());
            role.setName(cRole.getName());
            role.setRoleRsId(cRole.getId());
            this.tenantService.addTenantRoleToUser(user, role);
            return Response.ok();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    // Core Service Methods

    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, AuthenticationRequest authenticationRequest) throws IOException {
        try {
            User user = null;
            UserScopeAccess usa = null;

//            if(authenticationRequest.getCredential() == null && authenticationRequest.getToken() == null)
//                throw new BadRequestException("Unable to parse Auth data. Please review XML or JSON formatting.");

            if (authenticationRequest.getToken() != null && !StringUtils.isBlank(authenticationRequest.getToken().getId())) {
                ScopeAccess sa = scopeAccessService.getScopeAccessByAccessToken(authenticationRequest.getToken().getId());
                if (sa == null || ((HasAccessToken) sa).isAccessTokenExpired(new DateTime()) || !(sa instanceof UserScopeAccess)) {
                    String errMsg = "Token not authenticated";
                    logger.warn(errMsg);
                    throw new NotAuthenticatedException(errMsg);
                }
                usa = (UserScopeAccess) sa;
                user = this.checkAndGetUser(usa.getUserRsId());
            } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(PasswordCredentialsRequiredUsername.class)) {
                PasswordCredentialsRequiredUsername creds = (PasswordCredentialsRequiredUsername) authenticationRequest.getCredential().getValue();
                validatePasswordCredentials(creds);
                String username = creds.getUsername();
                String password = creds.getPassword();
                user = checkAndGetUserByName(username);
                usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(username, password, getCloudAuthClientId());

            } else if (authenticationRequest.getCredential().getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                ApiKeyCredentials creds = (ApiKeyCredentials) authenticationRequest.getCredential().getValue();
                validateApiKeyCredentials(creds);
                String username = creds.getUsername();
                String key = creds.getApiKey();
                user = checkAndGetUserByName(username);
                usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(username, key, getCloudAuthClientId());
            }
            List<TenantRole> roles = tenantService.getTenantRolesForScopeAccess(usa);
            if (authenticationRequest.getTenantName()!=null && !tenantService.hasTenantAccess(usa,authenticationRequest.getTenantName())) {
                String errMsg = "Token doesn't belong to Tenant with Id/Name: '"+ authenticationRequest.getTenantName() +"'";
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }
            if (authenticationRequest.getTenantId()!=null && !tenantService.hasTenantAccess(usa,authenticationRequest.getTenantId())) {
                String errMsg = "Token doesn't belong to Tenant with Id/Name: '"+ authenticationRequest.getTenantId() +"'";
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }
            List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(usa);

            // Remove Admin URLs if non admin token
            if (!this.authorizationService.authorizeCloudIdentityAdmin(usa)) {
                stripEndpoints(endpoints);
            }
            AuthenticateResponse auth = authConverterCloudV20.toAuthenticationResponse(user, usa, roles, endpoints);
            //This just verifies that the token has access to the tenant. It does not create a token with ONLY access to a specific tenant.
            String tenantId = authenticationRequest.getTenantId();
            try {

                if (tenantId != null) {
                    if (tenantId.isEmpty()) {
                        throw new BadRequestException("Invalid tenantId, not allowed to be blank.");
                    }
                    verifyTokenHasTenantAccessForAuthenticate(auth.getToken().getId(), tenantId);
                }


            } catch (ForbiddenException ex) {
                String errMsg = String.format("Tenant with Name/Id: '%s', is not valid for User '%s' (id: '%s')", tenantId, user.getUsername(), user.getId());
                logger.warn(errMsg);
                throw new NotAuthorizedException(errMsg);
            }

            // ToDo: removing serviceId from response for now
            if(auth.getUser().getRoles() != null) {
                for(Role r : auth.getUser().getRoles().getRole()) {
                    r.setServiceId(null);
                }
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createAccess(auth).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo)
            throws IOException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            ScopeAccess sa = checkAndGetToken(tokenId);

            if (!StringUtils.isBlank(belongsTo)) {
                UserScopeAccess usa = (UserScopeAccess) sa;
                List<TenantRole> roles = this.tenantService.getTenantRolesForScopeAccess(usa);
                if (!belongsTo(belongsTo, roles)) {
                    throw new NotFoundException();
                }
            }
            return Response.ok();
        } catch (BadRequestException bre) {
            return badRequestExceptionResponse(bre.getMessage());
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
            verifyServiceAdminLevelAccess(authToken);
            Tenant tenant = checkAndGetTenant(tenantId);
            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpointId);
            tenant.removeBaseUrlId(String.valueOf(baseUrl.getBaseUrlId()));
            tenantService.updateTenant(tenant);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId) {
        try {
            verifyServiceAdminLevelAccess(authToken);
            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpointTemplateId);
            this.endpointService.deleteBaseUrl(baseUrl.getBaseUrlId());
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String authToken, String roleId) {
        try {
            verifyIdentityAdminLevelAccess(authToken);
            if (roleId == null) {
                throw new BadRequestException("roleId cannot be null");
            }
            ClientRole role = checkAndGetClientRole(roleId);
            this.clientService.deleteClientRole(role);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                      String userId, String roleId) {
        try {
            verifyUserAdminLevelAccess(authToken);
            verifyTokenHasTenantAccess(authToken, tenantId);
            Tenant tenant = checkAndGetTenant(tenantId);
            User user = checkAndGetUser(userId);
            ClientRole role = checkAndGetClientRole(roleId);
            TenantRole tenantrole = new TenantRole();
            tenantrole.setClientId(role.getClientId());
            tenantrole.setRoleRsId(role.getId());
            tenantrole.setUserId(user.getId());
            tenantrole.setTenantIds(new String[]{tenant.getTenantId()});
            this.tenantService.deleteTenantRole(user.getUniqueId(), tenantrole);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders, String authToken, String serviceId) {
        try {
            verifyServiceAdminLevelAccess(authToken);
            Application client = checkAndGetApplication(serviceId);
            this.clientService.delete(client.getClientId());
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String authToken, String tenantId) {
        try {
            verifyServiceAdminLevelAccess(authToken);
            Tenant tenant = checkAndGetTenant(tenantId);
            tenantService.deleteTenant(tenant.getTenantId());
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        try {
            verifyUserAdminLevelAccess(authToken);
            User user = checkAndGetUser(userId);
            //is same domain?
            if (authorizationService.authorizeCloudUserAdmin(scopeAccessService.getScopeAccessByAccessToken(authToken))) {
                User caller = userService.getUserByAuthToken(authToken);
                verifyDomain(user, caller);
            }

            userService.softDeleteUser(user);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserFromSoftDeleted(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        try {
            checkXAUTHTOKEN(authToken, true, null);
            User user = checkAndGetSoftDeletedUser(userId);
            userService.deleteUser(user);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType)
            throws IOException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            if (credentialType.equals(JSONConstants.PASSWORD_CREDENTIALS)) {
                IdentityFault fault = new IdentityFault();
                fault.setCode(HttpServletResponse.SC_NOT_IMPLEMENTED);
                fault.setMessage("Method not implemented");
                return Response.ok(
                        OBJ_FACTORIES.getOpenStackIdentityV2Factory().createIdentityFault(fault))
                        .status(HttpServletResponse.SC_NOT_IMPLEMENTED);
            }

            if (!credentialType.equals(JSONConstants.APIKEY_CREDENTIALS)) {
                throw new BadRequestException("unsupported credential type");
            }

            User user = checkAndGetUser(userId);

            user.setApiKey("");

            userService.updateUser(user, false);

            return Response.noContent();

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {

        try {
            verifyUserAdminLevelAccess(authToken);

            User user = checkAndGetUser(userId);
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
            ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
            if (!authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken)
                    && config.getString("cloudAuth.adminRole").equals(role.getName())) {
                throw new ForbiddenException("Not authorized.");
            }
            this.tenantService.deleteGlobalRole(role);
            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getEndpoint(HttpHeaders httpHeaders, String authToken, String tenantId, String endpointId) {

        try {
            verifyServiceAdminLevelAccess(authToken);

            Tenant tenant = checkAndGetTenant(tenantId);

            if (!tenant.containsBaseUrlId(endpointId)) {
                String errMsg = String.format("Tenant %s does not have endpoint %s", tenantId, endpointId);
                logger.warn(errMsg);
                return notFoundExceptionResponse(errMsg);
            }

            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpointId);

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoint(endpointConverterCloudV20.toEndpoint(baseUrl)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders, String authToken, String endpointTemplateId) {

        try {
            verifyServiceAdminLevelAccess(authToken);

            CloudBaseUrl baseUrl = checkAndGetEndpointTemplate(endpointTemplateId);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityExtKscatalogV1Factory()
                    .createEndpointTemplate(this.endpointConverterCloudV20.toEndpointTemplate(baseUrl)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) throws IOException {
        if (StringUtils.isBlank(alias)) {
            return badRequestExceptionResponse("Invalid extension alias '" + alias + "'.");
        }

        final String normalizedAlias = alias.trim().toUpperCase();

        if (extensionMap == null) {
            extensionMap = new HashMap<String, JAXBElement<Extension>>();

            try {
                if (currentExtensions == null) {
                    JAXBContext jaxbContext = JAXBContextResolver.get();
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
                    StreamSource ss = new StreamSource(is);
                    currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
                }

                Extensions exts = currentExtensions.getValue();

                for (Extension e : exts.getExtension()) {
                    extensionMap.put(e.getAlias().trim().toUpperCase(), OBJ_FACTORIES.getOpenStackCommonV1Factory().createExtension(e));
                }
            } catch (Exception e) {
                // Return 500 error. Is WEB-IN/extensions.xml malformed?
                return serviceExceptionResponse();
            }
        }

        if (!extensionMap.containsKey(normalizedAlias)) {
            return notFoundExceptionResponse("Extension with alias '" + normalizedAlias + "' is not available.");
        }

        return Response.ok(extensionMap.get(normalizedAlias).getValue());
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken, String roleId) {

        try {
            verifyServiceAdminLevelAccess(authToken);
            ClientRole role = checkAndGetClientRole(roleId);
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createRole(this.roleConverterCloudV20.toRoleFromClientRole(role)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getSecretQA(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {

        try {
            verifyServiceAdminLevelAccess(authToken);
            User user = checkAndGetUser(userId);
            SecretQA secrets = OBJ_FACTORIES.getRackspaceIdentityExtKsqaV1Factory().createSecretQA();

            secrets.setAnswer(user.getSecretAnswer());
            secrets.setQuestion(user.getSecretQuestion());
            secrets.setUsername(user.getUsername());
            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKsqaV1Factory().createSecretQA(secrets).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String authToken, String serviceId) {
        try {
            verifyServiceAdminLevelAccess(authToken);
            Application client = checkAndGetApplication(serviceId);
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createService(serviceConverterCloudV20.toService(client)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders, String authToken, String tenantsId) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            Tenant tenant = checkAndGetTenant(tenantsId);
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenant(tenantConverterCloudV20.toTenant(tenant)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            Tenant tenant = this.tenantService.getTenantByName(name);
            if (tenant == null) {
                String errMsg = String.format("Tenant with id/name: '%s' was not found.", name);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createTenant(this.tenantConverterCloudV20.toTenant(tenant)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {

        try {
            ScopeAccess scopeAccessByAccessToken = getScopeAccessForValidToken(authToken);
            User caller = getUser(scopeAccessByAccessToken);

            //if caller has default user role
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken)) {
                if (caller.getId().equals(userId)) {
                    return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                            .createUser(this.userConverterCloudV20.toUser(caller)).getValue());
                } else {
                    throw new ForbiddenException("Not authorized.");
                }
            }

            verifyUserAdminLevelAccess(authToken);
            User user = this.userService.getUserById(userId);
            if (user == null) {
                String errMsg = String.format("User with id: '%s' was not found", userId);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            if (authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
                verifyDomain(user, caller);
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createUser(this.userConverterCloudV20.toUser(user)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String authToken, String name) throws IOException {

        try {
            verifyUserLevelAccess(authToken);
            //verifyServiceAdminLevelAccess(authToken);

            User user = this.userService.getUser(name);

            if (user == null) {
                String errMsg = String.format("User not found: '%s'", name);
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
            if(isUserAdmin(scopeAccessByAccessToken, null))
            {
                User adminUser = userService.getUserByAuthToken(authToken);
                verifyDomain(user,adminUser);
            }
            else if(isDefaultUser(scopeAccessByAccessToken, null))
            {
                verifySelf(authToken, user);
            }
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUser(userConverterCloudV20.toUser(user)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders, String authToken, String userId, String credentialType)
            throws IOException {

        try {
            verifyUserLevelAccess(authToken);

            if (!(credentialType.equals(JSONConstants.PASSWORD_CREDENTIALS)
                    || credentialType.equals(JSONConstants.APIKEY_CREDENTIALS))) {
                throw new BadRequestException("unsupported credential type");
            }
            User user = this.userService.getUserById(userId);


            ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken) ||
                    authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)) {
                User caller = getUser(scopeAccessByAccessToken);
                if (!caller.getId().equals(userId)) {
                    throw new ForbiddenException("Access denied.");
                }
            }

            if (user == null) {
                String errMsg = "Credential type RAX-KSKEY:apiKeyCredentials was not found for User with Id: " + userId;
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            JAXBElement<? extends CredentialType> creds = null;

            if (credentialType.equals(JSONConstants.PASSWORD_CREDENTIALS)) {
                if (StringUtils.isBlank(user.getPassword())) {
                    throw new NotFoundException("User doesn't have password credentials");
                }
                PasswordCredentialsRequiredUsername userCreds = new PasswordCredentialsRequiredUsername();
                userCreds.setPassword(user.getPassword());
                userCreds.setUsername(user.getUsername());
                creds = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createCredential(userCreds);
            } else if (credentialType.equals(JSONConstants.APIKEY_CREDENTIALS)) {
                if (StringUtils.isBlank(user.getApiKey())) {
                    throw new NotFoundException("User doesn't have api key credentials");
                }
                ApiKeyCredentials userCreds = new ApiKeyCredentials();
                userCreds.setApiKey(user.getApiKey());
                userCreds.setUsername(user.getUsername());
                creds = OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(userCreds);
            }

            return Response.ok(creds.getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String authToken, String userId, String roleId) {

        try {
            verifyServiceAdminLevelAccess(authToken);

            User user = checkAndGetUser(userId);

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

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createRole(this.roleConverterCloudV20.toRole(role)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders, String authToken, String userId, String marker, Integer limit)
            throws Exception {

        try {
            verifyUserLevelAccess(authToken);
            ScopeAccess callersScopeAccess = scopeAccessService.getScopeAccessByAccessToken(authToken);
            User user = checkAndGetUser(userId);

            if(isUserAdmin(callersScopeAccess, null) || isDefaultUser(callersScopeAccess, null))
            {
                verifySelf(authToken, user);
            }
            CredentialListType creds = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createCredentialListType();

            if (!StringUtils.isBlank(user.getPassword())) {
                PasswordCredentialsRequiredUsername userCreds = new PasswordCredentialsRequiredUsername();
                userCreds.setPassword(user.getPassword());
                userCreds.setUsername(user.getUsername());
                creds.getCredential().add(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createPasswordCredentials(userCreds));
            }

            if (!StringUtils.isBlank(user.getApiKey())) {
                ApiKeyCredentials userCreds = new ApiKeyCredentials();
                userCreds.setApiKey(user.getApiKey());
                userCreds.setUsername(user.getUsername());
                creds.getCredential().add(OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(userCreds));
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createCredentials(creds).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    private boolean isUserAdmin(ScopeAccess requesterScopeAccess, List<TenantRole> tenantRoles) {
        if(tenantRoles == null){
            tenantRoles = tenantService.getTenantRolesForScopeAccess(requesterScopeAccess);
        }
        boolean hasRole = false;
        for(TenantRole tenantRole : tenantRoles)
        {
            String name = tenantRole.getName();
            if (name.equals("identity:user-admin")){
                hasRole = true;
            }
        }
        return hasRole;
    }
    private boolean isDefaultUser(ScopeAccess requesterScopeAccess, List<TenantRole> tenantRoles) {
        if(tenantRoles == null){
            tenantRoles = tenantService.getTenantRolesForScopeAccess(requesterScopeAccess);
        }
        boolean hasRole = false;
        for(TenantRole tenantRole : tenantRoles)
        {
            String name = tenantRole.getName();
            if (name.equals("identity:default")){
                hasRole = true;
            }
        }
        return hasRole;
    }



    @Override
    public ResponseBuilder listEndpoints(HttpHeaders httpHeaders, String authToken, String tenantId) {

        try {
            verifyServiceAdminLevelAccess(authToken);
            verifyTokenHasTenantAccess(authToken, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            List<CloudBaseUrl> baseUrls = this.endpointService.getGlobalBaseUrls();
            if (tenant.getBaseUrlIds() != null) {
                for (String id : tenant.getBaseUrlIds()) {
                    Integer baseUrlId = Integer.parseInt(id);
                    //ToDo: Do not add if in global list also
                    baseUrls.add(this.endpointService.getBaseUrlById(baseUrlId));
                }
            }
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createEndpoints(this.endpointConverterCloudV20.toEndpointListFromBaseUrls(baseUrls)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders, String authToken, String tokenId) throws IOException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            ScopeAccess sa = checkAndGetToken(tokenId);

            List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(sa);

            EndpointList list = endpointConverterCloudV20.toEndpointList(endpoints);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoints(list).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders, String authToken, String serviceId) {

        try {
            verifyServiceAdminLevelAccess(authToken);

            List<CloudBaseUrl> baseUrls = null;

            if (StringUtils.isBlank(serviceId)) {
                baseUrls = this.endpointService.getBaseUrls();
            } else {
                Application client = checkAndGetApplication(serviceId);
                baseUrls = this.endpointService.getBaseUrlsByServiceId(client.getOpenStackType());
            }

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityExtKscatalogV1Factory()
                            .createEndpointTemplates(endpointConverterCloudV20.toEndpointTemplateList(baseUrls)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders) throws IOException {
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
            return serviceExceptionResponse();
        }
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken, String serviceId, String marker, Integer limit) {

        try {
            verifyServiceAdminLevelAccess(authToken);

            List<ClientRole> roles;

            if (StringUtils.isBlank(serviceId)) {
                roles = this.clientService.getAllClientRoles(null);
            } else {
                roles = this.clientService.getClientRolesByClientId(serviceId);
            }

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListFromClientRoles(roles)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) {

        try {
            verifyServiceAdminLevelAccess(authToken);
            verifyTokenHasTenantAccess(authToken, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            List<TenantRole> roles = this.tenantService.getTenantRolesForTenant(tenant.getTenantId());

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                    String userId) throws IOException {

        try {
            verifyServiceAdminLevelAccess(authToken);
            verifyTokenHasTenantAccess(authToken, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            User user = checkAndGetUser(userId);

            List<TenantRole> roles = this.tenantService.getTenantRolesForUserOnTenant(user, tenant);

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }

    }

    // KSADM Extension Role Methods

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String authToken, String marker, Integer limit) {

        try {
            verifyServiceAdminLevelAccess(authToken);

            List<Application> clients = this.clientService.getOpenStackServices();

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory().createServices(serviceConverterCloudV20.toServiceList(clients)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String authToken, String marker, Integer limit)
            throws IOException {
        try {
            verifyUserLevelAccess(authToken);
            List<Tenant> tenants = new ArrayList<Tenant>();

            ScopeAccess access = this.scopeAccessService.getAccessTokenByAuthHeader(authToken);
            if (access == null) { // ToDo: Send an empty list, it's what Cloud does.
                //return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenants(
                //        this.tenantConverterCloudV20.toTenantList(tenants)));
                throw new NotAuthorizedException("Not authorized.");
            }

            ScopeAccess sa = this.scopeAccessService.getScopeAccessByAccessToken(authToken);

            if (sa != null) {
                tenants = this.tenantService.getTenantsForScopeAccessByTenantRoles(sa);
            }

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenants(tenantConverterCloudV20.toTenantList(tenants)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {

        try {
            verifyUserLevelAccess(authToken);

            User user = this.userService.getUserById(userId);
            if (user == null) {
                String errMsg = "No Roles found User with id: " + userId;
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }
            ScopeAccess callersScopeAccess = scopeAccessService.getScopeAccessByAccessToken(authToken);
            User caller = getUser(callersScopeAccess);
            List<TenantRole> callersTenantRoles = tenantService.getTenantRolesForScopeAccess(callersScopeAccess);
            if (!authorizationService.authorizeCloudIdentityAdmin(callersScopeAccess)
                    && !authorizationService.authorizeCloudServiceAdmin(callersScopeAccess)) {
                if(isDefaultUser(callersScopeAccess,callersTenantRoles)){
                    verifySelf(authToken, user);
                }else{
                    verifyDomain(user, caller);
                }
            }

            List<TenantRole> roles = tenantService.getGlobalRolesForUser(user);
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUserGlobalRolesByServiceId(HttpHeaders httpHeaders, String authToken, String userId,
                                                          String serviceId) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);

            User user = checkAndGetUser(userId);

            List<TenantRole> roles = tenantService.getGlobalRolesForUser(user, new FilterParam[]{new FilterParam(FilterParamName.APPLICATION_ID, serviceId)});

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoles(roleConverterCloudV20.toRoleListJaxb(roles)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listGroups(HttpHeaders httpHeaders, String authToken, String groupName, String marker, Integer limit) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            List<Group> groups = cloudGroupService.getGroups(marker, limit);

            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups cloudGroups = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups();

            for (Group group : groups) {
                com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(group);
                cloudGroups.getGroup().add(cloudGroup);
            }

            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKsgrpV1Factory().createGroups(cloudGroups).getValue());
        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder getGroup(HttpHeaders httpHeaders, String authToken, String groupName) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            Group group = cloudGroupService.getGroupByName(groupName);
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(group);
            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKsgrpV1Factory().createGroup(cloudGroup).getValue());
        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder impersonate(HttpHeaders httpHeaders, String authToken, ImpersonationRequest impersonationRequest) throws IOException, JAXBException {
        //verifyServiceAdminLevelAccess(authToken);
        verifyRackerOrServiceAdminAccess(authToken);
        validateImpersonationRequest(impersonationRequest);

        String impersonatingToken = "";
        String impersonatingUsername = impersonationRequest.getUser().getUsername();

        User user = userService.getUser(impersonatingUsername);
        if (user == null) {
            logger.info("Impersonation call - calling cloud auth to get user");
            // Get from cloud.
            impersonatingToken = delegateCloud20Service.impersonateUser(impersonatingUsername, config.getString("ga.username"), config.getString("ga.password"));
        }else if(!user.isEnabled()){
            throw new ForbiddenException("User cannot be impersonated; User is not enabled");
        }else {
            if (!isValidImpersonatee(user)) {
                throw new BadRequestException("User cannot be impersonated; No valid impersonation roles assigned");
            }
            UserScopeAccess impAccess = (UserScopeAccess) scopeAccessService.getDirectScopeAccessForParentByClientId(user.getUniqueId(), getCloudAuthClientId());

            if (impAccess.isAccessTokenExpired(new DateTime())) {
                scopeAccessService.updateExpiredUserScopeAccess(impAccess);
            }
            impersonatingToken = impAccess.getAccessTokenString();
        }

        if (impersonatingToken == "" || impersonatingUsername == "") {
            throw new BadRequestException("Invalid user");
        }

        ScopeAccess sa = checkAndGetToken(authToken);
        ScopeAccess usa;
        if (sa instanceof UserScopeAccess) {
            UserScopeAccess userSa = (UserScopeAccess) sa;
            User impersonator = this.userService.getUserById(userSa.getUserRsId());
            usa = scopeAccessService.addImpersonatedScopeAccess(impersonator, getCloudAuthClientId(), impersonatingUsername, impersonatingToken);
        } else if (sa instanceof RackerScopeAccess) {
            RackerScopeAccess rackerSa = (RackerScopeAccess) sa;
            Racker racker = this.userService.getRackerByRackerId(rackerSa.getRackerId());
            usa = scopeAccessService.addImpersonatedScopeAccess(racker, getCloudAuthClientId(), impersonatingUsername, impersonatingToken);
        } else
            throw new NotAuthorizedException("User does not have access");

        ImpersonationResponse auth = authConverterCloudV20.toImpersonationResponse(usa);
        return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtRaxgaV1Factory().createAccess(auth).getValue());
    }

    private void validateImpersonationRequest(ImpersonationRequest impersonationRequest) {
        if (impersonationRequest.getUser() == null){
            throw new BadRequestException("User cannot be null for impersonation request");
        }else if (impersonationRequest.getUser().getUsername() == null){
            throw new BadRequestException("Username cannot be null for impersonation request");
        }else if(impersonationRequest.getUser().getUsername().isEmpty() || StringUtils.isBlank(impersonationRequest.getUser().getUsername())){
            throw new BadRequestException("Username cannot be empty or blank");
        }

    }

    public boolean isValidImpersonatee(User user) {
        List<TenantRole> tenantRolesForUser = tenantService.getGlobalRolesForUser(user, null);
        for (TenantRole role : tenantRolesForUser) {
            String name = role.getName();
            if (name.equals("identity:default") || name.equals("identity:user-admin")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String authToken, String userId) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            List<Group> groups = cloudGroupService.getGroupsForUser(userId);
            if(groups.size() == 0){
                Group defGroup = cloudGroupService.getGroupById(config.getInt("defaultGroupId"));
                groups.add(defGroup);
            }
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups cloudGroups = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups();
            for (Group group : groups) {
                com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(group);
                cloudGroups.getGroup().add(cloudGroup);
            }
            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKsgrpV1Factory().createGroups(cloudGroups).getValue());

        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder getGroupById(HttpHeaders httpHeaders, String authToken, String groupId) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            validateGroupId(groupId);
            Group group = cloudGroupService.getGroupById(Integer.parseInt(groupId));
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group cloudGroup = cloudKsGroupBuilder.build(group);
            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKsgrpV1Factory().createGroup(cloudGroup).getValue());
        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder addGroup(HttpHeaders httpHeaders, UriInfo uriInfo, String authToken,
                                    com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            validateKsGroup(group);
            Group groupDO = cloudGroupBuilder.build(group);
            cloudGroupService.addGroup(groupDO);
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs = cloudKsGroupBuilder.build(groupDO);
            return Response.created(uriInfo.getRequestUriBuilder().path(groupKs.getId()).build())
                    .entity(OBJ_FACTORIES.getRackspaceIdentityExtKsgrpV1Factory().createGroup(groupKs).getValue());
        } catch (DuplicateException bre) {
            return roleConflictExceptionResponse(bre.getMessage());
        } catch (Exception e) {
            return exceptionResponse(e);
        }


    }

    public void validateKsGroup(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group) {
        String checkName = group.getName().trim();
        if (group.getName() == null || checkName.isEmpty()) {
            throw new BadRequestException("Missing group name");
        }
        if (group.getName().length() > 200) {
            throw new BadRequestException("Group name length cannot exceed 200 characters");
        }
        if (group.getDescription().length() > 1000) {
            throw new BadRequestException("Group description length cannot exceed 1000 characters");
        }
    }

    @Override
    public ResponseBuilder updateGroup(HttpHeaders httpHeaders, String authToken, String groupId,
                                       com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            validateKsGroup(group);
            validateGroupId(groupId);
            group.setId(groupId);
            Group groupDO = cloudGroupBuilder.build(group);
            cloudGroupService.updateGroup(groupDO);
            com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs = cloudKsGroupBuilder.build(groupDO);
            return Response.ok().entity(OBJ_FACTORIES.getRackspaceIdentityExtKsgrpV1Factory().createGroup(groupKs).getValue());
        } catch (DuplicateException bre) {
            return roleConflictExceptionResponse(bre.getMessage());
        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder deleteGroup(HttpHeaders httpHeaders, String authToken, String groupId) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            validateGroupId(groupId);
            cloudGroupService.deleteGroup(groupId);
            return Response.noContent();
        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    public void validateGroupId(String groupId) {
        Boolean valid = false;
        if (groupId != null) {
            String checkGroupId = groupId.trim();
            try {
                Integer.parseInt(checkGroupId);
                valid = true;
            } catch (Exception e) {
                valid = false;
            }
        }
        if (!valid) {
            throw new BadRequestException("Invalid group id");
        }

    }

    @Override
    public ResponseBuilder addGroupToUser(HttpHeaders httpHeaders, String authToken, String groupId, String userId) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            validateGroupId(groupId);
            cloudGroupService.addGroupToUser(Integer.parseInt(groupId), userId);
            return Response.noContent();
        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder removeGroupFromUser(HttpHeaders httpHeaders, String authToken, String groupId, String userId) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            validateGroupId(groupId);
            if (userId == null || userId.trim().isEmpty()) {
                throw new BadRequestException("Invalid user id");
            }
            cloudGroupService.deleteGroupFromUser(Integer.parseInt(groupId), userId);
            return Response.noContent();
        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder listUsersWithGroup(HttpHeaders httpHeaders, String authToken, String groupId, String marker, Integer limit) throws IOException {
        try {
            verifyServiceAdminLevelAccess(authToken);
            validateGroupId(groupId);
            FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParamName.GROUP_ID, groupId)};
            String iMarker = (marker != null) ? marker : "0";
            int iLimit = (limit != null) ? limit : 0;
            Group exist = cloudGroupService.getGroupById(Integer.parseInt(groupId));
            if (exist == null) {
                String errorMsg = String.format("Group %s not found", groupId);
                throw new NotFoundException(errorMsg);
            }
            Users users = cloudGroupService.getAllEnabledUsers(filters, iMarker, iLimit);
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUsers(this.userConverterCloudV20.toUserList(users.getUsers())).getValue());
        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    // KSADM Extension User methods

    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken, Integer marker, Integer limit) {

        try {
            ScopeAccess scopeAccessByAccessToken = scopeAccessService.getScopeAccessByAccessToken(authToken);
            User caller = getUser(scopeAccessByAccessToken);

            //if default user
            if (authorizationService.authorizeCloudUser(scopeAccessByAccessToken)) {
                List<User> users = new ArrayList<User>();
                users.add(caller);
                return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                        .createUsers(this.userConverterCloudV20.toUserList(users)).getValue());
            }
            verifyUserAdminLevelAccess(authToken);
            Users users = new Users();
            if (authorizationService.authorizeCloudIdentityAdmin(scopeAccessByAccessToken) ||
                    authorizationService.authorizeCloudServiceAdmin(scopeAccessByAccessToken)) {
                users = this.userService.getAllUsers(null, marker, limit);
            } else {
                if (caller.getDomainId() != null) {
                    String domainId = caller.getDomainId();
                    FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParamName.DOMAIN_ID, domainId)};
                    users = this.userService.getAllUsers(filters, marker, limit);
                }
            }

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createUsers(this.userConverterCloudV20.toUserList(users.getUsers())).getValue());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders, String authToken, String tenantId, String marker, Integer limit) {

        try {
            verifyUserAdminLevelAccess(authToken);
            verifyTokenHasTenantAccess(authToken, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            List<User> users = this.tenantService.getUsersForTenant(tenant.getTenantId());

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUsers(userConverterCloudV20.toUserList(users)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                                      String roleId, String marker, Integer limit) {

        try {
            verifyUserAdminLevelAccess(authToken);
            verifyTokenHasTenantAccess(authToken, tenantId);

            Tenant tenant = checkAndGetTenant(tenantId);

            ClientRole role = checkAndGetClientRole(roleId);

            List<User> users = this.tenantService.getUsersWithTenantRole(tenant, role);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createUsers(this.userConverterCloudV20.toUserList(users)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders, String authToken, String userId,
                                          org.openstack.docs.identity.api.v2.User user) throws IOException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            User userDO = checkAndGetUser(userId);

            userDO.setEnabled(user.isEnabled());
            this.userService.updateUser(userDO, false);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createUser(this.userConverterCloudV20.toUser(userDO)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    public void setUserGroupService(GroupService cloudGroupService) {
        this.cloudGroupService = cloudGroupService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseBuilder updateSecretQA(HttpHeaders httpHeaders, String authToken, String userId, SecretQA secrets)
            throws IOException, JAXBException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            if (StringUtils.isBlank(secrets.getAnswer())) {
                throw new BadRequestException("Excpeting answer");
            }

            if (StringUtils.isBlank(secrets.getQuestion())) {
                throw new BadRequestException("Excpeting question");
            }

            User user = checkAndGetUser(userId);

            user.setSecretAnswer(secrets.getAnswer());
            user.setSecretQuestion(secrets.getQuestion());

            this.userService.updateUser(user, false);

            return Response.ok();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders, String authToken, String tenantId,
                                        org.openstack.docs.identity.api.v2.Tenant tenant) throws IOException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            Tenant tenantDO = checkAndGetTenant(tenantId);

            tenantDO.setDescription(tenant.getDescription());
            tenantDO.setDisplayName(tenant.getDisplayName());
            tenantDO.setEnabled(tenant.isEnabled());
            tenantDO.setName(tenant.getName());

            this.tenantService.updateTenant(tenantDO);

            return Response.ok(
                    OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenant(tenantConverterCloudV20.toTenant(tenantDO)).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    void verifyDomain(User retrievedUser, User caller) {
        if (caller.getDomainId() == null || !caller.getDomainId().equals(retrievedUser.getDomainId())) {
            throw new ForbiddenException("Not authorized.");
        }
    }

    @Override
    public ResponseBuilder updateUserApiKeyCredentials(HttpHeaders httpHeaders, String authToken, String userId,
                                                       String credentialType, ApiKeyCredentials creds) throws IOException, JAXBException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            if (StringUtils.isBlank(creds.getApiKey())) {
                String errMsg = "Expecting apiKey";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            validateUsername(creds.getUsername());
            User credUser = this.userService.getUser(creds.getUsername());

            if (credUser == null) {
                String errMsg = String.format("User: '%s' not found.", creds.getUsername());
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }


            User user = checkAndGetUser(userId);
            if (!creds.getUsername().equals(user.getUsername())) {
                String errMsg = "User and UserId mis-matched";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            user.setApiKey(creds.getApiKey());
            this.userService.updateUser(user, false);

            return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(creds).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder updateUserPasswordCredentials(HttpHeaders httpHeaders, String authToken, String userId,
                                                         String credentialType, PasswordCredentialsRequiredUsername creds) throws IOException, JAXBException {

        try {
            verifyServiceAdminLevelAccess(authToken);

            validatePasswordCredentials(creds);
            validatePassword(creds.getPassword());
            User user = checkAndGetUser(userId);
            if (!creds.getUsername().equals(user.getUsername())) {
                String errMsg = "User and UserId mis-matched";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            user.setPassword(creds.getPassword());
            this.userService.updateUser(user, false);

            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createPasswordCredentials(creds).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    // Core Admin Token Methods
    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo)
            throws IOException {
        try {
            //TODO: This token can be a Racker, Service or User of Proper Level
            verifyServiceAdminLevelAccess(authToken);

            ScopeAccess sa = checkAndGetToken(tokenId);

            AuthenticateResponse access = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createAuthenticateResponse();

            access.setToken(this.tokenConverterCloudV20.toToken(sa));

            if (sa instanceof UserScopeAccess || sa instanceof ImpersonatedScopeAccess) {
                String username = "";
                String impersonatedToken = "";
                User impersonator = null;
                User user = null;
                List<TenantRole> roles = null;
                if (sa instanceof UserScopeAccess) {
                    UserScopeAccess usa = (UserScopeAccess) sa;
                    user = userService.getUserByScopeAccess(usa);
                    roles = getRolesForScopeAccess(sa);
                    validateBelongsTo(belongsTo, roles);
                    access.setUser(userConverterCloudV20.toUserForAuthenticateResponse(user, roles));
                } else if (sa instanceof ImpersonatedScopeAccess) {
                    ImpersonatedScopeAccess isa = (ImpersonatedScopeAccess) sa;
                    impersonator = userService.getUserByScopeAccess(isa);
                    user = userService.getUser(isa.getImpersonatingUsername());
                    roles = tenantService.getTenantRolesForUser(user, null);
                    validateBelongsTo(belongsTo, roles);
                    ImpersonationResponse impersonationResponse = new ImpersonationResponse();
                    impersonationResponse.setToken(tokenConverterCloudV20.toToken(isa));
                    impersonationResponse.setUser(userConverterCloudV20.toUserForAuthenticateResponse(user, roles));
                    List<TenantRole> impRoles = this.tenantService.getGlobalRolesForUser(impersonator, null);

                    impersonationResponse.setImpersonator(this.userConverterCloudV20.toUserForAuthenticateResponse(impersonator, impRoles));
                    return Response.ok(OBJ_FACTORIES.getRackspaceIdentityExtRaxgaV1Factory().createAccess(impersonationResponse).getValue());
                }


            }
            return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createAccess(access).getValue());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    private void validateBelongsTo(String belongsTo, List<TenantRole> roles) {
        if (!belongsTo(belongsTo, roles)) {
            String errMsg = String.format("Token doesn't belong to Tenant with Id/Name: '%s'", belongsTo);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
    }

    public List<TenantRole> getRolesForScopeAccess(ScopeAccess scopeAccess) {
        List<TenantRole> roles = null;
        if (scopeAccess instanceof UserScopeAccess) {
            roles = this.tenantService.getTenantRolesForScopeAccess(scopeAccess);
        } else if (scopeAccess instanceof ImpersonatedScopeAccess) {
            roles = this.tenantService.getTenantRolesForScopeAccess(scopeAccess);
        }
        return roles;
    }

    private Response.ResponseBuilder badRequestExceptionResponse(String message) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_BAD_REQUEST);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(
                OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault));
    }

    Response.ResponseBuilder exceptionResponse(Exception ex) {
        if (ex instanceof BadRequestException) {
            return badRequestExceptionResponse(ex.getMessage());
        } else if (ex instanceof NotAuthorizedException) {
            return notAuthenticatedExceptionResponse(ex.getMessage());
        } else if (ex instanceof NotAuthenticatedException) {
            return notAuthenticatedExceptionResponse(ex.getMessage());
        } else if (ex instanceof ForbiddenException) {
            return forbiddenExceptionResponse(ex.getMessage());
        } else if (ex instanceof NotFoundException) {
            return notFoundExceptionResponse(ex.getMessage());
        } else if (ex instanceof ClientConflictException) {
            return tenantConflictExceptionResponse(ex.getMessage());
        } else if (ex instanceof UserDisabledException) {
            return userDisabledExceptionResponse(ex.getMessage());
        } else if (ex instanceof StalePasswordException) {
            return badRequestExceptionResponse(ex.getMessage());
        } else {
            return serviceExceptionResponse();
        }
    }

    private boolean belongsTo(String belongsTo, List<TenantRole> roles) {

        if (StringUtils.isBlank(belongsTo)) {
            return true;
        }

        if (roles == null || roles.size() == 0) {
            return false;
        }

        boolean ok = false;

        for (TenantRole role : roles) {
            if (role.containsTenantId(belongsTo)) {
                ok = true;
                break;
            }
        }
        return ok;
    }

    private Application checkAndGetApplication(String applicationId) {
        Application application = this.clientService.getById(applicationId);
        if (application == null) {
            String errMsg = String.format("Service %s not found", applicationId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return application;
    }

    ClientRole checkAndGetClientRole(String id) {
        ClientRole cRole = this.clientService.getClientRoleById(id);
        if (cRole == null) {
            String errMsg = String.format("Role %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return cRole;
    }

    CloudBaseUrl checkAndGetEndpointTemplate(int baseUrlId) {
        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("EndpointTemplate %s not found", baseUrlId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return baseUrl;
    }

    private CloudBaseUrl checkAndGetEndpointTemplate(String id) {
        Integer baseUrlId;
        try {
            baseUrlId = Integer.parseInt(id);
        } catch (NumberFormatException nfe) {
            String errMsg = String.format("EndpointTemplate %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return checkAndGetEndpointTemplate(baseUrlId);
    }

    Tenant checkAndGetTenant(String tenantId) {
        Tenant tenant = this.tenantService.getTenant(tenantId);

        if (tenant == null) {
            String errMsg = String.format("Tenant with id/name: '%s' was not found.", tenantId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return tenant;
    }

    private ScopeAccess checkAndGetToken(String tokenId) {
        ScopeAccess sa = this.scopeAccessService.getScopeAccessByAccessToken(tokenId);

        if (sa == null) {
            String errMsg = String.format("Token %s not found", tokenId);
            logger.warn(errMsg);
            throw new NotFoundException("Token not found.");
        }

        return sa;
    }

    User checkAndGetUser(String id) {
        User user = this.userService.getUserById(id);

        if (user == null) {
            String errMsg = String.format("User %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException("User not found");
        }

        return user;
    }

    private User checkAndGetUserByName(String username) {
        User user = this.userService.getUser(username);

        if (user == null) {
            String errMsg = String.format("User '%s' not found.", username);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    private User checkAndGetSoftDeletedUser(String id) {
        User user = this.userService.getSoftDeletedUser(id);

        if (user == null) {
            String errMsg = String.format("User %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException("User not found");
        }

        return user;
    }

    //method verifies that caller has the identity admin

    void verifyIdentityAdminLevelAccess(String authToken) {
        ScopeAccess authScopeAccess = getScopeAccessForValidToken(authToken);
        if (!authorizationService.authorizeCloudIdentityAdmin(authScopeAccess)) {
            String errMsg = "Not authorized.";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    public ScopeAccess getScopeAccessForValidToken(String authToken) {
        String errMsg = "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.";
        if (StringUtils.isBlank(authToken)) {
            throw new NotAuthorizedException(errMsg);
        }
        ScopeAccess authScopeAccess = this.scopeAccessService.getScopeAccessByAccessToken(authToken);
        if (authScopeAccess == null || ((HasAccessToken) authScopeAccess).isAccessTokenExpired(new DateTime())) {
            throw new NotAuthorizedException(errMsg);
        }
        return authScopeAccess;
    }

    //method verifies that caller is an identity admin or a service admin

    void verifyServiceAdminLevelAccess(String authToken) {
        ScopeAccess authScopeAccess = getScopeAccessForValidToken(authToken);
        if(!authorizationService.authorizeCloudIdentityAdmin(authScopeAccess) && !authorizationService.authorizeCloudServiceAdmin(authScopeAccess)) {
            String errMsg = "Not authorized.";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }
    void verifySelf(String authToken, User user) throws Exception {
        ScopeAccess authTokenScopeAccess = getScopeAccessForValidToken(authToken);
        User requester = userService.getUserByScopeAccess(authTokenScopeAccess);

        String username = user.getUsername();
        String uniqueId = user.getUniqueId();
        String requesterUniqueId = requester.getUniqueId();
        String requesterUsername = requester.getUsername();

        if (!((username.equals(requesterUsername) && (uniqueId.equals(requesterUniqueId))))){
                String errMsg = "Not authorized.";
                logger.warn(errMsg);
                throw new ForbiddenException(errMsg);
        }
    }

    void verifyRackerOrServiceAdminAccess(String authToken) {
        ScopeAccess scopeAccess = getScopeAccessForValidToken(authToken);
        if (!authorizationService.authorizeRacker(scopeAccess) && !authorizationService.authorizeCloudServiceAdmin(scopeAccess)) {
            String errMsg = "Not authorized.";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    //method verifies that caller has identity admin, service admin, user admin or user role access

    void verifyUserLevelAccess(String authToken) {
        ScopeAccess authScopeAccess = getScopeAccessForValidToken(authToken);
        if (!authorizationService.authorizeCloudIdentityAdmin(authScopeAccess)
                && !authorizationService.authorizeCloudServiceAdmin(authScopeAccess)
                && !authorizationService.authorizeCloudUserAdmin(authScopeAccess)
                && !authorizationService.authorizeCloudUser(authScopeAccess)) {
            String errMsg = "Not authorized.";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    //method verifies that caller is an identity admin, service admin or user admin

    void verifyUserAdminLevelAccess(String authToken) {
        ScopeAccess authScopeAccess = getScopeAccessForValidToken(authToken);
        if (!authorizationService.authorizeCloudIdentityAdmin(authScopeAccess)
                && !authorizationService.authorizeCloudServiceAdmin(authScopeAccess)
                && !authorizationService.authorizeCloudUserAdmin(authScopeAccess)) {
            String errMsg = "Not authorized.";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    void checkXAUTHTOKEN(String authToken, boolean identityOnly, String tenantId) {
        ScopeAccess authScopeAccess = getScopeAccessForValidToken(authToken);

        boolean authorized = false;

        authorized = this.authorizationService.authorizeCloudIdentityAdmin(authScopeAccess);

        if (!identityOnly && !authorized) { // Check for User Admin Access
            authorized = this.authorizationService.authorizeCloudUserAdmin(authScopeAccess);
            if (authorized && tenantId != null) { // Check tenant access
                authorized = false; // until matched in the loop below
                List<Tenant> adminTenants = this.tenantService.getTenantsForScopeAccessByTenantRoles(authScopeAccess);
                for (Tenant tenant : adminTenants) {
                    if (tenant.getTenantId().equals(tenantId)) {
                        authorized = true;
                    }
                }
            }
        }

        if (!authorized) {
            String errMsg = "Not authorized.";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    void verifyTokenHasTenantAccess(String authToken, String tenantId) {
        ScopeAccess authScopeAccess = getScopeAccessForValidToken(authToken);
        if (authorizationService.authorizeCloudIdentityAdmin(authScopeAccess) || authorizationService.authorizeCloudServiceAdmin(authScopeAccess)) {
            return;
        }
        verifyTokenHasTenant(tenantId, authScopeAccess);
    }

    private void verifyTokenHasTenant(String tenantId, ScopeAccess authScopeAccess) {
        List<Tenant> adminTenants = this.tenantService.getTenantsForScopeAccessByTenantRoles(authScopeAccess);
        for (Tenant tenant : adminTenants) {
            if (tenant.getTenantId().equals(tenantId)) {
                return;
            }
        }
        String errMsg = "Not authorized.";
        logger.warn(errMsg);
        throw new ForbiddenException(errMsg);
    }

    void verifyTokenHasTenantAccessForAuthenticate(String authToken, String tenantId) {
        ScopeAccess authScopeAccess = getScopeAccessForValidToken(authToken);
        verifyTokenHasTenant(tenantId, authScopeAccess);
    }

    private void stripEndpoints(List<OpenstackEndpoint> endpoints) {
        for (int i = 0; i < endpoints.size(); i++) {
            for (CloudBaseUrl baseUrl : endpoints.get(i).getBaseUrls()) {
                baseUrl.setAdminUrl(null);
            }
        }
    }

    private Response.ResponseBuilder serviceConflictExceptionResponse(String errMsg) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault).getValue());
    }

    private Response.ResponseBuilder roleConflictExceptionResponse(String errMsg) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault).getValue());
    }

    private Response.ResponseBuilder endpointTemplateConflictException(String errMsg) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault).getValue());
    }

    private Response.ResponseBuilder forbiddenExceptionResponse(String errMsg) {
        ForbiddenFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createForbiddenFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_FORBIDDEN)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createForbidden(fault).getValue());
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

    private String getCloudAuthServiceAdminRole() {
        return config.getString("cloudAuth.serviceAdminRole");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }

    private JAXBElement<? extends CredentialType> getJSONCredentials(String jsonBody) {

        JAXBElement<? extends CredentialType> jaxbCreds = null;

        CredentialType creds = JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString(jsonBody);

        if (creds instanceof ApiKeyCredentials) {
            jaxbCreds = OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials((ApiKeyCredentials) creds);
        } else if (creds instanceof PasswordCredentialsRequiredUsername) {
            jaxbCreds = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createPasswordCredentials((PasswordCredentialsRequiredUsername) creds);
        }

        return jaxbCreds;
    }

    @SuppressWarnings("unchecked")
    private JAXBElement<? extends CredentialType> getXMLCredentials(String body) {
        JAXBElement<? extends CredentialType> cred = null;
        try {
            JAXBContext context = JAXBContextResolver.get();
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends CredentialType>) unmarshaller.unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            throw new BadRequestException();
        }
        return cred;
    }

    private Response.ResponseBuilder notAuthenticatedExceptionResponse(String message) {
        UnauthorizedFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUnauthorizedFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUnauthorized(fault).getValue());
    }

    private Response.ResponseBuilder notFoundExceptionResponse(String message) {
        ItemNotFoundFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createItemNotFound(fault).getValue());
    }

    private Response.ResponseBuilder serviceExceptionResponse() {
        IdentityFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createIdentityFault();
        fault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createIdentityFault(fault).getValue());
    }

    Response.ResponseBuilder tenantConflictExceptionResponse(String message) {
        TenantConflictFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenantConflictFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenantConflict(fault).getValue());
    }

    Response.ResponseBuilder userConflictExceptionResponse(String message) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_CONFLICT)
                .entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(fault).getValue());
    }

    Response.ResponseBuilder userDisabledExceptionResponse(String message) {
        UserDisabledFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUserDisabledFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_FORBIDDEN).
                entity(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUserDisabled(fault).getValue());
    }

    public void setOBJ_FACTORIES(JAXBObjectFactories OBJ_FACTORIES) {
        this.OBJ_FACTORIES = OBJ_FACTORIES;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
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

    public void setClientService(ApplicationService clientService) {
        this.clientService = clientService;
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
}
