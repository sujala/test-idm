package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
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
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.BadRequestFault;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.IdentityFault;
import org.openstack.docs.identity.api.v2.ItemNotFoundFault;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.TenantConflictFault;
import org.openstack.docs.identity.api.v2.UnauthorizedFault;
import org.openstack.docs.identity.api.v2.UserDisabledFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.docs.identity.api.ext.rax_ksadm.v1.UserWithOnlyEnabled;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.api.converter.cloudv20.AuthConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.RoleConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.ServiceConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TenantConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TokenConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
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
import com.rackspace.idm.exception.DuplicateException;
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
    private ServiceConverterCloudV20 serviceConverterCloudV20;
    @Autowired
    private RoleConverterCloudV20 roleConverterCloudV20;
    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;
    @Autowired
    private Configuration config;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private JAXBElement<Extensions> currentExtensions;
    private HashMap<String, JAXBElement<Extension>> extensionMap;

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
            
            if (StringUtils.isBlank(username)) {
                String errMsg = "Expecting username";
                logger.warn(errMsg);
                return badRequestExceptionResponse(errMsg);
            }
            
            if (StringUtils.isBlank(password)) {
                String errMsg = "Expecting password";
                logger.warn(errMsg);
                return badRequestExceptionResponse(errMsg);
            }

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
            .isAssignableFrom(ApiKeyCredentials.class)) {
            ApiKeyCredentials creds = (ApiKeyCredentials) authenticationRequest
                .getCredential().getValue();
            String username = creds.getUsername();
            String key = creds.getApiKey();
            
            if (StringUtils.isBlank(username)) {
                String errMsg = "Expecting username";
                logger.warn(errMsg);
                return badRequestExceptionResponse(errMsg);
            }
            
            if (StringUtils.isBlank(key)) {
                String errMsg = "Expecting apiKey";
                logger.warn(errMsg);
                return badRequestExceptionResponse(errMsg);
            }

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

        if (!belongsTo(authenticationRequest.getTenantId(), roles)
            && !belongsTo(authenticationRequest.getTenantName(), roles)) {

            String errMsg = "User does not have access to tenant %s";
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

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

        List<Tenant> tenants = new ArrayList<Tenant>();

        ScopeAccess sa = this.scopeAccessService
            .getScopeAccessByAccessToken(authToken);

        if (sa != null) {
            tenants = this.tenantService
                .getTenantsForScopeAccessByTenantRoles(sa);
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createTenants(this.tenantConverterCloudV20.toTenantList(tenants)));
    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders)
        throws IOException {
        try {
            if (currentExtensions == null) {
                JAXBContext jaxbContext = JAXBContext
                    .newInstance("org.openstack.docs.common.api.v1:org.w3._2005.atom");
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                InputStream is = StringUtils.class
                    .getResourceAsStream("/extensions.xml");
                StreamSource ss = new StreamSource(is);

                currentExtensions = unmarshaller
                    .unmarshal(ss, Extensions.class);
            }

            return Response.ok(currentExtensions);
        } catch (Exception e) {
            // Return 500 error. Is WEB-IN/extensions.xml malformed?
            return serviceExceptionResponse();
        }
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias)
        throws IOException {
        if (StringUtils.isBlank(alias)) {
            return badRequestExceptionResponse("Invalid extension alias '"
                + alias + "'.");
        }

        final String normalizedAlias = alias.trim().toUpperCase();

        if (extensionMap == null) {
            extensionMap = new HashMap<String, JAXBElement<Extension>>();

            try {
                if (currentExtensions == null) {
                    JAXBContext jaxbContext = JAXBContext
                        .newInstance("org.openstack.docs.common.api.v1:org.w3._2005.atom");
                    Unmarshaller unmarshaller = jaxbContext
                        .createUnmarshaller();
                    InputStream is = StringUtils.class
                        .getResourceAsStream("/extensions.xml");
                    StreamSource ss = new StreamSource(is);

                    currentExtensions = unmarshaller.unmarshal(ss,
                        Extensions.class);
                }

                Extensions exts = currentExtensions.getValue();

                for (Extension e : exts.getExtension()) {
                    extensionMap.put(e.getAlias().trim().toUpperCase(),
                        OBJ_FACTORIES.getOpenStackCommonV1Factory()
                            .createExtension(e));
                }
            } catch (Exception e) {
                // Return 500 error. Is WEB-IN/extensions.xml malformed?
                return serviceExceptionResponse();
            }
        }

        if (!extensionMap.containsKey(normalizedAlias)) {
            return notFoundExceptionResponse("Extension with alias '"
                + normalizedAlias + "' is not available.");
        }

        return Response.ok(extensionMap.get(normalizedAlias));

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
            return notFoundExceptionResponse("Token not found");
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

            if (!belongsTo(belongsTo, roles)) {
                String errMsg = String.format("Token doesn't belong to Tenant with Id: '%s'", belongsTo);
                logger.warn(errMsg);
                return notFoundExceptionResponse("Token not found");
            }
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createAccess(access));
    }

    @Override
    public ResponseBuilder checkToken(HttpHeaders httpHeaders,
        String authToken, String tokenId, String belongsTo) throws IOException {

        ScopeAccess sa = this.scopeAccessService
            .getScopeAccessByAccessToken(tokenId);

        if (sa == null || !(sa instanceof UserScopeAccess)) {
            String errMsg = String.format("Token %s not found", tokenId);
            logger.warn(errMsg);
            return Response.ok().status(Status.NOT_FOUND);
        }

        if (!StringUtils.isBlank(belongsTo)) {
            UserScopeAccess usa = (UserScopeAccess) sa;
            List<TenantRole> roles = this.tenantService
                .getTenantRolesForScopeAccess(usa);

            if (!belongsTo(belongsTo, roles)) {
                return Response.ok().status(Status.NOT_FOUND);
            }
        }

        return Response.ok();
    }

    private boolean belongsTo(String belongsTo, List<TenantRole> roles) {

        if (StringUtils.isBlank(belongsTo)) {
            return true;
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
            return notFoundExceptionResponse("User not found");
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
            String errMsg = String.format(
                "Tenant with id/name: '%s' was not found", tenantsId);
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
            String errMsg = String.format(
                "Tenant with id/name: '%s' was not found", name);
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
    public ResponseBuilder listUserGroups(HttpHeaders httpHeaders, String userId)
        throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUserGroups");
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
        role.setRoleRsId(cRole.getId());

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
            if (globalRole.getRoleRsId().equals(roleId)) {
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
            if (globalRole.getRoleRsId().equals(roleId)) {
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
            ApiKeyCredentials.class)) {
            ApiKeyCredentials userCreds = (ApiKeyCredentials) creds.getValue();
            username = userCreds.getUsername();
            apiKey = userCreds.getApiKey();
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
                ApiKeyCredentials userCreds = new ApiKeyCredentials();
                String username = obj3.get("username").toString();
                String apikey = obj3.get("apikey").toString();
                if (StringUtils.isBlank(username)) {
                    throw new BadRequestException("username required");
                }
                if (StringUtils.isBlank(apikey)) {
                    throw new BadRequestException("apikey required");
                }
                userCreds.setUsername(username);
                userCreds.setApiKey(apikey);
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
            ApiKeyCredentials userCreds = new ApiKeyCredentials();
            userCreds.setApiKey(user.getApiKey());
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
            ApiKeyCredentials.class)) {
            ApiKeyCredentials userCreds = (ApiKeyCredentials) creds.getValue();
            username = userCreds.getUsername();
            apiKey = userCreds.getApiKey();
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

        return Response.ok(creds);
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
            ApiKeyCredentials userCreds = new ApiKeyCredentials();
            userCreds.setApiKey(user.getApiKey());
            userCreds.setUsername(user.getUsername());
            creds = OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory()
                .createApiKeyCredentials(userCreds);
        }

        return Response.ok(creds);
    }

    // KSADM Extension Tenant Methods
    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, org.openstack.docs.identity.api.v2.Tenant tenant) {

        Tenant savedTenant = this.tenantConverterCloudV20.toTenantDO(tenant);
        try {
            this.tenantService.addTenant(savedTenant);
        } catch (DuplicateException de) {
            return tenantConflictExceptionResponse(de.getMessage());
        } catch (Exception ex) {
            return serviceExceptionResponse();
        }

        return Response.created(
            uriInfo.getRequestUriBuilder().path(savedTenant.getTenantId())
                .build()).entity(
            OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenant(
                this.tenantConverterCloudV20.toTenant(savedTenant)));
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId,
        org.openstack.docs.identity.api.v2.Tenant tenant) throws IOException {

        Tenant tenantDO = this.tenantService.getTenant(tenantId);
        if (tenantDO == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        tenantDO.setDescription(tenant.getDescription());
        tenantDO.setDisplayName(tenant.getDisplayName());
        tenantDO.setEnabled(tenant.isEnabled());
        tenantDO.setName(tenant.getName());

        this.tenantService.updateTenant(tenantDO);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createTenant(this.tenantConverterCloudV20.toTenant(tenantDO)));
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId) {

        Tenant tenant = this.tenantService.getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        this.tenantService.deleteTenant(tenantId);

        return Response.noContent();
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String marker, Integer limit) {

        Tenant tenant = this.tenantService.getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<User> users = this.tenantService.getUsersForTenant(tenantId);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createUsers(this.userConverterCloudV20.toUserList(users)));
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String roleId, String marker,
        Integer limit) {

        Tenant tenant = this.tenantService.getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        ClientRole role = this.clientService.getClientRoleById(roleId);
        if (role == null) {
            String errMsg = String.format("Role %s not found", roleId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<User> users = this.tenantService.getUsersWithTenantRole(tenant,
            role);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createUsers(this.userConverterCloudV20.toUserList(users)));
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

        Tenant tenant = this.tenantService.getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        User user = this.userService.getUserById(userId);
        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        ClientRole role = this.clientService.getClientRoleById(roleId);
        if (role == null) {
            String errMsg = String.format("Role %s not found", roleId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        TenantRole tenantrole = new TenantRole();
        tenantrole.setClientId(role.getClientId());
        tenantrole.setRoleRsId(role.getId());
        tenantrole.setUserId(user.getId());
        tenantrole.setTenantIds(new String[]{tenant.getTenantId()});

        this.tenantService.addTenantRoleToUser(user, tenantrole);

        return Response.ok();
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders,
        String authToken, String tenantId, String userId, String roleId) {
        Tenant tenant = this.tenantService.getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        User user = this.userService.getUserById(userId);
        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        ClientRole role = this.clientService.getClientRoleById(roleId);
        if (role == null) {
            String errMsg = String.format("Role %s not found", roleId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        TenantRole tenantrole = new TenantRole();
        tenantrole.setClientId(role.getClientId());
        tenantrole.setRoleRsId(role.getId());
        tenantrole.setUserId(user.getId());
        tenantrole.setTenantIds(new String[]{tenant.getTenantId()});

        this.tenantService.deleteTenantRole(user.getUniqueId(), tenantrole);

        return Response.noContent();
    }

    // KSADM Extension Role Methods
    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String authToken,
        String serviceId, String marker, Integer limit) {

        List<ClientRole> roles = null;

        if (StringUtils.isBlank(serviceId)) {
            roles = this.clientService.getAllClientRoles();
        } else {
            roles = this.clientService.getClientRolesByClientId(serviceId);
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createRoles(
                this.roleConverterCloudV20.toRoleListFromClientRoles(roles)));
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, Role role) {

        ClientRole clientRole = new ClientRole();
        clientRole.setClientId(role.getServiceId());
        clientRole.setDescription(role.getDescription());
        clientRole.setName(role.getName());

        this.clientService.addClientRole(clientRole);

        return Response.created(
            uriInfo.getRequestUriBuilder().path(clientRole.getId()).build())
            .entity(
                OBJ_FACTORIES.getOpenStackIdentityV2Factory()
                    .createRole(
                        this.roleConverterCloudV20
                            .toRoleFromClientRole(clientRole)));
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String authToken,
        String roleId) {

        ClientRole role = this.clientService.getClientRoleById(roleId);
        if (role == null) {
            String errMsg = String.format("Role %s not found", roleId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createRole(this.roleConverterCloudV20.toRoleFromClientRole(role)));
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders,
        String authToken, String roleId) {

        ClientRole role = this.clientService.getClientRoleById(roleId);
        if (role == null) {
            String errMsg = String.format("Role %s not found", roleId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        this.clientService.deleteClientRole(role);

        return Response.noContent();
    }

    // KSADM Extension Role Methods
    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders,
        String authToken, String marker, Integer limit) {

        List<Client> clients = this.clientService.getOpenStackServices();

        return Response.ok(OBJ_FACTORIES
            .getOpenStackIdentityExtKsadmnV1Factory().createServices(
                this.serviceConverterCloudV20.toServiceList(clients)));
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, UriInfo uriInfo,
        String authToken, Service service) {

        Client client = new Client();
        client.setOpenStackType(service.getType());
        client.setDescription(service.getDescription());
        client.setName(service.getType());

        this.clientService.add(client);

        service.setId(client.getClientId());

        return Response.created(
            uriInfo.getRequestUriBuilder().path(service.getId()).build())
            .entity(
                OBJ_FACTORIES.getOpenStackIdentityExtKsadmnV1Factory()
                    .createService(service));
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders,
        String authToken, String serviceId) {

        Client client = this.clientService.getById(serviceId);
        if (client == null) {
            String errMsg = String.format("Service %s not found", serviceId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        return Response.ok(OBJ_FACTORIES
            .getOpenStackIdentityExtKsadmnV1Factory().createService(
                this.serviceConverterCloudV20.toService(client)));
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders,
        String authToken, String serviceId) {

        Client client = this.clientService.getById(serviceId);
        if (client == null) {
            String errMsg = String.format("Service %s not found", serviceId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        this.clientService.delete(client.getClientId());

        return Response.noContent();
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders,
        String authToken, String userId, UserWithOnlyEnabled user)
        throws IOException {

        User userDO = this.userService.getUserById(userId);
        if (userDO == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        userDO.setLocked(!user.isEnabled());
        this.userService.updateUser(userDO, false);

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createUser(this.userConverterCloudV20.toUser(userDO)));
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
        return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(
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

    private Response.ResponseBuilder tenantConflictExceptionResponse(
        String message) {
        TenantConflictFault fault = OBJ_FACTORIES
            .getOpenStackIdentityV2Factory().createTenantConflictFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
            OBJ_FACTORIES.getOpenStackIdentityV2Factory().createTenantConflict(
                fault));
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    @Override
    public ResponseBuilder listEndpointTemplates(HttpHeaders httpHeaders,
        String authToken, String serviceId) {

        List<CloudBaseUrl> baseUrls = null;

        if (StringUtils.isBlank(serviceId)) {
            baseUrls = this.endpointService.getBaseUrls();
        } else {
            Client client = this.clientService.getById(serviceId);
            if (client == null) {
                String errMsg = String
                    .format("Serivce %s not found", serviceId);
                logger.warn(errMsg);
                return notFoundExceptionResponse(errMsg);
            }
            baseUrls = this.endpointService.getBaseUrlsByServiceId(client
                .getOpenStackType());
        }

        return Response
            .ok(OBJ_FACTORIES.getOpenStackIdentityExtKscatalogV1Factory()
                .createEndpointTemplates(
                    this.endpointConverterCloudV20
                        .toEndpointTemplateList(baseUrls)));
    }

    @Override
    public ResponseBuilder addEndpointTemplate(HttpHeaders httpHeaders,
        UriInfo uriInfo, String authToken, EndpointTemplate endpoint) {

        CloudBaseUrl baseUrl = this.endpointConverterCloudV20
            .toCloudBaseUrl(endpoint);

        this.endpointService.addBaseUrl(baseUrl);

        return Response.created(
            uriInfo.getRequestUriBuilder()
                .path(String.valueOf(baseUrl.getBaseUrlId())).build())
            .entity(
                OBJ_FACTORIES.getOpenStackIdentityExtKscatalogV1Factory()
                    .createEndpointTemplate(
                        this.endpointConverterCloudV20
                            .toEndpointTemplate(baseUrl)));
    }

    @Override
    public ResponseBuilder getEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String endpointTemplateId) {

        Integer baseUrlId = Integer.parseInt(endpointTemplateId);

        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("EnpointTemplate %s not found",
                baseUrlId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        return Response.ok(OBJ_FACTORIES
            .getOpenStackIdentityExtKscatalogV1Factory()
            .createEndpointTemplate(
                this.endpointConverterCloudV20.toEndpointTemplate(baseUrl)));
    }

    @Override
    public ResponseBuilder deleteEndpointTemplate(HttpHeaders httpHeaders,
        String authToken, String endpointTemplateId) {

        Integer baseUrlId = Integer.parseInt(endpointTemplateId);

        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("EnpointTemplate %s not found",
                baseUrlId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        this.endpointService.deleteBaseUrl(baseUrlId);

        return Response.noContent();

    }

    @Override
    public ResponseBuilder listEndpoints(HttpHeaders httpHeaders,
        String authToken, String tenantId) {

        Tenant tenant = this.tenantService.getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        for (String id : tenant.getBaseUrlIds()) {
            Integer baseUrlId = Integer.parseInt(id);
            baseUrls.add(this.endpointService.getBaseUrlById(baseUrlId));
        }

        return Response.ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory()
            .createEndpoints(
                this.endpointConverterCloudV20
                    .toEndpointListFromBaseUrls(baseUrls)));
    }

    @Override
    public ResponseBuilder addEndpoint(HttpHeaders httpHeaders,
        String authToken, String tenantId, EndpointTemplate endpoint) {

        Tenant tenant = this.tenantService.getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(endpoint
            .getId());
        if (baseUrl == null) {
            String errMsg = String.format("EndpointTemplate %s not found",
                endpoint.getId());
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        tenant.addBaseUrlId(String.valueOf(endpoint.getId()));
        this.tenantService.updateTenant(tenant);

        return Response
            .ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoint(
                this.endpointConverterCloudV20.toEndpoint(baseUrl)));
    }

    @Override
    public ResponseBuilder getEndpoint(HttpHeaders httpHeaders,
        String authToken, String tenantId, String endpointId) {

        Tenant tenant = this.tenantService.getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        if (!tenant.containsBaseUrlId(endpointId)) {
            String errMsg = String.format(
                "Tenant %s does not have endpoint %s", tenantId, endpointId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        Integer baseUrlId = Integer.parseInt(endpointId);

        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("Endpoint %s not found", baseUrlId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        return Response
            .ok(OBJ_FACTORIES.getOpenStackIdentityV2Factory().createEndpoint(
                this.endpointConverterCloudV20.toEndpoint(baseUrl)));
    }

    @Override
    public ResponseBuilder deleteEndpoint(HttpHeaders httpHeaders,
        String authToken, String tenantId, String endpointId) {
        Tenant tenant = this.tenantService.getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        Integer baseUrlId = Integer.parseInt(endpointId);

        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseUrlId);
        if (baseUrl == null) {
            String errMsg = String.format("EndpointTemplate %s not found",
                baseUrlId);
            logger.warn(errMsg);
            return notFoundExceptionResponse(errMsg);
        }

        tenant.removeBaseUrlId(endpointId);

        this.tenantService.updateTenant(tenant);

        return Response.noContent();
    }
}
