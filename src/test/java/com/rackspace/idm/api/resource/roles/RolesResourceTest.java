package com.rackspace.idm.api.resource.roles;

import com.rackspace.api.idm.v1.Role;
import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.api.resource.pagination.Paginator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
    Configuration config;
    Paginator<ClientRole> clientRolePaginator;
    UriInfo uriInfo;
    ScopeAccessService scopeAccessService;


    @Before
    public void setUp() throws Exception {
        authorizationService = mock(AuthorizationService.class);
        applicationService = mock(ApplicationService.class);
        rolesConverter = mock(RolesConverter.class);
        rolesResource = new RolesResource(rolesConverter,authorizationService,applicationService,null);
        spy = spy(rolesResource);
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        when(rolesConverter.toClientRole(any(Role.class))).thenReturn(new ClientRole());
        config = mock(Configuration.class);
        clientRolePaginator = mock(Paginator.class);
        scopeAccessService = mock(ScopeAccessService.class);
        rolesResource.setConfig(config);
        rolesResource.setPaginator(clientRolePaginator);
        rolesResource.setScopeAccessService(scopeAccessService);
        uriInfo = mock(UriInfo.class);

        when(applicationService.getClientRolesPaged(anyString(), anyString(), anyInt(), anyInt())).thenReturn(new PaginatorContext<ClientRole>());

    }

    @Test
    public void getAllRoles_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        rolesResource.getAllRoles(null, uriInfo, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void getAllRoles_callsApplicationService_getClientRolesPaged() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        rolesResource.getAllRoles(null, null, null, null);
        verify(applicationService).getClientRolesPaged(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    public void getAllRoles_returns200Status() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        Response response = rolesResource.getAllRoles(null, null, null, null);
        assertThat("response status", response.getStatus(), equalTo(200));
    }

    @Test(expected = BadRequestException.class)
    public void updateRole_withNullApplication_throwsBadRequestException() throws Exception {
        Role role = validRole();
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        doReturn(mock(ScopeAccess.class)).when(scopeAccessService).getScopeAccessByAccessToken(anyString());
        when(applicationService.getById("appId")).thenReturn(null);
        rolesResource.updateRole(null,"foo", role);
        verify(applicationService).getById("appId");
    }

    @Test(expected = NotFoundException.class)
    public void updateRole_invalidRoleId_throwsNotFoundException() throws Exception {
        when(applicationService.getClientRoleById("id")).thenReturn(null);
        doReturn(mock(ScopeAccess.class)).when(scopeAccessService).getScopeAccessByAccessToken(anyString());
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

    @Test(expected = NotFoundException.class)
    public void deleteRole_withNullClientId_throwsNotFoundException() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.getClientRoleById("roleId")).thenReturn(null);
        doReturn(mock(ScopeAccess.class)).when(scopeAccessService).getScopeAccessByAccessToken(anyString());
        rolesResource.deleteRole(null, "roleId");
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
