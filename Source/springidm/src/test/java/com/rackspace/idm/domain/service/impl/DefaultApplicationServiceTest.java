package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
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

    @Before
    public void setUp() throws Exception {
        clientDao = mock(ApplicationDao.class);
        customerDao = mock(CustomerDao.class);
        userDao = mock(UserDao.class);
        scopeAccessDao = mock(ScopeAccessDao.class);
        service = new DefaultApplicationService(scopeAccessDao, clientDao,customerDao,userDao,null);
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
        service.addClientGroup(new ClientGroup("id","id",null,null));
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
        when(clientDao.getClientGroup("customerId","clientId", "groupName")).thenReturn(null);
        service.deleteClientGroup("customerId","clientId", "groupName");
    }

    @Test (expected = NotFoundException.class)
    public void checkAndGetPermission_permissionIsNull_throwsNotFoundException() throws Exception {
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        service.checkAndGetPermission(null, null, null);
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
}
