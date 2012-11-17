package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/17/12
 * Time: 6:41 PM
 */
public class UserGlobalRoleResourceTest {

    UserGlobalRoleResource userGlobalRoleResource;
    private TenantService tenantService;
    private ScopeAccessService scopeAccessService;
    private AuthorizationService authorizationService;
    private UserService userService;
    private ApplicationService applicationService;

    @Before
    public void setUp() throws Exception {
        tenantService = mock(TenantService.class);
        scopeAccessService = mock(ScopeAccessService.class);
        authorizationService = mock(AuthorizationService.class);
        userService = mock(UserService.class);
        applicationService = mock(ApplicationService.class);
        userGlobalRoleResource = new UserGlobalRoleResource(userService,authorizationService, applicationService,scopeAccessService, tenantService);
    }

    @Test
    public void grantTenantRoleToUser_callsTenantService_getTenantById() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getAccessTokenByAuthHeader(null)).thenReturn(scopeAccess);
        doNothing().when(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);
        when(userService.loadUser("userId")).thenReturn(new User());
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        when(tenantService.getTenant("tenantId")).thenReturn(new Tenant());
        userGlobalRoleResource.grantTenantRoleToUser(null, "userId", "tenantId", "roleId");
        verify(tenantService).getTenant("tenantId");
    }

    @Test(expected = BadRequestException.class)
    public void grantTenantRoleToUser_withInvalidTenantId_throwsBadRequestException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getAccessTokenByAuthHeader(null)).thenReturn(scopeAccess);
        doNothing().when(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);
        when(userService.loadUser("userId")).thenReturn(new User());
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        when(tenantService.getTenant("tenantId")).thenReturn(null);
        userGlobalRoleResource.grantTenantRoleToUser(null, "userId", "tenantId", "roleId");
    }

    @Test
    public void grantGlobalRoleToUser_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        userGlobalRoleResource.grantGlobalRoleToUser("authHeader", "userId", "roleId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void grantGlobalRoleToUser_callsUserService_loadUser() throws Exception {
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        userGlobalRoleResource.grantGlobalRoleToUser("authHeader", "userId", "roleId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void grantGlobalRoleToUser_callsApplicationService_getClientRoleById() throws Exception {
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        userGlobalRoleResource.grantGlobalRoleToUser("authHeader", "userId", "roleId");
        verify(applicationService).getClientRoleById("roleId");
    }

    @Test (expected = BadRequestException.class)
    public void grantGlobalRoleToUser_roleIsNull_throwsBadRequest() throws Exception {
        userGlobalRoleResource.grantGlobalRoleToUser("authHeader", "userId", "roleId");
    }

    @Test (expected = BadRequestException.class)
    public void grantGlobalRoleToUser_roleAlreadyPresent_throwsBadRequest() throws Exception {
        ClientRole cRole = new ClientRole();
        cRole.setClientId("123456");
        cRole.setId("123456");

        cRole.setName("default");

        TenantRole tRole = new TenantRole();
        tRole.setClientId("123456");
        tRole.setName("default");
        tRole.setRoleRsId("123456");
        tRole.setUserId("userId");

        when(applicationService.getClientRoleById("roleId")).thenReturn(cRole);
        when(userService.loadUser(anyString())).thenReturn(new User());
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRoleList.add(tRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(tenantRoleList);
        userGlobalRoleResource.grantGlobalRoleToUser("authHeader", "userId", "roleId");
    }

    @Test
    public void grantGlobalRoleToUser_callsTenantService_addTenantRoleToUser() throws Exception {
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        userGlobalRoleResource.grantGlobalRoleToUser("authHeader", "userId", "roleId");
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void grantGlobalRoleToUser_responseNoContent_returns204() throws Exception {
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        Response response = userGlobalRoleResource.grantGlobalRoleToUser("authHeader", "userId", "roleId");
        assertThat("response code", response.getStatus(), equalTo(204));
    }

    @Test
    public void deleteGlobalRoleFromUser_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        when(userService.loadUser("userId")).thenReturn(new User());
        userGlobalRoleResource.deleteGlobalRoleFromUser("authHeader", "userId", "roleId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void deleteGlobalRoleFromUser_callsUserService_loadUser() throws Exception {
        when(userService.loadUser("userId")).thenReturn(new User());
        userGlobalRoleResource.deleteGlobalRoleFromUser("authHeader", "userId", "roleId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void deleteGlobalRoleFromUser_callsTenantService_deleteTenantRole() throws Exception {
        when(userService.loadUser("userId")).thenReturn(new User());
        userGlobalRoleResource.deleteGlobalRoleFromUser("authHeader", "userId", "roleId");
        verify(tenantService).deleteTenantRole(anyString(), any(TenantRole.class));
    }

    @Test
    public void deleteGlobalRoleFromUser_responseNoContent_returns204() throws Exception {
        when(userService.loadUser("userId")).thenReturn(new User());
        Response response = userGlobalRoleResource.deleteGlobalRoleFromUser("authHeader", "userId", "roleId");
        assertThat("response code", response.getStatus(), equalTo(204));
    }

    @Test
    public void deleteTenantRoleFromUser_callsScopeAccessService_getAccessTokenByAuthHeader() throws Exception {
        when(userService.loadUser("userId")).thenReturn(new User());
        userGlobalRoleResource.deleteTenantRoleFromUser("authHeader", "userId", "tenantId", "roleId");
        verify(scopeAccessService).getAccessTokenByAuthHeader("authHeader");
    }

    @Test
    public void deleteTenantRoleFromUser_callsAuthorizationService_authorizaeIdmSuperAdminOrRackspaceClient() throws Exception {
        when(userService.loadUser("userId")).thenReturn(new User());
        userGlobalRoleResource.deleteTenantRoleFromUser("authHeader", "userId", "tenantId", "roleId");
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(any(ScopeAccess.class));
    }

    @Test
    public void deleteTenantRoleFromUser_callsUserService_loadUser() throws Exception {
        when(userService.loadUser("userId")).thenReturn(new User());
        userGlobalRoleResource.deleteTenantRoleFromUser("authHeader", "userId", "tenantId", "roleId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void deleteTenantRoleFromUser_callsTenantService_deleteTenantRole() throws Exception {
        when(userService.loadUser("userId")).thenReturn(new User());
        userGlobalRoleResource.deleteTenantRoleFromUser("authHeader", "userId", "tenantId", "roleId");
        verify(tenantService).deleteTenantRole(anyString(), any(TenantRole.class));
    }

    @Test
    public void deleteTenantRoleFromUser_responseNoContent_returns204() throws Exception {
        when(userService.loadUser("userId")).thenReturn(new User());
        Response response = userGlobalRoleResource.deleteTenantRoleFromUser("authHeader", "userId", "tenantId", "roleId");
        assertThat("response code", response.getStatus(), equalTo(204));
    }

    @Test
    public void createTenantRole_callsApplicationService_getClientRoleById() throws Exception {
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        userGlobalRoleResource.createTenantRole("tenantId", "roleId");
        verify(applicationService).getClientRoleById("roleId");
    }

    @Test (expected = BadRequestException.class)
    public void createTenantRole_roleIsNull_throwsBadRequest() throws Exception {
        userGlobalRoleResource.createTenantRole("tenantId", "roleId");
    }

    @Test
    public void createTenantRole_roleNotNull_returnsTenantRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setClientId("clientId");
        clientRole.setId("id");
        clientRole.setName("name");
        when(applicationService.getClientRoleById("roleId")).thenReturn(clientRole);
        TenantRole tenantRole = userGlobalRoleResource.createTenantRole("tenantId", "roleId");
        assertThat("client id", tenantRole.getClientId(), equalTo("clientId"));
        assertThat("id", tenantRole.getRoleRsId(), equalTo("id"));
        assertThat("name", tenantRole.getName(), equalTo("name"));
    }
}
