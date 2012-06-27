package com.rackspace.idm.api.resource.roles;

import com.rackspace.api.idm.v1.Role;
import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.Response;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
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
    RolesResource spy;

    @Before
    public void setUp() throws Exception {
        authorizationService = mock(AuthorizationService.class);
        applicationService = mock(ApplicationService.class);
        rolesConverter = mock(RolesConverter.class);
        rolesResource = new RolesResource(rolesConverter,authorizationService,applicationService,null);
        spy = spy(rolesResource);
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        when(rolesConverter.toClientRole(any(Role.class))).thenReturn(new ClientRole());
    }

    @Test
    public void getAllRoles_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        rolesResource.getAllRoles(null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void getAllRoles_callsApplicationService_getAllClientRoles() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        rolesResource.getAllRoles(null, null, null);
        verify(applicationService).getAllClientRoles(anyList());
    }

    @Test
    public void getAllRoles_withApplicationId_addsApplicationIdFilter() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        rolesResource.getAllRoles(null, null, "applicationId");
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(applicationService).getAllClientRoles(argumentCaptor.capture());
        assertThat("FilterParam", ((FilterParam)argumentCaptor.getValue().get(0)).getParam(), equalTo(FilterParam.FilterParamName.APPLICATION_ID));
    }

    @Test
    public void getAllRoles_withName_addsNameFilter() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        rolesResource.getAllRoles(null, "name", null);
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(applicationService).getAllClientRoles(argumentCaptor.capture());
        assertThat("FilterParam", ((FilterParam)argumentCaptor.getValue().get(0)).getParam(), equalTo(FilterParam.FilterParamName.ROLE_NAME));
    }

    @Test
    public void getAllRoles_returns200Status() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        Response response = rolesResource.getAllRoles(null, null, null);
        assertThat("response status", response.getStatus(), equalTo(200));
    }

    @Test
    public void addRole_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        doNothing().when(spy).validateRole(null);
        ClientRole clientRole = new ClientRole();
        clientRole.setId("clientRoleId");
        when(rolesConverter.toClientRole(any(Role.class))).thenReturn(clientRole);
        spy.addRole(null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void addRole_callsApplicationService_addClientRole() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        doNothing().when(spy).validateRole(null);
        ClientRole clientRole = new ClientRole();
        clientRole.setId("clientRoleId");
        when(rolesConverter.toClientRole(any(Role.class))).thenReturn(clientRole);
        spy.addRole(null, null);
        verify(applicationService).addClientRole(clientRole);
    }

    @Test
    public void addRole_callsRolesConverter_toClientRole() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        doNothing().when(spy).validateRole(null);
        ClientRole clientRole = new ClientRole();
        clientRole.setId("clientRoleId");
        when(rolesConverter.toClientRole(any(Role.class))).thenReturn(clientRole);
        spy.addRole(null, null);
        verify(rolesConverter).toClientRole(any(Role.class));
    }

    @Test
    public void addRole_returnsCreatedStatus() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        doNothing().when(spy).validateRole(null);
        ClientRole clientRole = new ClientRole();
        clientRole.setId("clientRoleId");
        when(rolesConverter.toClientRole(any(Role.class))).thenReturn(clientRole);
        Response response = spy.addRole(null, null);
        assertThat("response status", response.getStatus(), equalTo(201));
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

    @Test(expected = NotFoundException.class)
    public void getRole_invalidRole_throwsNotFoundException() throws Exception {
        when(applicationService.getClientRoleById("id")).thenReturn(null);
        rolesResource.getRole(null,"id");
    }

    @Test
    public void getRole_callsApplicationService_getClientRoleById() throws Exception {
        rolesResource.getRole(null,"id");
        verify(applicationService).getClientRoleById("id");
    }

    @Test
    public void deleteRole_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        rolesResource.deleteRole(null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void deleteRole_callsApplicationService_getClientRoleById() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        rolesResource.deleteRole(null, "roleId");
        verify(applicationService).getClientRoleById("roleId");
    }

    @Test(expected = NotFoundException.class)
    public void deleteRole_withNullClientId_throwsNotFoundException() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.getClientRoleById("roleId")).thenReturn(null);
        rolesResource.deleteRole(null, "roleId");
    }

    @Test
    public void deleteRole_callsApplicationService_deleteClientRole() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        rolesResource.deleteRole(null, "roleId");
        verify(applicationService).deleteClientRole(any(ClientRole.class));
    }

    @Test
    public void deleteRole_returnsNoContentStatus() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.getClientRoleById("roleId")).thenReturn(new ClientRole());
        Response response = rolesResource.deleteRole(null, "roleId");
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test(expected = BadRequestException.class)
    public void validateRole_nullRole_throwsBadRequestException() throws Exception {
        rolesResource.validateRole(null);
    }

    @Test(expected = BadRequestException.class)
    public void validateRole_nullRoleName_throwsBadRequestException() throws Exception {
        Role role = validRole();
        role.setName(null);
        rolesResource.validateRole(role);
    }

    @Test(expected = BadRequestException.class)
    public void validateRole_emptyStringRoleName_throwsBadRequestException() throws Exception {
        Role role = validRole();
        role.setName("");
        rolesResource.validateRole(role);
    }

    @Test(expected = BadRequestException.class)
    public void validateRole_blankStringRoleName_throwsBadRequestException() throws Exception {
        Role role = validRole();
        role.setName(" ");
        rolesResource.validateRole(role);
    }

    @Test(expected = BadRequestException.class)
    public void validateRole_withNullApplicationId_throwsBadRequestException() throws Exception {
        Role role = validRole();
        role.setApplicationId(null);
        rolesResource.validateRole(role);
    }

    @Test(expected = BadRequestException.class)
    public void validateRole_withEmptyApplicationId_throwsBadRequestException() throws Exception {
        Role role = validRole();
        role.setApplicationId("");
        rolesResource.validateRole(role);
    }

    private Role validRole() {
        Role role = new Role();
        role.setName("roleName");
        role.setId("foo");
        role.setApplicationId("appId");
        return role;
    }
}
