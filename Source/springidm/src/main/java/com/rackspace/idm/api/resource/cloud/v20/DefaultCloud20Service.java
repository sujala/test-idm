package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

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

import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.BadRequestFault;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.IdentityFault;
import org.openstack.docs.identity.api.v2.ItemNotFoundFault;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.UnauthorizedFault;
import org.openstack.docs.identity.api.v2.UserDisabledFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApikeyCredentialsWithUsername;
import com.rackspace.idm.api.converter.cloudv20.AuthConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.RoleConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TenantConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TokenConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.UserDisabledException;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:14 PM
 */
@Component
public class DefaultCloud20Service implements Cloud20Service {

    @Autowired
    private UserService userService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private EndpointService endpointService;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private AuthConverterCloudV20 authConverterCloudV20;
    @Autowired
    private TokenConverterCloudV20 tokenConverterCloudV20;
    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;
    @Autowired
    private EndpointConverterCloudV20 endpointConverterCloudV20;
    @Autowired
    private TenantConverterCloudV20 tenantConverterCloudV20;
    @Autowired
    private RoleConverterCloudV20 roleConverterCloudV20;
    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;
    @Autowired
    private Configuration config;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Core Service Methods
    @Override
    public Response.ResponseBuilder authenticate(HttpHeaders httpHeaders,
        AuthenticationRequest authenticationRequest) throws IOException {

        User user = null;
        UserScopeAccess usa = null;

        if (authenticationRequest.getCredential().getDeclaredType()
            .isAssignableFrom(PasswordCredentialsRequiredUsername.class)) {

            PasswordCredentialsRequiredUsername creds = (PasswordCredentialsRequiredUsername) authenticationRequest
                .getCredential().getValue();
            String username = creds.getUsername();
            String password = creds.getPassword();

            user = this.userService.getUser(username);

            if (user == null) {
                return userNotFoundExceptionResponse(username);
            }

            try {
                usa = this.scopeAccessService
                    .getUserScopeAccessForClientIdByUsernameAndPassword(
                        username, password, getCloudAuthClientId());
            } catch (NotAuthenticatedException nae) {
                return notAuthenticatedExceptionResponse(username);
            } catch (UserDisabledException ude) {
                return userDisabledExceptionResponse(username);
            } catch (Exception ex) {
                return serviceExceptionResponse();
            }

        } else if (authenticationRequest.getCredential().getDeclaredType()
            .isAssignableFrom(ApikeyCredentialsWithUsername.class)) {
            ApikeyCredentialsWithUsername creds = (ApikeyCredentialsWithUsername) authenticationRequest
                .getCredential().getValue();
            String username = creds.getUsername();
            String key = creds.getApikey();

            user = this.userService.getUser(username);

            if (user == null) {
                return userNotFoundExceptionResponse(username);
            }

            try {
                usa = this.scopeAccessService
                    .getUserScopeAccessForClientIdByUsernameAndApiCredentials(
                        username, key, getCloudAuthClientId());
            } catch (NotAuthenticatedException nae) {
                return notAuthenticatedExceptionResponse(username);
            } catch (UserDisabledException ude) {
                return userDisabledExceptionResponse(username);
            } catch (Exception ex) {
                return serviceExceptionResponse();
            }
        }

        List<TenantRole> roles = this.tenantService
            .getTenantRolesForScopeAccess(usa);

        List<OpenstackEndpoint> endpoints = this.scopeAccessService
            .getOpenstackEndpointsForScopeAccess(usa);

        AuthenticateResponse auth = authConverterCloudV20
            .toAuthenticationResponse(user, usa, roles, endpoints);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createAccess(auth));
    }

    @Override
    public ResponseBuilder listTenants(HttpHeaders httpHeaders,
        String authToken, String marker, Integer limit) throws IOException {

        ScopeAccess sa = this.scopeAccessService
            .getScopeAccessByAccessToken(authToken);

        if (sa == null) {
            String errMsg = String.format("Token %s not found", authToken);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<Tenant> tenants = this.tenantService
            .getTenantsForScopeAccessByTenantRoles(sa);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createTenants(this.tenantConverterCloudV20.toTenantList(tenants)));
    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders)
        throws IOException {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias)
        throws IOException {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    // Core Admin Token Methods
    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders,
        String authToken, String tokenId, String belongsTo) throws IOException {

        ScopeAccess sa = this.scopeAccessService
            .getScopeAccessByAccessToken(tokenId);

        if (sa == null) {
            String errMsg = String.format("Token %s not found", tokenId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        AuthenticateResponse access = OBJ_FACTORIES
            .getOpenStackIdentityV2Factory().createAuthenticateResponse();

        access.setToken(this.tokenConverterCloudV20.toToken(sa));

        if (sa instanceof UserScopeAccess) {
            UserScopeAccess usa = (UserScopeAccess) sa;
            User user = this.userService.getUser(usa.getUsername());
            List<TenantRole> roles = this.tenantService
                .getTenantRolesForScopeAccess(usa);
            if (roles != null && roles.size() > 0) {
                access.setUser(this.userConverterCloudV20
                    .toUserForAuthenticateResponse(user, roles));
            }
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createAccess(access));
    }

    @Override
    public ResponseBuilder checkToken(HttpHeaders httpHeaders, String authToken, String tokenId, String belongsTo) throws IOException {
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders,
        String authToken, String tokenId) throws IOException {

        ScopeAccess sa = this.scopeAccessService
            .getScopeAccessByAccessToken(tokenId);

        if (sa == null) {
            String errMsg = String.format("Token %s not found", tokenId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<OpenstackEndpoint> endpoints = this.scopeAccessService
            .getOpenstackEndpointsForScopeAccess(sa);

        EndpointList list = this.endpointConverterCloudV20
            .toEndpointList(endpoints);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createEndpoints(list));
    }

    // Core Admin User Methods
    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders,
        String authToken, String name) throws IOException {

        User user = this.userService.getUser(name);

        if (user == null) {
            String errMsg = String.format("User %s not found", name);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createUser(this.userConverterCloudV20.toUser(user)));
    }

    @Override
    public ResponseBuilder getUserById(HttpHeaders httpHeaders,
        String authToken, String userId) throws IOException {

        User user = this.userService.getUserById(userId);

        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createUser(this.userConverterCloudV20.toUser(user)));
    }

    @Override
    public ResponseBuilder listUserGlobalRoles(HttpHeaders httpHeaders,
        String authToken, String userId) throws IOException {

        User user = this.userService.getUserById(userId);

        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<TenantRole> roles = this.tenantService.getGlobalRolesForUser(user);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createRoles(this.roleConverterCloudV20.toRoleListJaxb(roles)));
    }

    // Core Admin Tenant Methods
    @Override
    public ResponseBuilder getTenantById(HttpHeaders httpHeaders,
        String authToken, String tenantsId) throws IOException {

        Tenant tenant = this.tenantService.getTenant(tenantsId);

        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantsId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createTenant(this.tenantConverterCloudV20.toTenant(tenant)));
    }

    @Override
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders,
        String authToken, String name) throws IOException {
        Tenant tenant = this.tenantService.getTenantByName(name);

        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", name);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createTenant(this.tenantConverterCloudV20.toTenant(tenant)));
    }

    @Override
    public ResponseBuilder listRolesForUserOnTenant(HttpHeaders httpHeaders,
        String authToken, String tenantsId, String userId) throws IOException {

        Tenant tenant = this.tenantService.getTenant(tenantsId);

        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantsId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        User user = this.userService.getUserById(userId);

        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<TenantRole> roles = this.tenantService
            .getTenantRolesForUserOnTenant(user, tenant);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createRoles(this.roleConverterCloudV20.toRoleListJaxb(roles)));

    }

    // ====== END OF CORE METHODS

    // ====== START KSADM Extension Methods

    // KSADM Extension User methods
    @Override
    public ResponseBuilder listUsers(HttpHeaders httpHeaders, String authToken,
        String marker, int limit) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String userId) throws IOException {
        //TODO write me
        throw new UnsupportedOperationException("not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUserGroups");
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, org.openstack.docs.identity.api.v2.User user) {

        User userDO = this.userConverterCloudV20.toUserDO(user);

        this.userService.addUser(userDO);

        return Response.created(
            uriInfo.getRequestUriBuilder().path(user.getId()).build()).entity(
            OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUser(
                this.userConverterCloudV20.toUser(userDO)));
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders,
        String authToken, String userId,
        org.openstack.docs.identity.api.v2.User user) throws IOException {

        User retrievedUser = this.userService.getUserById(userId);

        if (retrievedUser == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        User userDO = this.userConverterCloudV20.toUserDO(user);

        retrievedUser.copyChanges(userDO);

        this.userService.updateUser(retrievedUser, false);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createUser(this.userConverterCloudV20.toUser(retrievedUser)));
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders,
        String authToken, String userId) throws IOException {

        User user = this.userService.getUserById(userId);

        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        user.setSoftDeleted(true);
        this.userService.updateUser(user, false);

        return Response.noContent();
    }

    @Override
    public ResponseBuilder listUserRoles(HttpHeaders httpHeaders,
        String authToken, String userId, String serviceId) {

        User user = this.userService.getUserById(userId);

        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<TenantRole> roles = this.tenantService.getGlobalRolesForUser(user);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createRoles(this.roleConverterCloudV20.toRoleListJaxb(roles)));
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders,
        String authToken, String userId, String roleId) {

        User user = this.userService.getUserById(userId);
        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        ClientRole cRole = this.clientService.getClientRoleById(roleId);
        if (cRole == null) {
            String errMsg = String.format("Role %s not found", roleId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        TenantRole role = new TenantRole();
        role.setClientId(cRole.getClientId());
        role.setId(cRole.getId());

        this.tenantService.addTenantRoleToUser(user, role);

        return Response.ok();
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders,
        String authToken, String userId, String roleId) {

        User user = this.userService.getUserById(userId);
        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<TenantRole> globalRoles = this.tenantService
            .getGlobalRolesForUser(user);

        TenantRole role = null;

        for (TenantRole globalRole : globalRoles) {
            if (globalRole.getId().equals(roleId)) {
                role = globalRole;
            }
        }

        if (role == null) {
            String errMsg = String.format("Role %s not found for user %s",
                roleId, userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        ClientRole cRole = this.clientService.getClientRoleById(roleId);

        role.setDescription(cRole.getDescription());
        role.setName(cRole.getName());

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createRole(this.roleConverterCloudV20.toRole(role)));
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders,
        String authToken, String userId, String roleId) {
        User user = this.userService.getUserById(userId);
        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<TenantRole> globalRoles = this.tenantService
            .getGlobalRolesForUser(user);

        TenantRole role = null;

        for (TenantRole globalRole : globalRoles) {
            if (globalRole.getId().equals(roleId)) {
                role = globalRole;
            }
        }

        if (role == null) {
            String errMsg = String.format("Role %s not found for user %s",
                roleId, userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        this.tenantService.deleteGlobalRole(role);

        return Response.noContent();
    }

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders,
        String authToken, String userId, String body) throws IOException {

        JAXBElement<? extends CredentialType> creds = null;

        try {
            if (httpHeaders.getMediaType().isCompatible(
                MediaType.APPLICATION_XML_TYPE)) {
                creds = getXMLCredentials(body);
            } else {
                creds = getJSONCredentials(body);
            }
        } catch (BadRequestException ex) {
            return badRequestExceptionResponse(ex.getMessage());
        }

        String username = null;
        String password = null;
        String apiKey = null;

        User user = null;

        if (creds.getDeclaredType().isAssignableFrom(
            PasswordCredentialsRequiredUsername.class)) {
            PasswordCredentialsRequiredUsername userCreds = (PasswordCredentialsRequiredUsername) creds
                .getValue();
            username = userCreds.getUsername();
            password = userCreds.getPassword();
            user = this.userService.getUserById(userId);
            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                logger.warn(errMsg);
                return notFoundExceptionResponse(errMsg);
            }
            if (!username.equals(user.getUsername())) {
                String errMsg = "User and UserId mis-matched";
                logger.warn(errMsg);
                return badRequestExceptionResponse(errMsg);
            }
            user.setPassword(password);
            this.userService.updateUser(user, false);
        } else if (creds.getDeclaredType().isAssignableFrom(
            ApikeyCredentialsWithUsername.class)) {
            ApikeyCredentialsWithUsername userCreds = (ApikeyCredentialsWithUsername) creds
                .getValue();
            username = userCreds.getUsername();
            apiKey = userCreds.getApikey();
            user = this.userService.getUserById(userId);
            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                logger.warn(errMsg);
                return notFoundExceptionResponse(errMsg);
            }
            if (!username.equals(user.getUsername())) {
                String errMsg = "User and UserId mis-matched";
                logger.warn(errMsg);
                return badRequestExceptionResponse(errMsg);
            }
            user.setApiKey(apiKey);
            this.userService.updateUser(user, false);
        }

        return Response.ok(
            OBJ_FACTORIES.getOpenStackIdentityV2Factory().createCredential(
                creds.getValue())).status(Status.CREATED);
    }

    @SuppressWarnings("unchecked")
    private JAXBElement<? extends CredentialType> getXMLCredentials(String body) {
        JAXBElement<? extends CredentialType> cred = null;
        try {
            JAXBContext context = JAXBContext
                .newInstance("org.openstack.docs.identity.api.v2:com.rackspace.docs.identity.api.ext.rax_kskey.v1");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends CredentialType>) unmarshaller
                .unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            throw new BadRequestException();
        }
        return cred;
    }

    private JAXBElement<? extends CredentialType> getJSONCredentials(
        String jsonBody) {

        JSONParser parser = new JSONParser();
        JAXBElement<? extends CredentialType> creds = null;

        try {
            JSONObject obj = (JSONObject) parser.parse(jsonBody);

            if (obj.containsKey("passwordCredentials")) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                    "passwordCredentials").toString());
                PasswordCredentialsRequiredUsername userCreds = new PasswordCredentialsRequiredUsername();
                String username = obj3.get("username").toString();
                String password = obj3.get("password").toString();
                if (StringUtils.isBlank(username)) {
                    throw new BadRequestException("username required");
                }
                if (StringUtils.isBlank(password)) {
                    throw new BadRequestException("password required");
                }
                userCreds.setUsername(username);
                userCreds.setPassword(password);
                creds = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createCredential(userCreds);

            } else if (obj.containsKey("apiKeyCredentials")) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                    "apiKeyCredentials").toString());
                ApikeyCredentialsWithUsername userCreds = new ApikeyCredentialsWithUsername();
                String username = obj3.get("username").toString();
                String apikey = obj3.get("apikey").toString();
                if (StringUtils.isBlank(username)) {
                    throw new BadRequestException("username required");
                }
                if (StringUtils.isBlank(apikey)) {
                    throw new BadRequestException("apikey required");
                }
                userCreds.setUsername(username);
                userCreds.setApikey(apikey);
                creds = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createCredential(userCreds);
            } else {
                throw new BadRequestException("unrecognized credential type");
            }
        } catch (ParseException e) {
            throw new BadRequestException("malformed JSON");
        }
        return creds;
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders,
        String authToken, String userId, String marker, Integer limit)
        throws IOException {

        User user = this.userService.getUserById(userId);
        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        CredentialListType creds = OBJ_FACTORIES
            .getOpenStackIdentityV2Factory().createCredentialListType();

        if (!StringUtils.isBlank(user.getPassword())) {
            PasswordCredentialsRequiredUsername userCreds = new PasswordCredentialsRequiredUsername();
            userCreds.setPassword(user.getPassword());
            userCreds.setUsername(user.getUsername());
            creds.getCredential().add(
                OBJ_FACTORIES.getOpenStackIdentityV2Factory().createCredential(
                    userCreds));
        }

        if (!StringUtils.isBlank(user.getApiKey())) {
            ApikeyCredentialsWithUsername userCreds = new ApikeyCredentialsWithUsername();
            userCreds.setApikey(user.getApiKey());
            userCreds.setUsername(user.getUsername());
            creds.getCredential().add(
                OBJ_FACTORIES.getOpenStackIdentityV2Factory().createCredential(
                    userCreds));
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createCredentials(creds));
    }

    @Override
    public ResponseBuilder updateUserCredential(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType, String body)
        throws IOException {
        JAXBElement<? extends CredentialType> creds = null;

        if (!(credentialType.equals("passwordCredentials") || credentialType
            .endsWith("apiKeyCredentials"))) {
            return badRequestExceptionResponse("unsupported credential type");
        }

        try {
            if (httpHeaders.getMediaType().isCompatible(
                MediaType.APPLICATION_XML_TYPE)) {
                creds = getXMLCredentials(body);
            } else {
                creds = getJSONCredentials(body);
            }
        } catch (BadRequestException ex) {
            return badRequestExceptionResponse(ex.getMessage());
        }

        String username = null;
        String password = null;
        String apiKey = null;

        User user = null;

        if (creds.getDeclaredType().isAssignableFrom(
            PasswordCredentialsRequiredUsername.class)) {
            PasswordCredentialsRequiredUsername userCreds = (PasswordCredentialsRequiredUsername) creds
                .getValue();
            username = userCreds.getUsername();
            password = userCreds.getPassword();
            user = this.userService.getUserById(userId);
            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                logger.warn(errMsg);
                return notFoundExceptionResponse(errMsg);
            }
            if (!username.equals(user.getUsername())) {
                String errMsg = "User and UserId mis-matched";
                logger.warn(errMsg);
                return badRequestExceptionResponse(errMsg);
            }
            user.setPassword(password);
            this.userService.updateUser(user, false);
        } else if (creds.getDeclaredType().isAssignableFrom(
            ApikeyCredentialsWithUsername.class)) {
            ApikeyCredentialsWithUsername userCreds = (ApikeyCredentialsWithUsername) creds
                .getValue();
            username = userCreds.getUsername();
            apiKey = userCreds.getApikey();
            user = this.userService.getUserById(userId);
            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                logger.warn(errMsg);
                return notFoundExceptionResponse(errMsg);
            }
            if (!username.equals(user.getUsername())) {
                String errMsg = "User and UserId mis-matched";
                logger.warn(errMsg);
                return badRequestExceptionResponse(errMsg);
            }
            user.setApiKey(apiKey);
            this.userService.updateUser(user, false);
        }

        return Response.ok(creds).status(Status.CREATED);
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType)
        throws IOException {

        if (!(credentialType.equals("passwordCredentials") || credentialType
            .endsWith("apiKeyCredentials"))) {
            return badRequestExceptionResponse("unsupported credential type");
        }

        User user = this.userService.getUserById(userId);
        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        if (credentialType.equals("passwordCredentials")) {
            user.setPassword("");
        } else if (credentialType.equals("apiKeyCredentials")) {
            user.setApiKey("");
        }

        this.userService.updateUser(user, false);

        return Response.noContent();
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders,
        String authToken, String userId, String credentialType)
        throws IOException {

        if (!(credentialType.equals("passwordCredentials") || credentialType
            .endsWith("apiKeyCredentials"))) {
            return badRequestExceptionResponse("unsupported credential type");
        }

        User user = this.userService.getUserById(userId);
        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        JAXBElement<? extends CredentialType> creds = null;

        if (credentialType.equals("passwordCredentials")) {
            if (StringUtils.isBlank(user.getPassword())) {
                return notFoundExceptionResponse("User doesn't have password credentials");
            }
            PasswordCredentialsRequiredUsername userCreds = new PasswordCredentialsRequiredUsername();
            userCreds.setPassword(user.getPassword());
            userCreds.setUsername(user.getUsername());
            creds = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                .createCredential(userCreds);

        } else if (credentialType.equals("apiKeyCredentials")) {
            if (StringUtils.isBlank(user.getApiKey())) {
                return notFoundExceptionResponse("User doesn't have api key credentials");
            }
            ApikeyCredentialsWithUsername userCreds = new ApikeyCredentialsWithUsername();
            userCreds.setApikey(user.getApiKey());
            userCreds.setUsername(user.getUsername());
            creds = OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory()
                .createApikeyCredentials(userCreds);
        }

        return Response.ok(creds);
    }

    // KSADM Extension Tenant Methods
    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, String authToken,
        String body) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String body) throws IOException {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String marker, Integer limit) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String roleId, String marker,
        Integer limit) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String marker, Integer limit) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String userId, String roleId) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String userId, String roleId) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    // KSADM Extension Role Methods
    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken,
        String serviceId, String marker, Integer limit) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, String authToken,
        String body) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken,
        String roleId) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders,
        String authToken, String roleId) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    // KSADM Extension Role Methods
    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders,
        String authToken, String marker, Integer limit) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders,
        String authToken, String body) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders,
        String authToken, String serviceId) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders,
        String authToken, String serviceId) {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders,
        String authToken, String userId, String body) throws IOException {
        // TODO write me
        return Response.status(Status.NOT_FOUND);
    }

    private Response.ResponseBuilder userNotFoundExceptionResponse(
        String username) {
        String errMsg = String.format("User %s not found", username);
        ItemNotFoundFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
            OBJ_FACTORIES.getOpenStackIdentityV2Factory().createItemNotFound(
                fault));
    }

    private Response.ResponseBuilder userDisabledExceptionResponse(
        String username) {
        String errMsg = String.format("User %s is disabled", username);
        UserDisabledFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createUserDisabledFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_FORBIDDEN).entity(
            OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUserDisabled(
                    fault));
    }

    private Response.ResponseBuilder notAuthenticatedExceptionResponse(
        String username) {
        String errMsg = String.format("User %s not authenticated", username);
        UnauthorizedFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createUnauthorizedFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED).entity(
            OBJ_FACTORIES.getOpenStackIdentityV2Factory().createUnauthorized(
                    fault));
    }

    private Response.ResponseBuilder notFoundExceptionResponse(String message) {
        ItemNotFoundFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
            OBJ_FACTORIES.getOpenStackIdentityV2Factory().createItemNotFound(
                    fault));
    }

    private Response.ResponseBuilder badRequestExceptionResponse(String message) {
        BadRequestFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_BAD_REQUEST);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
            OBJ_FACTORIES.getOpenStackIdentityV2Factory().createBadRequest(
                    fault));
    }

    private Response.ResponseBuilder serviceExceptionResponse() {
        IdentityFault fault = OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createIdentityFault();
        fault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            .entity(
                OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createIdentityFault(fault));
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }
}
