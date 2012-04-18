package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

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
}
