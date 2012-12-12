package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 1:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserTenantsResourceTest {
    private UserTenantsResource userTenantsResource;
    private UserService userService;
    private AuthorizationService authorizationService;
    private RolesConverter rolesConverter;
    private TenantService tenantService;
    private InputValidator inputValidator;

    @Before
    public void setUp() throws Exception {
        userService = mock(UserService.class);
        authorizationService = mock(AuthorizationService.class);
        rolesConverter = mock(RolesConverter.class);
        tenantService = mock(TenantService.class);
        inputValidator = mock(InputValidator.class);
        userTenantsResource = new UserTenantsResource(userService, authorizationService, rolesConverter, tenantService, inputValidator);
    }

    @Test
    public void getAllTenantRolesForUser_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        userTenantsResource.getAllTenantRolesForUser("authHeader", "userId", "applicaionId", "tenantId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getAllTenantRolesForUser_callsUserService_loadUser() throws Exception {
        userTenantsResource.getAllTenantRolesForUser("authHeader", "userId", "applicationId", "tenantId");
        verify(userService).loadUser("userId");
    }

    @Test
    public void getAllTenantRolesForUser_callsTenantService_getTenantRolesForUser() throws Exception {
        userTenantsResource.getAllTenantRolesForUser("authHeader", "userId", "applicationId", "tenantId");
        verify(tenantService).getTenantRolesForUser(any(User.class), anyString(), anyString());
    }

    @Test
    public void getAllTenantRolesForUser_callsRolesConverter_toRoleJaxbFromTenantRole() throws Exception {
        userTenantsResource.getAllTenantRolesForUser("authHeader", "userId", "applicationId", "tenantId");
        verify(rolesConverter).toRoleJaxbFromTenantRole(any(ArrayList.class));
    }

    @Test
    public void getAllTenantRolesForUser_responseOk_returns200() throws Exception {
        Response response = userTenantsResource.getAllTenantRolesForUser("authHeader", "userId", "applicationId", "tenantId");
        MatcherAssert.assertThat("respone code", response.getStatus(), Matchers.equalTo(200));
    }

}
