package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

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

    @Before
    public void setUp() throws Exception {
        clientDao = mock(ApplicationDao.class);
        customerDao = mock(CustomerDao.class);
        userDao = mock(UserDao.class);
        service = new DefaultApplicationService(null, clientDao,customerDao,userDao,null);
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
