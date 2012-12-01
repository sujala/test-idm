package com.rackspace.idm.domain.service.impl;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;

public class ApplicationServiceTests {
    ScopeAccessDao mockScopeAccessDao;
    ApplicationDao mockApplicationDao;
    CustomerDao mockCustomerDao;
    UserDao mockUserDao;
    TenantDao mockTenantDao;
    ApplicationService clientService;

    String clientId = "ClientId";
    ClientSecret clientSecret = ClientSecret.newInstance("Secret");
    String name = "Name";
    String customerId = "CustomerId";

    String groupName = "groupName";
    String groupType = "groupType";

    String userDN = "userDN";
    String groupDN = "groupDN";
    String username = "username";

    String uniqueId = "uniqueId";
    
    String id = "XXX";

    @Before
    public void setUp() throws Exception {

        mockApplicationDao = EasyMock.createMock(ApplicationDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        mockUserDao = EasyMock.createMock(UserDao.class);
        mockScopeAccessDao = EasyMock.createMock(ScopeAccessDao.class);
        mockTenantDao = EasyMock.createMock(TenantDao.class);

        clientService = new DefaultApplicationService();
        clientService.setScopeAccessDao(mockScopeAccessDao);
        clientService.setApplicationDao(mockApplicationDao);
        clientService.setCustomerDao(mockCustomerDao);
        clientService.setUserDao(mockUserDao);
        clientService.setTenantDao(mockTenantDao);
    }

    @Test
    public void shouldGetClientByName() {

        Application client = getFakeClient();
        EasyMock.expect(mockApplicationDao.getClientByClientname(name)).andReturn(
            client);
        EasyMock.replay(mockApplicationDao);

        Application retrievedClient = clientService.getByName(name);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(mockApplicationDao);
    }

    @Test
    public void shouldGetClientById() {

        Application client = getFakeClient();
        EasyMock.expect(mockApplicationDao.getClientByClientId(clientId)).andReturn(
            client);
        EasyMock.replay(mockApplicationDao);

        Application retrievedClient = clientService.getById(clientId);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(mockApplicationDao);
    }

    @Test
    public void shouldGetClientByCustomerIdAndClientId() {

        Application client = getFakeClient();
        EasyMock.expect(
            mockApplicationDao
                .getClientByCustomerIdAndClientId(customerId, clientId))
            .andReturn(client);
        EasyMock.replay(mockApplicationDao);

        Application retrievedClient = clientService.getClient(customerId, clientId);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(mockApplicationDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientByName() {
        EasyMock.expect(mockApplicationDao.getClientByClientname(name)).andReturn(
            null);
        EasyMock.replay(mockApplicationDao);

        Application retrievedClient = clientService.getByName(name);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(mockApplicationDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientById() {
        EasyMock.expect(mockApplicationDao.getClientByClientId(clientId)).andReturn(
            null);
        EasyMock.replay(mockApplicationDao);

        Application retrievedClient = clientService.getById(clientId);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(mockApplicationDao);
    }

    @Test
    public void shouldAddClient() {
        Application client = getFakeClient();
        Customer customer = getFakeCustomer();

        EasyMock.expect(
            mockCustomerDao.getCustomerByCustomerId(client.getRCN()))
            .andReturn(customer);
        EasyMock.replay(mockCustomerDao);

        EasyMock.expect(mockApplicationDao.getClientByClientname(client.getName()))
            .andReturn(null);
        mockApplicationDao.addClient((Application) EasyMock.anyObject());
        EasyMock.replay(mockApplicationDao);

        clientService.add(client);

        EasyMock.verify(mockApplicationDao);
    }

    @Test(expected = DuplicateException.class)
    public void shouldNotAddClientIfClientNameAlreadyTaken() {
        Application client = getFakeClient();
        Customer customer = getFakeCustomer();

        EasyMock.expect(
            mockCustomerDao.getCustomerByCustomerId(client.getRCN()))
            .andReturn(customer);
        EasyMock.replay(mockCustomerDao);

        EasyMock.expect(mockApplicationDao.getClientByClientname(client.getName()))
            .andReturn(client);
        EasyMock.replay(mockApplicationDao);

        clientService.add(client);

        EasyMock.verify(mockApplicationDao);
    }

    @Test
    public void shouldDeleteClient() {
        List<Permission> perms = new ArrayList<Permission>();
        mockApplicationDao.deleteClient(EasyMock.anyObject(Application.class));
        EasyMock.expect(mockApplicationDao.getClientByClientId(clientId)).andReturn(
            getFakeClient());

        EasyMock.expect(
            mockScopeAccessDao.getMostRecentDirectScopeAccessForParentByClientId(uniqueId,
                clientId)).andReturn(getFakeScopeAccess());
        EasyMock.expect(
            mockScopeAccessDao.getPermissionsByParentAndPermission(
                EasyMock.anyObject(String.class),
                EasyMock.anyObject(Permission.class))).andReturn(perms);

        List<ClientRole> clientRoles = new ArrayList<ClientRole>();
        EasyMock.expect(mockApplicationDao.getClientRolesByClientId(clientId)).andReturn(clientRoles);

        EasyMock.replay(mockApplicationDao, mockScopeAccessDao);

        clientService.delete(clientId);

        EasyMock.verify(mockApplicationDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundExceptionForDeleteNonExistentClient() {
        EasyMock.expect(mockApplicationDao.getClientByClientId(clientId)).andReturn(
            null);

        EasyMock.replay(mockApplicationDao);

        clientService.delete(clientId);
    }

    @Test
    public void shouldSaveClient() {
        Application client = getFakeClient();
        mockApplicationDao.updateClient(client);
        EasyMock.replay(mockApplicationDao);

        clientService.save(client);

        EasyMock.verify(mockApplicationDao);
    }

    @Test
    public void shouldSoftDeleteClient() {
        Application client = getFakeClient();

        mockApplicationDao.softDeleteApplication(client);
        EasyMock.replay(mockApplicationDao);

        clientService.softDeleteApplication(client);

        EasyMock.verify(mockApplicationDao);
    }

    @Test
    public void shouldGetClientByCustomerId() {
        List<Application> clientList = new ArrayList<Application>();
        clientList.add(getFakeClient());
        clientList.add(getFakeClient());

        Applications clients = new Applications();
        clients.setClients(clientList);
        clients.setLimit(100);
        clients.setOffset(0);
        clients.setTotalRecords(2);

        EasyMock.expect(
            mockApplicationDao.getClientsByCustomerId(customerId, 0, 100))
            .andReturn(clients);
        EasyMock.replay(mockApplicationDao);

        Applications returned = clientService.getByCustomerId(customerId, 0, 100);

        Assert.assertTrue(returned.getClients().size() == 2);
        EasyMock.verify(mockApplicationDao);
    }

    @Test
    public void shouldResetClientSecret() {

        Application client = getFakeClient();
        String oldSecret = client.getClientSecret();

        mockApplicationDao.updateClient(client);
        EasyMock.replay(mockApplicationDao);

        ClientSecret clientSecret = clientService.resetClientSecret(client);

        Assert.assertNotSame(oldSecret, clientSecret.getValue());
        EasyMock.verify(mockApplicationDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotResetClientSecretIfNull() {
        Application client = null;
        clientService.resetClientSecret(client);
    }

    private Application getFakeClient() {
        Application client = new Application(clientId, clientSecret, name,
            customerId);
        client.setUniqueId(uniqueId);
        return client;
    }

    private Customer getFakeCustomer() {
        Customer customer = new Customer();
        customer.setRcn(customerId);
        return customer;
    }

    private List<ClientGroup> getFakeClientGroupList() {
        List<ClientGroup> groups = new ArrayList<ClientGroup>();
        groups.add(getFakeClientGroup());
        return groups;
    }

    private ClientGroup getFakeClientGroup() {
        ClientGroup group = new ClientGroup(clientId, customerId, groupName,
            groupType);
        group.setUniqueId(groupDN);
        return group;
    }

    private User getFakeUser() {
        User user = new User();
        user.setUsername(username);
        user.setUniqueId(userDN);
        user.setEnabled(true);
        return user;
    }

    private ScopeAccess getFakeScopeAccess() {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(clientId);
        sa.setClientRCN(customerId);
        return sa;
    }
}
