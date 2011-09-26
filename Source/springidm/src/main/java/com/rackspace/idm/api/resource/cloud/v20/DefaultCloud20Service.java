package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
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

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApikeyCredentialsWithUsername;
import com.rackspace.idm.api.converter.cloudv20.AuthConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.RoleConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TenantConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TokenConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
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
    public ResponseBuilder listTenants(HttpHeaders httpHeaders, String marker,
        Integer limit) throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listTenants");
    }

    @Override
    public ResponseBuilder listExtensions(HttpHeaders httpHeaders)
        throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listExtensions");
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias)
        throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getExtension");
    }

    // Core Admin Token Methods
    @Override
    public ResponseBuilder validateToken(HttpHeaders httpHeaders,
        String tokenId, String belongsTo) throws IOException {

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
    public ResponseBuilder listEndpointsForToken(HttpHeaders httpHeaders,
        String tokenId) throws IOException {

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
    public ResponseBuilder listUsers(HttpHeaders httpHeaders) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUsers");
    }

    @Override
    public ResponseBuilder getUserByName(HttpHeaders httpHeaders, String name)
        throws IOException {

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
    public ResponseBuilder getUserById(HttpHeaders httpHeaders, String userId)
        throws IOException {

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
        String userId) throws IOException {

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
        String tenantsId) throws IOException {

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
    public ResponseBuilder getTenantByName(HttpHeaders httpHeaders, String name)
        throws IOException {
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
        String tenantsId, String userId) throws IOException {

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

    // END OF CORE METHODS

    @Override
    public ResponseBuilder addUserCredential(HttpHeaders httpHeaders,
        String userId, String body) throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addUserCredential");
    }

    @Override
    public ResponseBuilder listCredentials(HttpHeaders httpHeaders,
        String userId, String marker, Integer limit) throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listCredentials");
    }

    @Override
    public ResponseBuilder updateUserCredential(HttpHeaders httpHeaders,
        String userId, String body) throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.updateUserCredential");
    }

    @Override
    public ResponseBuilder getUserCredential(HttpHeaders httpHeaders,
        String userId) throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getUserCredential");
    }

    @Override
    public ResponseBuilder deleteUserCredential(HttpHeaders httpHeaders,
        String userId) throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteUserCredential");
    }

    @Override
    public ResponseBuilder addUser(HttpHeaders httpHeaders, String body) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addUser");
    }

    @Override
    public ResponseBuilder updateUser(HttpHeaders httpHeaders, String userId,
        String body) throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.updateUser");
    }

    @Override
    public ResponseBuilder deleteUser(HttpHeaders httpHeaders, String userId)
        throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteUser");
    }

    @Override
    public ResponseBuilder setUserEnabled(HttpHeaders httpHeaders,
        String userId, String body) throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.setUserEnabled");
    }

    @Override
    public ResponseBuilder listUserRoles(HttpHeaders httpHeaders,
        String userId, String serviceId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUserRoles");
    }

    @Override
    public ResponseBuilder addUserRole(HttpHeaders httpHeaders, String userId,
        String roleId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addUserRole");
    }

    @Override
    public ResponseBuilder getUserRole(HttpHeaders httpHeaders, String userId,
        String roleId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getUserRole");
    }

    @Override
    public ResponseBuilder deleteUserRole(HttpHeaders httpHeaders,
        String userId, String roleId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteUserRole");
    }

    @Override
    public ResponseBuilder OS_KSADM_addUserCredential(HttpHeaders httpHeaders,
        String userId, String body) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_addUserCredential");
    }

    @Override
    public ResponseBuilder OS_KSADM_listCredentials(HttpHeaders httpHeaders,
        String userId, String marker, Integer limit) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_listCredentials");
    }

    @Override
    public ResponseBuilder OS_KSADM_updateUserCredential(
        HttpHeaders httpHeaders, String userId, String credentialType,
        String body) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_updateUserCredential");
    }

    @Override
    public ResponseBuilder OS_KSADM_getUserCredential(HttpHeaders httpHeaders,
        String userId, String credentialType) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_getUserCredential");
    }

    @Override
    public ResponseBuilder OS_KSADM_deleteUserCredential(
        HttpHeaders httpHeaders, String userId, String credentialType) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.OS_KSADM_deleteUserCredential");
    }

    @Override
    public ResponseBuilder addTenant(HttpHeaders httpHeaders, String body) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addTenant");
    }

    @Override
    public ResponseBuilder updateTenant(HttpHeaders httpHeaders,
        String tenantId, String body) throws IOException {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.updateTenant");
    }

    @Override
    public ResponseBuilder deleteTenant(HttpHeaders httpHeaders, String tenantId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteTenant");
    }

    @Override
    public ResponseBuilder listRolesForTenant(HttpHeaders httpHeaders,
        String tenantId, String marker, Integer limit) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listRolesForTenant");
    }

    @Override
    public ResponseBuilder listUsersWithRoleForTenant(HttpHeaders httpHeaders,
        String tenantId, String roleId, String marker, Integer limit) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUsersWithRoleForTenant");
    }

    @Override
    public ResponseBuilder listUsersForTenant(HttpHeaders httpHeaders,
        String tenantId, String marker, Integer limit) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listUsersForTenant");
    }

    @Override
    public ResponseBuilder addRolesToUserOnTenant(HttpHeaders httpHeaders,
        String tenantId, String userId, String roleId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addRolesToUserOnTenant");
    }

    @Override
    public ResponseBuilder deleteRoleFromUserOnTenant(HttpHeaders httpHeaders,
        String tenantId, String userId, String roleId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteRoleFromUserOnTenant");
    }

    @Override
    public ResponseBuilder listRoles(HttpHeaders httpHeaders, String serviceId,
        String marker, Integer limit) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listRoles");
    }

    @Override
    public ResponseBuilder addRole(HttpHeaders httpHeaders, String body) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addRole");
    }

    @Override
    public ResponseBuilder getRole(HttpHeaders httpHeaders, String roleId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getRole");
    }

    @Override
    public ResponseBuilder deleteRole(HttpHeaders httpHeaders, String roleId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteRole");
    }

    @Override
    public ResponseBuilder listServices(HttpHeaders httpHeaders, String marker,
        Integer limit) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.listServices");
    }

    @Override
    public ResponseBuilder addService(HttpHeaders httpHeaders, String body) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.addService");
    }

    @Override
    public ResponseBuilder getService(HttpHeaders httpHeaders, String serviceId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.getService");
    }

    @Override
    public ResponseBuilder deleteService(HttpHeaders httpHeaders,
        String serviceId) {
        // TODO write me
        throw new UnsupportedOperationException(
            "not written -- com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.deleteService");
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
