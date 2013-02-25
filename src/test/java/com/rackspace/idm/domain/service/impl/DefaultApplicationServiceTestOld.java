package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/3/12
 * Time: 1:52 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultApplicationServiceTestOld {

    @InjectMocks
    private DefaultApplicationService defaultApplicationService = new DefaultApplicationService();
    @Mock
    private ApplicationDao applicationDao;
    @Mock
    ApplicationRoleDao applicationRoleDao;
    @Mock
    private CustomerService customerService;
    @Mock
    private ScopeAccessService scopeAccessService;
    @Mock
    private TenantService tenantService;

    private DefaultApplicationService spy;

    @Before
    public void setUp() throws Exception {
        spy = spy(defaultApplicationService);
    }

    @Test (expected = IllegalStateException.class)
    public void addDefinedPermission_customerIsNull_throwsIllegalStateException() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        when(customerService.getCustomer(anyString())).thenReturn(null);
        defaultApplicationService.addDefinedPermission(definedPermission);
    }

    @Test (expected = IllegalStateException.class)
    public void addDefinedPermission_clientIsNull_throwsIllegalStateException() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        when(customerService.getCustomer(anyString())).thenReturn(new Customer());
        when(applicationDao.getClientByClientId(anyString())).thenReturn(null);
        defaultApplicationService.addDefinedPermission(definedPermission);
    }

    @Test
    public void getAllApplications_callsClientDao_getAllClients() throws Exception {
        defaultApplicationService.getAllApplications(null, 0, 0);
        verify(applicationDao).getAllClients(null, 0, 0);
    }

    @Test
    public void checkAndGetApplication_applicationNotNull_returnsApplication() throws Exception {
        Application application = new Application();
        doReturn(application).when(spy).getById("applicationId");
        assertThat("application",spy.checkAndGetApplication("applicationId"),equalTo(application));
    }

    @Test
    public void checkAndGetApplication_applicationNull_throwsNotFoundException() throws Exception {
        try{
            doReturn(null).when(spy).getById("applicationId");
            spy.checkAndGetApplication("applicationId");
            assertTrue("should throw exception",false);
        } catch (NotFoundException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Service applicationId not found"));
        }
    }

    @Test
    public void getClientByScope_callsClientDao_getClientByScope() throws Exception {
        defaultApplicationService.getClientByScope(null);
        verify(applicationDao).getClientByScope(null);
    }

    @Test (expected = NotFoundException.class)
    public void getDefinedPermissionByClientIdAndPermissionId_clientIdNotFound_throwsNotFoundException() throws Exception {
        defaultApplicationService.getDefinedPermissionByClientIdAndPermissionId("clientId", "permissionId");
    }

    @Test
    public void getAvailableScopes_callsClientDAO_getAvailableScopes() throws Exception {
        defaultApplicationService.getAvailableScopes();
        verify(applicationDao).getAvailableScopes();
    }

    @Test(expected = NotFoundException.class)
    public void getAvailableScopes_throwsNotFoundException_whenNotScopesWhereFound() throws Exception {
        when(applicationDao.getAvailableScopes()).thenReturn(null);
        defaultApplicationService.getAvailableScopes();
    }

    @Test (expected = NotFoundException.class)
    public void checkAndGetPermission_permissionIsNull_throwsNotFoundException() throws Exception {
        when(applicationDao.getClientByClientId(null)).thenReturn(new Application());
        defaultApplicationService.checkAndGetPermission(null, null, null);
    }

    @Test (expected = NotFoundException.class)
    public void checkAndGetPermission_customerIdDoesNotMatch_throwsNotFoundException() throws Exception {
        Application client = new Application();
        client.setRCN("rcn");
        client.setClientId("clientId");
        DefinedPermission permission = new DefinedPermission();
        permission.setClientId("clientId");
        permission.setCustomerId("customerId");
        permission.setPermissionId("permissionId");
        when(scopeAccessService.getPermissionForParent(anyString(), any(Permission.class))).thenReturn(permission);
        when(applicationDao.getClientByClientId(anyString())).thenReturn(client);
        defaultApplicationService.checkAndGetPermission("notMatch", "clientId", "permissionId");
    }

    @Test (expected = NotFoundException.class)
    public void checkAndGetPermission_clientIdDoesNotMatch_throwsNotFoundException() throws Exception {
        Application client = new Application();
        client.setRCN("rcn");
        client.setClientId("clientId");
        DefinedPermission permission = new DefinedPermission();
        permission.setClientId("clientId");
        permission.setCustomerId("customerId");
        permission.setPermissionId("permissionId");
        when(scopeAccessService.getPermissionForParent(anyString(), any(Permission.class))).thenReturn(permission);
        when(applicationDao.getClientByClientId(anyString())).thenReturn(client);
        defaultApplicationService.checkAndGetPermission("customerId", "notMatch", "permissionId");
    }

    @Test (expected = NotFoundException.class)
    public void checkAndGetPermission_permissionNotEnabled_throwsNotFoundException() throws Exception {
        Application client = new Application();
        client.setRCN("rcn");
        client.setClientId("clientId");
        DefinedPermission permission = new DefinedPermission();
        permission.setClientId("clientId");
        permission.setCustomerId("customerId");
        permission.setPermissionId("permissionId");
        permission.setEnabled(false);
        when(scopeAccessService.getPermissionForParent(anyString(), any(Permission.class))).thenReturn(permission);
        when(applicationDao.getClientByClientId(anyString())).thenReturn(client);
        defaultApplicationService.checkAndGetPermission("customerId", "clientId", "permissionId");
    }

    @Test
    public void checkAndGetPermission_permissionCreated_returnsCorrectly() throws Exception {
        Application client = new Application();
        client.setRCN("rcn");
        client.setClientId("clientId");
        DefinedPermission permission = new DefinedPermission();
        permission.setClientId("clientId");
        permission.setCustomerId("customerId");
        permission.setPermissionId("permissionId");
        permission.setEnabled(true);
        when(scopeAccessService.getPermissionForParent(anyString(), any(Permission.class))).thenReturn(permission);
        when(applicationDao.getClientByClientId(anyString())).thenReturn(client);
        DefinedPermission definedPermission = defaultApplicationService.checkAndGetPermission("customerId", "clientId", "permissionId");
        assertThat("permission id", definedPermission.getPermissionId(), equalTo("permissionId"));
    }

    @Test
    public void updateClientRole_callsClientDao_updateClientRole() throws Exception {
        defaultApplicationService.updateClientRole(null);
        verify(applicationRoleDao).updateClientRole(null);
    }

    @Test
    public void getClientRolesByClientId_callsClientRoleDao_getClientRoles() throws Exception {
        List<ClientRole> roles = new ArrayList<ClientRole>();
        Application application = new Application();
        when(applicationDao.getClientByClientId("clientId")).thenReturn(application);
        defaultApplicationService.getClientRolesByClientId("clientId");
        verify(applicationRoleDao).getClientRolesForApplication(application);
    }

    @Test (expected = NotFoundException.class)
    public void getClientRolesByClientId_throwsNotFoundException() throws Exception {
        when(applicationDao.getClientByClientId("blah")).thenReturn(null);
        defaultApplicationService.getClientRolesByClientId("blah");
    }

    @Test
    public void getAllClientRoles_callsClientDao_getAllClientRoles() throws Exception {
        List<ClientRole> roles = new ArrayList<ClientRole>();
        when(applicationDao.getAllClientRoles(null)).thenReturn(roles);
        defaultApplicationService.getAllClientRoles();
    }

    @Test
    public void getClientRoleByClientIdAndRoleName_callsClientDao_getClientRoleByClientIdAndRoleName() throws Exception {
        when(applicationDao.getClientRoleByClientIdAndRoleName("clientId", "roleName")).thenReturn(new ClientRole());
        defaultApplicationService.getClientRoleByClientIdAndRoleName("clientId", "roleName");
        verify(applicationRoleDao).getClientRoleByApplicationAndName("clientId", "roleName");
    }

    @Test
    public void getClientRoleById_callsClientDao_getClientRoleById() throws Exception {
        when(applicationDao.getClientRoleById("id")).thenReturn(new ClientRole());
        defaultApplicationService.getClientRoleById("id");
        verify(applicationRoleDao).getClientRole("id");
    }

    @Test
    public void getOpenStackService_callsClientDao_getOpenStackService() throws Exception {
        when(applicationDao.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        defaultApplicationService.getOpenStackServices();
        verify(applicationDao).getOpenStackServices();
    }

    @Test
    public void updateClient_callsClientDao_updateClient() throws Exception {
        Application client = new Application();
        defaultApplicationService.updateClient(client);
        verify(applicationDao).updateClient(client);
    }

    @Test(expected = NotFoundException.class)
    public void addClientRole_throwsNotFoundException_whenClientIsNotFound() throws Exception {
        ClientRole role = new ClientRole();
        role.setClientId("clientId");
        when(applicationDao.getClientByClientId("clientId")).thenReturn(null);
        defaultApplicationService.addClientRole(role);
    }

    @Test(expected = DuplicateException.class)
    public void addClientRole_throwsDuplicateException_whenRoleIsNotFound() throws Exception {
        ClientRole role = new ClientRole();
        Application application = new Application();
        role.setClientId("clientId");
        role.setName("role");
        when(applicationDao.getClientByClientId("clientId")).thenReturn(application);
        when(applicationRoleDao.getClientRoleByApplicationAndName(application, role)).thenReturn(new ClientRole());
        defaultApplicationService.addClientRole(role);
    }

    @Test
    public void addClientRole_callsClientDao_addClientRole() throws Exception {
        ClientRole role = new ClientRole();
        Application application = new Application();
        role.setClientId("clientId");
        role.setName("role");
        when(applicationDao.getClientByClientId("clientId")).thenReturn(application);
        when(applicationRoleDao.getClientRoleByApplicationAndName(application, role)).thenReturn(null);
        defaultApplicationService.addClientRole(role);
        verify(applicationRoleDao).addClientRole(any(Application.class), eq(role));
    }

    @Test (expected = NotFoundException.class)
    public void loadApplication_clientIsNull_throwsNotFoundException() throws Exception {
        when(applicationDao.getClientByClientId(null)).thenReturn(null);
        defaultApplicationService.loadApplication(null);
    }

    @Test
    public void loadApplication_clientFound_returnsApplicationClient() throws Exception {
        Application client = new Application();
        client.setClientId("correctClientId");
        when(applicationDao.getClientByClientId("clientId")).thenReturn(client);
        Application applicationClient = defaultApplicationService.loadApplication("clientId");
        assertThat("client id", applicationClient.getClientId(), equalTo("correctClientId"));
    }

    @Test
    public void deleteClientRole_callsTenantDao_deleteTenantRole() throws Exception {
        ClientRole role = new ClientRole();
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRoles.add(tenantRole);
        when(tenantService.getTenantRolesForClientRole(role)).thenReturn(tenantRoles);
        defaultApplicationService.deleteClientRole(role);
        verify(tenantService).deleteTenantRole(tenantRole);
    }

    @Test
    public void deleteClientRole_callsClientDao_deleteClientRole() throws Exception {
        ClientRole role = new ClientRole();
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        when(tenantService.getTenantRolesForClientRole(role)).thenReturn(tenantRoles);
        defaultApplicationService.deleteClientRole(role);
        verify(applicationRoleDao).deleteClientRole(role);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientServices_clientIsNull_throwsIllegalArgumentException() throws Exception {
        Application client = mock(Application.class);
        defaultApplicationService.getClientServices(client);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientServices_clientUniqueIdIsNull_throwsIllegalArgumentException() throws Exception {
        defaultApplicationService.getClientServices(new Application());
    }

    @Test (expected = NotFoundException.class)
    public void delete_clientIdIsNull_throwsNotFoundException() throws Exception {
        defaultApplicationService.delete(null);
    }

    @Test
    public void delete_callsDeleteDefinedPermission() throws Exception {
        Application client = new Application();
        DefinedPermission definedPermission = new DefinedPermission();
        List<DefinedPermission> definedPermissionList = new ArrayList<DefinedPermission>();
        definedPermissionList.add(definedPermission);
        when(applicationDao.getClientByClientId("clientId")).thenReturn(client);
        doReturn(definedPermissionList).when(spy).getDefinedPermissionsByClient(client);
        spy.delete("clientId");
        verify(spy).deleteDefinedPermission(definedPermission);
    }

    @Test
    public void delete_callsDeleteClientRole() throws Exception {
        Application client = new Application();
        ClientRole clientRole = new ClientRole();
        List<ClientRole> clientRoleList = new ArrayList<ClientRole>();
        clientRoleList.add(clientRole);
        when(applicationDao.getClientByClientId("clientId")).thenReturn(client);
        when(applicationDao.getClientRolesByClientId("clientId")).thenReturn(clientRoleList);
        spy.delete("clientId");
        verify(spy).deleteClientRole(clientRole);
    }
}
