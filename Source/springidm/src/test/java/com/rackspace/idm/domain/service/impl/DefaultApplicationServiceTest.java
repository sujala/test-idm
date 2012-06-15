package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.UserDisabledException;
import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/3/12
 * Time: 1:52 PM
 */
public class DefaultApplicationServiceTest {

    DefaultApplicationService service;
    private ApplicationDao clientDao;
    private CustomerDao customerDao;
    private UserDao userDao;
    private ScopeAccessDao scopeAccessDao;
    private TenantDao tenantDao;

    @Before
    public void setUp() throws Exception {
        clientDao = mock(ApplicationDao.class);
        customerDao = mock(CustomerDao.class);
        userDao = mock(UserDao.class);
        scopeAccessDao = mock(ScopeAccessDao.class);
        tenantDao = mock(TenantDao.class);
        service = new DefaultApplicationService(scopeAccessDao, clientDao,customerDao,userDao,tenantDao);
    }

    @Test (expected = IllegalStateException.class)
    public void addDefinedPermission_customerIsNull_throwsIllegalStateException() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        when(customerDao.getCustomerByCustomerId(anyString())).thenReturn(null);
        service.addDefinedPermission(definedPermission);
    }

    @Test (expected = IllegalStateException.class)
    public void addDefinedPermission_clientIsNull_throwsIllegalStateException() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        when(customerDao.getCustomerByCustomerId(anyString())).thenReturn(new Customer());
        when(clientDao.getClientByClientId(anyString())).thenReturn(null);
        service.addDefinedPermission(definedPermission);
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
        service.addDefinedPermission(definedPermission);
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
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        when(scopeAccessDao.addDirectScopeAccess(anyString(), any(ScopeAccess.class))).thenReturn(new ScopeAccess());
        when(scopeAccessDao.getPermissionByParentAndPermission(anyString(), eq(definedPermission))).thenReturn(new DefinedPermission());
        service.addDefinedPermission(definedPermission);
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
        service.addDefinedPermission(definedPermission);
        verify(scopeAccessDao).definePermission(anyString(), any(DefinedPermission.class));
    }

    @Test
    public void deleteDefinedPermission_scopeAccessDao_callsGetPermissionsByPermission() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setClientId("clientId");
        definedPermission.setCustomerId("customerId");
        definedPermission.setPermissionId("permissionId");
        when(scopeAccessDao.getPermissionsByPermission(definedPermission)).thenReturn(new ArrayList<Permission>());
        service.deleteDefinedPermission(definedPermission);
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
        service.deleteDefinedPermission(definedPermission);
        verify(scopeAccessDao).removePermissionFromScopeAccess(permission);
    }

    @Test
    public void getAllApplications_callsClientDao_getAllClients() throws Exception {
        service.getAllApplications(null, 0, 0);
        verify(clientDao).getAllClients(null, 0, 0);
    }

    @Test
    public void getClientByScope_callsClientDao_getClientByScope() throws Exception {
        service.getClientByScope(null);
        verify(clientDao).getClientByScope(null);
    }

    @Test
    public void getDefinedPermissionByClientIdAndPermissionId_callsScopeAccessDao_getPermissionByParentAndPermission() throws Exception {
        Application client = new Application();
        client.setRCN("rcn");
        client.setClientId("clientId");
        client.setUniqueId("uniqueId");
        when(clientDao.getClientByClientId("clientId")).thenReturn(client);
        service.getDefinedPermissionByClientIdAndPermissionId("clientId", "permissionId");
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
        DefinedPermission permission = service.getDefinedPermissionByClientIdAndPermissionId("clientId", "permissionId");
        assertThat("permission", permission.toString(), equalTo("Permission [ldapEntry=null, permissionId=null, clientId=null, customerId=null]"));
    }

    @Test (expected = NotFoundException.class)
    public void getDefinedPermissionByClientIdAndPermissionId_clientIdNotFound_throwsNotFoundException() throws Exception {
        service.getDefinedPermissionByClientIdAndPermissionId("clientId", "permissionId");
    }

    @Test
    public void updateDefinedPermission_callsScopeAccessDao_updatePermissionForScopeAccess() throws Exception {
        service.updateDefinedPermission(null);
        verify(scopeAccessDao).updatePermissionForScopeAccess(null);
    }

    @Test
    public void getAvailableScopes_callsClientDAO_getAvailableScopes() throws Exception {
        service.getAvailableScopes();
        verify(clientDao).getAvailableScopes();
    }

    @Test(expected = NotFoundException.class)
    public void getAvailableScopes_throwsNotFoundException_whenNotScopesWhereFound() throws Exception {
        when(clientDao.getAvailableScopes()).thenReturn(null);
        service.getAvailableScopes();
    }

    @Test(expected = NotFoundException.class)
    public void addClientGroup_throwsNotFoundException_whenClientDoesNotExist() throws Exception {
        when(clientDao.getClientByClientId("id")).thenReturn(null);
        service.addClientGroup(new ClientGroup("id",null,null,null));
    }

    @Test(expected = NotFoundException.class)
    public void addClientGroup_throwsNotFoundException_whenCustomerDoesNotExist() throws Exception {
        when(clientDao.getClientByClientId("id")).thenReturn(new Application());
        when(customerDao.getCustomerByCustomerId("id")).thenReturn(null);
        service.addClientGroup(new ClientGroup("id", "id", null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteClientGroup_throwsIllegalArgumentException_whenClientIdIsNull() throws Exception {
        service.deleteClientGroup("id", null, "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteClientGroup_throwsIllegalArgumentException_whenClientIdIsBlank() throws Exception {
        service.deleteClientGroup("id", "", "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteClientGroup_throwsIllegalArgumentException_whenGroupNameNull() throws Exception {
        service.deleteClientGroup("id", "id", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteClientGroup_throwsIllegalArgumentException_whenGroupNameIsBlank() throws Exception {
        service.deleteClientGroup("id", "id", "");
    }

    @Test(expected = NotFoundException.class)
    public void deleteClientGroup_throwsNotFoundException_whenGroupIsNotFound() throws Exception {
        when(clientDao.getClientGroup("customerId", "clientId", "groupName")).thenReturn(null);
        service.deleteClientGroup("customerId", "clientId", "groupName");
    }

    @Test (expected = NotFoundException.class)
    public void checkAndGetPermission_permissionIsNull_throwsNotFoundException() throws Exception {
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        service.checkAndGetPermission(null, null, null);
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
        service.checkAndGetPermission("notMatch", "clientId", "permissionId");
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
        service.checkAndGetPermission("customerId", "notMatch", "permissionId");
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
        service.checkAndGetPermission("customerId", "clientId", "permissionId");
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
        DefinedPermission definedPermission = service.checkAndGetPermission("customerId", "clientId", "permissionId");
        assertThat("permission id", definedPermission.getPermissionId(), equalTo("permissionId"));
    }

    @Test
    public void updateClientRole_callsClientDao_updateClientRole() throws Exception {
        service.updateClientRole(null);
        verify(clientDao).updateClientRole(null);
    }

    @Test
    public void getClientRolesByClientId_callsClientDao_getClientRolesByClientId() throws Exception {
        List<ClientRole> roles = new ArrayList<ClientRole>();
        when(clientDao.getClientRolesByClientId("clientId")).thenReturn(roles);
        service.getClientRolesByClientId("clientId");
        verify(clientDao).getClientRolesByClientId("clientId");
    }

    @Test
    public void getAllClientRoles_callsClientDao_getAllClientRoles() throws Exception {
        List<ClientRole> roles = new ArrayList<ClientRole>();
        when(clientDao.getAllClientRoles(null)).thenReturn(roles);
        service.getAllClientRoles(null);
    }

    @Test
    public void getClientRoleByClientIdAndRoleName_callsClientDao_getClientRoleByClientIdAndRoleName() throws Exception {
        when(clientDao.getClientRoleByClientIdAndRoleName("clientId", "roleName")).thenReturn(new ClientRole());
        service.getClientRoleByClientIdAndRoleName("clientId", "roleName");
        verify(clientDao).getClientRoleByClientIdAndRoleName("clientId", "roleName");
    }

    @Test
    public void getClientRoleById_callsClientDao_getClientRoleById() throws Exception {
        when(clientDao.getClientRoleById("id")).thenReturn(new ClientRole());
        service.getClientRoleById("id");
        verify(clientDao).getClientRoleById("id");
    }

    @Test
    public void getOpenStackService_callsClientDao_getOpenStackService() throws Exception {
        when(clientDao.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        service.getOpenStackServices();
        verify(clientDao).getOpenStackServices();
    }

    @Test(expected = NotFoundException.class)
    public void removeUserFromClientGroup_throwsNotFoundException_whenCustomerDoesNotExist() throws Exception {
        ClientGroup clientGroup = new ClientGroup("clientId", "customerId", "name", "type");
        when(userDao.getUserByUsername("username")).thenReturn(new User());
        when(customerDao.getCustomerByCustomerId("customerId")).thenReturn(null);
        service.removeUserFromClientGroup("username", clientGroup);
    }

    @Test
    public void updateClient_callsClientDao_updateClient() throws Exception {
        Application client = new Application();
        service.updateClient(client);
        verify(clientDao).updateClient(client);
    }

    @Test(expected = NotFoundException.class)
    public void addClientRole_throwsNotFoundException_whenClientIsNotFound() throws Exception {
        ClientRole role = new ClientRole();
        role.setClientId("clientId");
        when(clientDao.getClientByClientId("clientId")).thenReturn(null);
        service.addClientRole(role);
    }

    @Test(expected = DuplicateException.class)
    public void addClientRole_throwsDuplicateException_whenRoleIsNotFound() throws Exception {
        ClientRole role = new ClientRole();
        role.setClientId("clientId");
        role.setName("role");
        when(clientDao.getClientByClientId("clientId")).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName("clientId","role")).thenReturn(new ClientRole());
        service.addClientRole(role);
    }

    @Test
    public void addClientRole_callsClientDao_addClientRole() throws Exception {
        ClientRole role = new ClientRole();
        role.setClientId("clientId");
        role.setName("role");
        when(clientDao.getClientByClientId("clientId")).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName("clientId","role")).thenReturn(null);
        service.addClientRole(role);
        verify(clientDao).addClientRole(anyString(), eq(role));
    }

    @Test (expected = NotFoundException.class)
    public void loadApplication_clientIsNull_throwsNotFoundException() throws Exception {
        when(clientDao.getClientByClientId(null)).thenReturn(null);
        service.loadApplication(null);
    }

    @Test
    public void loadApplication_clientFound_returnsApplicationClient() throws Exception {
        Application client = new Application();
        client.setClientId("correctClientId");
        when(clientDao.getClientByClientId("clientId")).thenReturn(client);
        Application applicationClient = service.loadApplication("clientId");
        assertThat("client id", applicationClient.getClientId(), equalTo("correctClientId"));
    }

    @Test
    public void deleteClientRole_callsTenantDao_deleteTenantRole() throws Exception {
        ClientRole role = new ClientRole();
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRoles.add(tenantRole);
        when(tenantDao.getAllTenantRolesForClientRole(role)).thenReturn(tenantRoles);
        service.deleteClientRole(role);
        verify(tenantDao).deleteTenantRole(tenantRole);
    }

    @Test
    public void deleteClientRole_callsClientDao_deleteClientRole() throws Exception {
        ClientRole role = new ClientRole();
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        when(tenantDao.getAllTenantRolesForClientRole(role)).thenReturn(tenantRoles);
        service.deleteClientRole(role);
        verify(clientDao).deleteClientRole(role);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientServices_clientUniqueIdIsNull_throwsIllegalArgumentException() throws Exception {
        service.getClientServices(new Application());
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
        service.getClientServices(client);
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
        Applications clientServices = service.getClientServices(client);
        assertThat("total records", clientServices.getTotalRecords(), equalTo(1));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addUserToClientGroup_usernameIsBlank_throwsIllegalArgument() throws Exception {
        service.addUserToClientGroup("", null);
    }

    @Test (expected = NotFoundException.class)
    public void addUserToClientGroup_userIsNull_throwsNotFoundException() throws Exception {
        when(userDao.getUserByUsername("username")).thenReturn(null);
        service.addUserToClientGroup("username", null);
    }

    @Test (expected = UserDisabledException.class)
    public void addUserToClientGroup_userIsDisabled_throwsUserDisabled() throws Exception {
        when(userDao.getUserByUsername("username")).thenReturn(new User());
        service.addUserToClientGroup("username", null);
    }

    @Test
    public void addUserToClientGroup_userExistsInGroup_throwsDuplicateException() throws Exception {
        User user = new User();
        ClientGroup clientGroup = new ClientGroup();
        user.setEnabled(true);
        user.setUniqueId("uniqueId");
        when(userDao.getUserByUsername("username")).thenReturn(user);
        doThrow(new DuplicateException()).when(clientDao).addUserToClientGroup("uniqueId", clientGroup);
        service.addUserToClientGroup("username", clientGroup);
        verify(clientDao).addUserToClientGroup("uniqueId", clientGroup);
    }
}
