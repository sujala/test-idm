package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;

import junit.framework.Assert;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

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

    DefaultApplicationService service;
    ApplicationDao applicationDao;
    ApplicationRoleDao applicationRoleDao;
    CustomerService customerService;
    ScopeAccessService scopeAccessService;
    TenantService tenantService;

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
        service = new DefaultApplicationService();
        applicationDao = EasyMock.createMock(ApplicationDao.class);
        applicationRoleDao = EasyMock.createMock(ApplicationRoleDao.class);
        customerService = EasyMock.createMock(CustomerService.class);
        scopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        tenantService = EasyMock.createMock(TenantService.class);

        service.applicationDao = applicationDao;
        service.customerService = customerService;
        service.tenantService = tenantService;
        service.applicationRoleDao = applicationRoleDao;
        service.scopeAccessService = scopeAccessService;
    }

    @Test
    public void shouldGetClientByName() {

        Application client = getFakeClient();
        EasyMock.expect(applicationDao.getClientByClientname(name)).andReturn(
            client);
        EasyMock.replay(applicationDao);

        Application retrievedClient = service.getByName(name);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldGetClientById() {

        Application client = getFakeClient();
        EasyMock.expect(applicationDao.getClientByClientId(clientId)).andReturn(
            client);
        EasyMock.replay(applicationDao);

        Application retrievedClient = service.getById(clientId);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldGetClientByCustomerIdAndClientId() {

        Application client = getFakeClient();
        EasyMock.expect(
            applicationDao
                .getClientByCustomerIdAndClientId(customerId, clientId))
            .andReturn(client);
        EasyMock.replay(applicationDao);

        Application retrievedClient = service.getClient(customerId, clientId);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientByName() {
        EasyMock.expect(applicationDao.getClientByClientname(name)).andReturn(
            null);
        EasyMock.replay(applicationDao);

        Application retrievedClient = service.getByName(name);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientById() {
        EasyMock.expect(applicationDao.getClientByClientId(clientId)).andReturn(
            null);
        EasyMock.replay(applicationDao);

        Application retrievedClient = service.getById(clientId);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldAddClient() {
        Application client = getFakeClient();
        Customer customer = getFakeCustomer();

        EasyMock.expect(
            customerService.getCustomer(client.getRCN()))
            .andReturn(customer);
        EasyMock.replay(customerService);

        EasyMock.expect(applicationDao.getClientByClientname(client.getName()))
            .andReturn(null);
        applicationDao.addClient((Application) EasyMock.anyObject());
        EasyMock.replay(applicationDao);

        service.add(client);

        EasyMock.verify(applicationDao);
    }

    @Test(expected = DuplicateException.class)
    public void shouldNotAddClientIfClientNameAlreadyTaken() {
        Application client = getFakeClient();
        Customer customer = getFakeCustomer();

        EasyMock.expect(
            customerService.getCustomer(client.getRCN()))
            .andReturn(customer);
        EasyMock.replay(customerService);

        EasyMock.expect(applicationDao.getClientByClientname(client.getName()))
            .andReturn(client);
        EasyMock.replay(applicationDao);

        service.add(client);

        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldDeleteClient() {
        List<Permission> perms = new ArrayList<Permission>();
        applicationDao.deleteClient(EasyMock.anyObject(Application.class));
        EasyMock.expect(applicationDao.getClientByClientId(clientId)).andReturn(
            getFakeClient());

        EasyMock.expect(
            scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(uniqueId,
                clientId)).andReturn(getFakeScopeAccess());
        EasyMock.expect(
            scopeAccessService.getPermissionsForParentByPermission(
                EasyMock.anyObject(String.class),
                EasyMock.anyObject(Permission.class))).andReturn(perms);

        List<ClientRole> clientRoles = new ArrayList<ClientRole>();
        EasyMock.expect(applicationDao.getClientRolesByClientId(clientId)).andReturn(clientRoles);

        EasyMock.replay(applicationDao, scopeAccessService);

        service.delete(clientId);

        EasyMock.verify(applicationDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundExceptionForDeleteNonExistentClient() {
        EasyMock.expect(applicationDao.getClientByClientId(clientId)).andReturn(
            null);

        EasyMock.replay(applicationDao);

        service.delete(clientId);
    }

    @Test
    public void shouldSaveClient() {
        Application client = getFakeClient();
        applicationDao.updateClient(client);
        EasyMock.replay(applicationDao);

        service.save(client);

        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldSoftDeleteClient() {
        Application client = getFakeClient();

        applicationDao.softDeleteApplication(client);
        EasyMock.replay(applicationDao);

        service.softDeleteApplication(client);

        EasyMock.verify(applicationDao);
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
            applicationDao.getClientsByCustomerId(customerId, 0, 100))
            .andReturn(clients);
        EasyMock.replay(applicationDao);

        Applications returned = service.getByCustomerId(customerId, 0, 100);

        Assert.assertTrue(returned.getClients().size() == 2);
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldResetClientSecret() {

        Application client = getFakeClient();
        String oldSecret = client.getClientSecret();

        applicationDao.updateClient(client);
        EasyMock.replay(applicationDao);

        ClientSecret clientSecret = service.resetClientSecret(client);

        Assert.assertNotSame(oldSecret, clientSecret.getValue());
        EasyMock.verify(applicationDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotResetClientSecretIfNull() {
        Application client = null;
        service.resetClientSecret(client);
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
