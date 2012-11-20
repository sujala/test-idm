package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.UserDisabledException;
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
public class DefaultApplicationServiceTest {

    @InjectMocks
    private DefaultApplicationService defaultApplicationService = new DefaultApplicationService();
    @Mock
    private ApplicationDao clientDao;
    @Mock
    private CustomerDao customerDao;
    @Mock
    private UserDao userDao;
    @Mock
    private ScopeAccessDao scopeAccessDao;
    @Mock
    private TenantDao tenantDao;

    private DefaultApplicationService spy;
    @Mock
    ApplicationRoleDao applicationRoleDao;

    @Before
    public void setUp() throws Exception {
        spy = spy(defaultApplicationService);
    }

    @Test (expected = IllegalStateException.class)
    public void addDefinedPermission_customerIsNull_throwsIllegalStateException() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        when(customerDao.getCustomerByCustomerId(anyString())).thenReturn(null);
        defaultApplicationService.addDefinedPermission(definedPermission);
    }

    @Test (expected = IllegalStateException.class)
    public void addDefinedPermission_clientIsNull_throwsIllegalStateException() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        when(customerDao.getCustomerByCustomerId(anyString())).thenReturn(new Customer());
        when(clientDao.getClientByClientId(anyString())).thenReturn(null);
        defaultApplicationService.addDefinedPermission(definedPermission);
    }

    @Test
    public void addDefinedPermission_scopeAccessIsNull_callsAddDirectScopeAccess() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        Application client = new Application();
        client.setUniqueId("uniqueId");
        client.setClientId("clientId");
        client.setRCN("rcn");
        when(customerDao.getCustomerByCustomerId(anyString())).thenReturn(new Customer());
        when(clientDao.getClientByClientId(anyString())).thenReturn(client);
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        when(scopeAccessDao.addDirectScopeAccess(anyString(), any(ScopeAccess.class))).thenReturn(new ScopeAccess());
        defaultApplicationService.addDefinedPermission(definedPermission);
        verify(scopeAccessDao).addDirectScopeAccess(anyString(), any(ScopeAccess.class));
    }

    @Test (expected = DuplicateException.class)
    public void addDefinedPermission_permissionExists_throwsDuplicateException() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        Application client = new Application();
        client.setUniqueId("uniqueId");
        client.setClientId("clientId");
        client.setRCN("rcn");
        when(customerDao.getCustomerByCustomerId(anyString())).thenReturn(new Customer());
        when(clientDao.getClientByClientId(anyString())).thenReturn(client);
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ClientScopeAccess());
        when(scopeAccessDao.addDirectScopeAccess(anyString(), any(ScopeAccess.class))).thenReturn(new ScopeAccess());
        when(scopeAccessDao.getPermissionByParentAndPermission(anyString(), eq(definedPermission))).thenReturn(new DefinedPermission());
        defaultApplicationService.addDefinedPermission(definedPermission);
    }

    @Test
    public void addDefinedPermission_scopeAccessDao_callsDefinePermission() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        Application client = new Application();
        client.setUniqueId("uniqueId");
        client.setClientId("clientId");
        client.setRCN("rcn");
        when(customerDao.getCustomerByCustomerId(anyString())).thenReturn(new Customer());
        when(clientDao.getClientByClientId(anyString())).thenReturn(client);
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        when(scopeAccessDao.addDirectScopeAccess(anyString(), any(ScopeAccess.class))).thenReturn(new ScopeAccess());
        when(scopeAccessDao.getPermissionByParentAndPermission(anyString(), eq(definedPermission))).thenReturn(null);
        defaultApplicationService.addDefinedPermission(definedPermission);
        verify(scopeAccessDao).definePermission(anyString(), any(DefinedPermission.class));
    }

    @Test
    public void deleteDefinedPermission_scopeAccessDao_callsGetPermissionsByPermission() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setClientId("clientId");
        definedPermission.setCustomerId("customerId");
        definedPermission.setPermissionId("permissionId");
        when(scopeAccessDao.getPermissionsByPermission(definedPermission)).thenReturn(new ArrayList<Permission>());
        defaultApplicationService.deleteDefinedPermission(definedPermission);
        verify(scopeAccessDao).getPermissionsByPermission(definedPermission);
    }

    @Test
    public void deleteDefinedPermission_scopeAccessDao_callsRemovePermissionsFromScopeAccess() throws Exception {
        List<Permission> permissionList = new ArrayList<Permission>();
        Permission permission = new Permission();
        permissionList.add(permission);
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setClientId("clientId");
        definedPermission.setCustomerId("customerId");
        definedPermission.setPermissionId("permissionId");
        when(scopeAccessDao.getPermissionsByPermission(definedPermission)).thenReturn(permissionList);
        defaultApplicationService.deleteDefinedPermission(definedPermission);
        verify(scopeAccessDao).removePermissionFromScopeAccess(permission);
    }

    @Test
    public void getAllApplications_callsClientDao_getAllClients() throws Exception {
        defaultApplicationService.getAllApplications(null, 0, 0);
        verify(clientDao).getAllClients(null, 0, 0);
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
        verify(clientDao).getClientByScope(null);
    }

    @Test
    public void getDefinedPermissionByClientIdAndPermissionId_callsScopeAccessDao_getPermissionByParentAndPermission() throws Exception {
        Application client = new Application();
        client.setRCN("rcn");
        client.setClientId("clientId");
        client.setUniqueId("uniqueId");
        when(clientDao.getClientByClientId("clientId")).thenReturn(client);
        defaultApplicationService.getDefinedPermissionByClientIdAndPermissionId("clientId", "permissionId");
        verify(scopeAccessDao).getPermissionByParentAndPermission(anyString(), any(Permission.class));
    }

    @Test
    public void getDefinedPermissionByClientIdAndPermissionId_returnsPermission_succeeds() throws Exception {
        Application client = new Application();
        client.setRCN("rcn");
        client.setClientId("clientId");
        client.setUniqueId("uniqueId");
        when(clientDao.getClientByClientId("clientId")).thenReturn(client);
        when(scopeAccessDao.getPermissionByParentAndPermission(anyString(), any(Permission.class))).thenReturn(new DefinedPermission());
        DefinedPermission permission = defaultApplicationService.getDefinedPermissionByClientIdAndPermissionId("clientId", "permissionId");
        assertThat("permission", permission.toString(), equalTo("Permission [ldapEntry=null, permissionId=null, clientId=null, customerId=null]"));
    }

    @Test (expected = NotFoundException.class)
    public void getDefinedPermissionByClientIdAndPermissionId_clientIdNotFound_throwsNotFoundException() throws Exception {
        defaultApplicationService.getDefinedPermissionByClientIdAndPermissionId("clientId", "permissionId");
    }

    @Test
    public void updateDefinedPermission_callsScopeAccessDao_updatePermissionForScopeAccess() throws Exception {
        defaultApplicationService.updateDefinedPermission(null);
        verify(scopeAccessDao).updatePermissionForScopeAccess(null);
    }

    @Test
    public void getAvailableScopes_callsClientDAO_getAvailableScopes() throws Exception {
        defaultApplicationService.getAvailableScopes();
        verify(clientDao).getAvailableScopes();
    }

    @Test(expected = NotFoundException.class)
    public void getAvailableScopes_throwsNotFoundException_whenNotScopesWhereFound() throws Exception {
        when(clientDao.getAvailableScopes()).thenReturn(null);
        defaultApplicationService.getAvailableScopes();
    }

    @Test(expected = NotFoundException.class)
    public void addClientGroup_throwsNotFoundException_whenClientDoesNotExist() throws Exception {
        when(clientDao.getClientByClientId("id")).thenReturn(null);
        defaultApplicationService.addClientGroup(new ClientGroup("id", null, null, null));
    }

    @Test(expected = NotFoundException.class)
    public void addClientGroup_throwsNotFoundException_whenCustomerDoesNotExist() throws Exception {
        when(clientDao.getClientByClientId("id")).thenReturn(new Application());
        when(customerDao.getCustomerByCustomerId("id")).thenReturn(null);
        defaultApplicationService.addClientGroup(new ClientGroup("id", "id", null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteClientGroup_throwsIllegalArgumentException_whenClientIdIsNull() throws Exception {
        defaultApplicationService.deleteClientGroup("id", null, "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteClientGroup_throwsIllegalArgumentException_whenClientIdIsBlank() throws Exception {
        defaultApplicationService.deleteClientGroup("id", "", "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteClientGroup_throwsIllegalArgumentException_whenGroupNameNull() throws Exception {
        defaultApplicationService.deleteClientGroup("id", "id", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteClientGroup_throwsIllegalArgumentException_whenGroupNameIsBlank() throws Exception {
        defaultApplicationService.deleteClientGroup("id", "id", "");
    }

    @Test(expected = NotFoundException.class)
    public void deleteClientGroup_throwsNotFoundException_whenGroupIsNotFound() throws Exception {
        when(clientDao.getClientGroup("customerId", "clientId", "groupName")).thenReturn(null);
        defaultApplicationService.deleteClientGroup("customerId", "clientId", "groupName");
    }

    @Test (expected = NotFoundException.class)
    public void checkAndGetPermission_permissionIsNull_throwsNotFoundException() throws Exception {
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
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
        when(scopeAccessDao.getPermissionByParentAndPermission(anyString(), any(Permission.class))).thenReturn(permission);
        when(clientDao.getClientByClientId(anyString())).thenReturn(client);
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
        when(scopeAccessDao.getPermissionByParentAndPermission(anyString(), any(Permission.class))).thenReturn(permission);
        when(clientDao.getClientByClientId(anyString())).thenReturn(client);
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
        when(scopeAccessDao.getPermissionByParentAndPermission(anyString(), any(Permission.class))).thenReturn(permission);
        when(clientDao.getClientByClientId(anyString())).thenReturn(client);
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
        when(scopeAccessDao.getPermissionByParentAndPermission(anyString(), any(Permission.class))).thenReturn(permission);
        when(clientDao.getClientByClientId(anyString())).thenReturn(client);
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
        when(clientDao.getClientByClientId("clientId")).thenReturn(application);
        defaultApplicationService.getClientRolesByClientId("clientId");
        verify(applicationRoleDao).getClientRolesForApplication(application);
    }

    @Test (expected = NotFoundException.class)
    public void getClientRolesByClientId_throwsNotFoundException() throws Exception {
        when(clientDao.getClientByClientId("blah")).thenReturn(null);
        defaultApplicationService.getClientRolesByClientId("blah");
    }

    @Test
    public void getClientRolesPaged_callsApplicationRoleDao_getPagedUser() throws Exception {
        when(applicationRoleDao.getClientRolesPaged(anyString(),anyString(), anyInt(), anyInt())).thenReturn(new PaginatorContext<ClientRole>());
        when(applicationRoleDao.getClientRolesPaged(anyString(), anyInt(), anyInt())).thenReturn(new PaginatorContext<ClientRole>());
        when(applicationRoleDao.getClientRolesPaged(anyInt(), anyInt())).thenReturn(new PaginatorContext<ClientRole>());
        defaultApplicationService.getClientRolesPaged("applicationId", "roleName", 0, 10);
        defaultApplicationService.getClientRolesPaged("applicationId", 0, 10);
        defaultApplicationService.getClientRolesPaged(0, 10);
        verify(applicationRoleDao).getClientRolesPaged(anyString(), anyString(), anyInt(), anyInt());
        verify(applicationRoleDao).getClientRolesPaged(anyString(), anyInt(), anyInt());
        verify(applicationRoleDao).getClientRolesPaged(anyInt(), anyInt());
    }

    @Test
    public void getAllClientRoles_callsClientDao_getAllClientRoles() throws Exception {
        List<ClientRole> roles = new ArrayList<ClientRole>();
        when(clientDao.getAllClientRoles(null)).thenReturn(roles);
        defaultApplicationService.getAllClientRoles();
    }

    @Test
    public void getClientRoleByClientIdAndRoleName_callsClientDao_getClientRoleByClientIdAndRoleName() throws Exception {
        when(clientDao.getClientRoleByClientIdAndRoleName("clientId", "roleName")).thenReturn(new ClientRole());
        defaultApplicationService.getClientRoleByClientIdAndRoleName("clientId", "roleName");
        verify(applicationRoleDao).getClientRoleByApplicationAndName("clientId", "roleName");
    }

    @Test
    public void getClientRoleById_callsClientDao_getClientRoleById() throws Exception {
        when(clientDao.getClientRoleById("id")).thenReturn(new ClientRole());
        defaultApplicationService.getClientRoleById("id");
        verify(applicationRoleDao).getClientRole("id");
    }

    @Test
    public void getOpenStackService_callsClientDao_getOpenStackService() throws Exception {
        when(clientDao.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        defaultApplicationService.getOpenStackServices();
        verify(clientDao).getOpenStackServices();
    }

    @Test(expected = NotFoundException.class)
    public void removeUserFromClientGroup_throwsNotFoundException_whenCustomerDoesNotExist() throws Exception {
        ClientGroup clientGroup = new ClientGroup("clientId", "customerId", "name", "type");
        when(userDao.getUserByUsername("username")).thenReturn(new User());
        when(customerDao.getCustomerByCustomerId("customerId")).thenReturn(null);
        defaultApplicationService.removeUserFromClientGroup("username", clientGroup);
    }

    @Test
    public void updateClient_callsClientDao_updateClient() throws Exception {
        Application client = new Application();
        defaultApplicationService.updateClient(client);
        verify(clientDao).updateClient(client);
    }

    @Test(expected = NotFoundException.class)
    public void addClientRole_throwsNotFoundException_whenClientIsNotFound() throws Exception {
        ClientRole role = new ClientRole();
        role.setClientId("clientId");
        when(clientDao.getClientByClientId("clientId")).thenReturn(null);
        defaultApplicationService.addClientRole(role);
    }

    @Test(expected = DuplicateException.class)
    public void addClientRole_throwsDuplicateException_whenRoleIsNotFound() throws Exception {
        ClientRole role = new ClientRole();
        Application application = new Application();
        role.setClientId("clientId");
        role.setName("role");
        when(clientDao.getClientByClientId("clientId")).thenReturn(application);
        when(applicationRoleDao.getClientRoleByApplicationAndName(application, role)).thenReturn(new ClientRole());
        defaultApplicationService.addClientRole(role);
    }

    @Test
    public void addClientRole_callsClientDao_addClientRole() throws Exception {
        ClientRole role = new ClientRole();
        Application application = new Application();
        role.setClientId("clientId");
        role.setName("role");
        when(clientDao.getClientByClientId("clientId")).thenReturn(application);
        when(applicationRoleDao.getClientRoleByApplicationAndName(application, role)).thenReturn(null);
        defaultApplicationService.addClientRole(role);
        verify(applicationRoleDao).addClientRole(any(Application.class), eq(role));
    }

    @Test (expected = NotFoundException.class)
    public void loadApplication_clientIsNull_throwsNotFoundException() throws Exception {
        when(clientDao.getClientByClientId(null)).thenReturn(null);
        defaultApplicationService.loadApplication(null);
    }

    @Test
    public void loadApplication_clientFound_returnsApplicationClient() throws Exception {
        Application client = new Application();
        client.setClientId("correctClientId");
        when(clientDao.getClientByClientId("clientId")).thenReturn(client);
        Application applicationClient = defaultApplicationService.loadApplication("clientId");
        assertThat("client id", applicationClient.getClientId(), equalTo("correctClientId"));
    }

    @Test
    public void deleteClientRole_callsTenantDao_deleteTenantRole() throws Exception {
        ClientRole role = new ClientRole();
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRoles.add(tenantRole);
        when(tenantDao.getAllTenantRolesForClientRole(role)).thenReturn(tenantRoles);
        defaultApplicationService.deleteClientRole(role);
        verify(tenantDao).deleteTenantRole(tenantRole);
    }

    @Test
    public void deleteClientRole_callsClientDao_deleteClientRole() throws Exception {
        ClientRole role = new ClientRole();
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        when(tenantDao.getAllTenantRolesForClientRole(role)).thenReturn(tenantRoles);
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

    @Test
    public void getClientServices_callsClientDao_getClientByClientId() throws Exception {
        Application client = new Application();
        client.setUniqueId("uniqueId");
        List<ScopeAccess> services = new ArrayList<ScopeAccess>();
        ScopeAccess scopeAccess = new ScopeAccess();
        scopeAccess.setClientId("clientId");
        services.add(scopeAccess);
        when(scopeAccessDao.getScopeAccessesByParent("uniqueId")).thenReturn(services);
        defaultApplicationService.getClientServices(client);
        verify(clientDao).getClientByClientId("clientId");
    }

    @Test
    public void getClientServices_createsApplicationClients_returnsCorrectInfo() throws Exception {
        Application client = new Application();
        client.setUniqueId("uniqueId");
        List<ScopeAccess> services = new ArrayList<ScopeAccess>();
        ScopeAccess scopeAccess = new ScopeAccess();
        scopeAccess.setClientId("clientId");
        services.add(scopeAccess);
        when(scopeAccessDao.getScopeAccessesByParent("uniqueId")).thenReturn(services);
        Applications clientServices = defaultApplicationService.getClientServices(client);
        assertThat("total records", clientServices.getTotalRecords(), equalTo(1));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addUserToClientGroup_usernameIsBlank_throwsIllegalArgument() throws Exception {
        defaultApplicationService.addUserToClientGroup("", null);
    }

    @Test (expected = NotFoundException.class)
    public void addUserToClientGroup_userIsNull_throwsNotFoundException() throws Exception {
        when(userDao.getUserByUsername("username")).thenReturn(null);
        defaultApplicationService.addUserToClientGroup("username", null);
    }

    @Test (expected = UserDisabledException.class)
    public void addUserToClientGroup_userIsDisabled_throwsUserDisabled() throws Exception {
        when(userDao.getUserByUsername("username")).thenReturn(new User());
        defaultApplicationService.addUserToClientGroup("username", null);
    }

    @Test
    public void addUserToClientGroup_userExistsInGroup_throwsDuplicateException() throws Exception {
        User user = new User();
        ClientGroup clientGroup = new ClientGroup();
        user.setEnabled(true);
        user.setUniqueId("uniqueId");
        when(userDao.getUserByUsername("username")).thenReturn(user);
        doThrow(new DuplicateException()).when(clientDao).addUserToClientGroup("uniqueId", clientGroup);
        defaultApplicationService.addUserToClientGroup("username", clientGroup);
        verify(clientDao).addUserToClientGroup("uniqueId", clientGroup);
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
        when(clientDao.getClientByClientId("clientId")).thenReturn(client);
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
        when(clientDao.getClientByClientId("clientId")).thenReturn(client);
        when(clientDao.getClientRolesByClientId("clientId")).thenReturn(clientRoleList);
        spy.delete("clientId");
        verify(spy).deleteClientRole(clientRole);
    }

    @Test
    public void getDefinedPermissionsByClient_foundPermissions_returnsDefinedPermissionsList() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        client.setRCN("rcn");
        client.setUniqueId("uniqueId");
        Permission permission = new DefinedPermission();
        List<Permission> permissionList = new ArrayList<Permission>();
        permissionList.add(permission);
        when(scopeAccessDao.getPermissionsByParentAndPermission(eq("uniqueId"), any(Permission.class))).thenReturn(permissionList);
        List<DefinedPermission> result = defaultApplicationService.getDefinedPermissionsByClient(client);
        assertThat("permission", result.get(0), equalTo(permission));
        assertThat("list", result.size(), equalTo(1));
    }

    @Test
    public void getClientGroupsForUserByClientIdAndType_groupNotNullAndFilterByClientAndClientIdNotMatch_returnsEmptyList() throws Exception {
        String[] groupIds = {"groupId"};
        ClientGroup group = new ClientGroup();
        group.setClientId("notMatch");
        group.setType("type");
        when(userDao.getGroupIdsForUser("username")).thenReturn(groupIds);
        when(clientDao.getClientGroupByUniqueId("groupId")).thenReturn(group);
        List<ClientGroup> result = defaultApplicationService.getClientGroupsForUserByClientIdAndType("username", "clientId", "type");
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getClientGroupsForUser_clientGroupIsNull_returnsEmptyList() throws Exception {
        String[] groupIds = {"groupId"};
        when(userDao.getGroupIdsForUser("username")).thenReturn(groupIds);
        when(clientDao.getClientGroupByUniqueId("groupId")).thenReturn(null);
        List<ClientGroup> result = defaultApplicationService.getClientGroupsForUser("username");
        assertThat("list", result.isEmpty(), equalTo(true));
    }
}
