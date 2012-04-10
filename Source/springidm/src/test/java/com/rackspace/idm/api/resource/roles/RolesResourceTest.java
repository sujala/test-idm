package com.rackspace.idm.api.resource.roles;

import com.rackspace.api.idm.v1.Role;
import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/10/12
 * Time: 10:53 AM
 */
public class RolesResourceTest {

    RolesResource rolesResource;
    AuthorizationService authorizationService;
    ApplicationService applicationService;
    RolesConverter rolesConverter;

    @Before
    public void setUp() throws Exception {
        authorizationService = mock(AuthorizationService.class);
        applicationService = mock(ApplicationService.class);
        rolesConverter = mock(RolesConverter.class);
        rolesResource = new RolesResource(rolesConverter,authorizationService,applicationService,null);
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        when(rolesConverter.toClientRole(any(Role.class))).thenReturn(new ClientRole());
    }

    @Test
    public void updateRole_callsApplicationService_getById() throws Exception {
        Role role = validRole();
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.getById("appId")).thenReturn(new Application());
        rolesResource.updateRole(null,"foo", role);
        verify(applicationService).getById("appId");
    }

    @Test(expected = BadRequestException.class)
    public void updateRole_withNullApplication_throwsBadRequestException() throws Exception {
        Role role = validRole();
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.getById("appId")).thenReturn(null);
        rolesResource.updateRole(null,"foo", role);
        verify(applicationService).getById("appId");
    }

    @Test(expected = NotFoundException.class)
    public void updateRole_invalidRoleId_throwsNotFoundException() throws Exception {
        when(applicationService.getClientRoleById("id")).thenReturn(null);
        when(applicationService.getById("appId")).thenReturn(new Application());
        rolesResource.updateRole(null, "id", validRole());
    }

    private Role validRole() {
        Role role = new Role();
        role.setName("roleName");
        role.setId("foo");
        role.setApplicationId("appId");
        return role;
    }
}
