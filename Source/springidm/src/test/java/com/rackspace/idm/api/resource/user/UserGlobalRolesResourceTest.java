package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 2:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserGlobalRolesResourceTest {
    private UserGlobalRolesResource userGlobalRolesResource;
    private AuthorizationService authorizationService;
    private UserService userService;
    private TenantService tenantService;
    private UserGlobalRoleResource roleResource;
    private RolesConverter rolesConverter;

    @Before
    public void setUp() throws Exception {
        authorizationService = mock(AuthorizationService.class);
        userService = mock(UserService.class);
        tenantService = mock(TenantService.class);
        roleResource = mock(UserGlobalRoleResource.class);
        rolesConverter = mock(RolesConverter.class);
        userGlobalRolesResource = new UserGlobalRolesResource(userService, authorizationService, tenantService, roleResource, rolesConverter);
    }

    @Test
    public void getRoles_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        userGlobalRolesResource.getRoles("authHeader", "userId", "applicationId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getRoles_callsUserService_loadUser() throws Exception {
        userGlobalRolesResource.getRoles("authHeader", "userId", "applicationId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void getRoles_applicationIdIsBlank_createsNewFilterReturnsOk() throws Exception {
        Response response = userGlobalRolesResource.getRoles("authHeader", "userId", "");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void getRoles_callsTenantService_getGlobalRolesForUser() throws Exception {
        userGlobalRolesResource.getRoles("authHeader", "userId", "applicationId");
        verify(tenantService).getGlobalRolesForUser(any(User.class), any(FilterParam[].class));
    }

    @Test
    public void getRoleResource_returnsRoleResource() throws Exception {
        UserGlobalRoleResource resource = userGlobalRolesResource.getRoleResource();
        assertThat("roles resource", resource, equalTo(roleResource));
    }
}
