package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientGroup;
import com.rackspace.idm.entities.ClientSecret;
import com.rackspace.idm.entities.ClientStatus;
import com.rackspace.idm.entities.Clients;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.CustomerStatus;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.test.stub.StubLogger;

public class ClientServiceTests {
    ClientDao mockClientDao;
    CustomerDao mockCustomerDao;
    UserDao mockUserDao;
    ClientService clientService;

    String clientId = "ClientId";
    ClientSecret clientSecret = ClientSecret.newInstance("Secret");
    String name = "Name";
    String inum = "Inum";
    String iname = "Iname";
    String customerId = "CustomerId";
    ClientStatus status = ClientStatus.ACTIVE;
    String seeAlso = "SeeAlso";
    String owner = "Owner";
    
    String customerName = "Name";
    String customerInum = "Inum";
    String customerIname = "Iname";
    CustomerStatus customerStatus = CustomerStatus.ACTIVE;
    String customerSeeAlso = "SeeAlso";
    String customerOwner = "Owner";
    String customerCountry = "USA";
    
    String resourceId = "resource";
    String resourceValue = "resourceValue";
    
    String groupName = "groupName";
    
    String userDN = "userDN";
    String groupDN = "groupDN";
    String username = "username";

    @Before
    public void setUp() throws Exception {

        mockClientDao = EasyMock.createMock(ClientDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        mockUserDao = EasyMock.createMock(UserDao.class);

        clientService = new DefaultClientService(mockClientDao, mockCustomerDao, mockUserDao,
            new StubLogger());
    }

    @Test
    public void shouldGetClientByName() {

        Client client = getFakeClient();
        EasyMock.expect(mockClientDao.findByClientname(name)).andReturn(client);
        EasyMock.replay(mockClientDao);

        Client retrievedClient = clientService.getByName(name);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldGetClientById() {

        Client client = getFakeClient();
        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(
            client);
        EasyMock.replay(mockClientDao);

        Client retrievedClient = clientService.getById(clientId);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientByName() {
        EasyMock.expect(mockClientDao.findByClientname(name)).andReturn(null);
        EasyMock.replay(mockClientDao);

        Client retrievedClient = clientService.getByName(name);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientById() {
        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(null);
        EasyMock.replay(mockClientDao);

        Client retrievedClient = clientService.getById(clientId);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldAuthenticateClient() {
        EasyMock.expect(
            mockClientDao.authenticateDeprecated(clientId, clientSecret.getValue()))
            .andReturn(true);
        EasyMock.replay(mockClientDao);

        boolean authenticated = clientService.authenticateDeprecated(clientId,
                clientSecret.getValue());

        Assert.assertTrue(authenticated);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldAddClient() {
        Client client = getFakeClient();
        Customer customer = getFakeCustomer();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(client.getCustomerId())).andReturn(customer);
        EasyMock.replay(mockCustomerDao);
        
        EasyMock.expect(mockClientDao.findByClientname(client.getName())).andReturn(null);
        EasyMock.expect(
            mockClientDao
                .getUnusedClientInum(customer.getInum()))
            .andReturn(customer.getInum() + "!8888.8888");
        mockClientDao.add((Client)EasyMock.anyObject());
        EasyMock.replay(mockClientDao);
        
        clientService.add(client);
        
        EasyMock.verify(mockClientDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotAddClientIfCustomerNotExist() {
        Client client = getFakeClient();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(client.getCustomerId())).andReturn(null);
        EasyMock.replay(mockCustomerDao);
        
        clientService.add(client);
    }
    

    @Test(expected = DuplicateException.class)
    public void shouldNotAddClientIfClientNameAlreadyTaken() {
        Client client = getFakeClient();
        Customer customer = getFakeCustomer();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(client.getCustomerId())).andReturn(customer);
        EasyMock.replay(mockCustomerDao);
        
        EasyMock.expect(mockClientDao.findByClientname(client.getName())).andReturn(client);
        EasyMock.replay(mockClientDao);
        
        clientService.add(client);
        
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldDeleteClient() {
        mockClientDao.delete(clientId);
        EasyMock.replay(mockClientDao);
        
        clientService.delete(clientId);
        
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldSaveClient() {
        Client client = getFakeClient();
        mockClientDao.save(client);
        EasyMock.replay(mockClientDao);
        
        clientService.save(client);
        
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldSoftDeleteClient() {
        Client client = getFakeClient();
        
        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(client);
        mockClientDao.save(client);
        EasyMock.replay(mockClientDao);
        
        clientService.softDelete(clientId);
        
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldGetClientByCustomerId() {
        List<Client> clientList = new ArrayList<Client>();
        clientList.add(getFakeClient());
        clientList.add(getFakeClient());
        
        Clients clients = new Clients();
        clients.setClients(clientList);
        clients.setLimit(100);
        clients.setOffset(0);
        clients.setTotalRecords(2);
        
        EasyMock.expect(mockClientDao.getByCustomerId(customerId, 0, 100)).andReturn(clients);
        EasyMock.replay(mockClientDao);
        
        Clients returned = clientService.getByCustomerId(customerId, 0, 100);
        
        Assert.assertTrue(returned.getClients().size() == 2);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldGetPermissionByClientIdAndPermissionId()
    {
        Permission resource = getFakePermission();
        
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(resource);
        EasyMock.replay(mockClientDao);
        
        Permission returned = clientService.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId);
        
        Assert.assertTrue(resource.getClientId().equals(returned.getClientId()));
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldGetPermissionsByClientId()
    {
        List<Permission> resources = new ArrayList<Permission>();
        resources.add(getFakePermission());
        resources.add(getFakePermission());
        
        EasyMock.expect(mockClientDao.getDefinedPermissionsByClientId(clientId)).andReturn(resources);
        EasyMock.replay(mockClientDao);
        
        List<Permission> returned = clientService.getDefinedPermissionsByClientId(clientId);
        
        Assert.assertTrue(returned.size() == 2);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldAddPermission() {
        Client client = getFakeClient();
        Customer customer = getFakeCustomer();
        Permission resource = getFakePermission();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId)).andReturn(customer);
        EasyMock.replay(mockCustomerDao);
        
        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(client);
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(null);
        mockClientDao.addDefinedPermission(resource);
        EasyMock.replay(mockClientDao);
        
        clientService.addDefinedPermission(resource);
        
        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = IllegalStateException.class)
    public void shouldNotAddPermissionIfCustomerNotFound() {
        Client client = getFakeClient();
        Customer customer = getFakeCustomer();
        Permission resource = getFakePermission();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId)).andReturn(null);
        EasyMock.replay(mockCustomerDao);
        
        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(client);
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(null);
        mockClientDao.addDefinedPermission(resource);
        EasyMock.replay(mockClientDao);
        
        clientService.addDefinedPermission(resource);
        
        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = IllegalStateException.class)
    public void shouldNotAddPermissionIfClientNotFound() {
        Client client = getFakeClient();
        Customer customer = getFakeCustomer();
        Permission resource = getFakePermission();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId)).andReturn(customer);
        EasyMock.replay(mockCustomerDao);
        
        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(null);
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(null);
        mockClientDao.addDefinedPermission(resource);
        EasyMock.replay(mockClientDao);
        
        clientService.addDefinedPermission(resource);
        
        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = DuplicateException.class)
    public void shouldNotAddPermissionIfDuplicate() {
        Client client = getFakeClient();
        Customer customer = getFakeCustomer();
        Permission resource = getFakePermission();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId)).andReturn(customer);
        EasyMock.replay(mockCustomerDao);
        
        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(client);
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(resource);
        mockClientDao.addDefinedPermission(resource);
        EasyMock.replay(mockClientDao);
        
        clientService.addDefinedPermission(resource);
        
        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldUpdatePermission() {
        Permission resource = getFakePermission();
        mockClientDao.updateDefinedPermission(resource);
        EasyMock.replay(mockClientDao);
        
        clientService.updateDefinedPermission(resource);
        
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldDeletePermission() {
        Permission resource = getFakePermission();
        mockClientDao.deleteDefinedPermission(resource);
        EasyMock.replay(mockClientDao);
        
        clientService.deleteDefinedPermission(resource);
        
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldResetClientSecret() {

        Client client = getFakeClient();
        String oldSecret = client.getClientSecret();

        mockClientDao.save(client);
        EasyMock.replay(mockClientDao);

        ClientSecret clientSecret = clientService.resetClientSecret(client);

        Assert.assertNotSame(oldSecret, clientSecret.getValue());
        EasyMock.verify(mockClientDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotResetClientSecretIfNull() {
        Client client = null;
        ClientSecret clientSecret = clientService.resetClientSecret(client);
    }
    
    @Test
    public void ShouldGetClientGroup() {
        ClientGroup group = getFakeClientGroup();
        EasyMock.expect(mockClientDao.getClientGroupByClientIdAndGroupName(clientId, groupName)).andReturn(group);
        EasyMock.replay(mockClientDao);
        ClientGroup returnedGroup = clientService.getClientGroupByClientIdAndGroupName(clientId, groupName);
        Assert.assertTrue(returnedGroup.getName().equals(group.getName()));
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void ShouldGetClientGroups() {
        ClientGroup group = getFakeClientGroup();
        List<ClientGroup> groups = new ArrayList<ClientGroup>();
        groups.add(group);
        groups.add(group);
        EasyMock.expect(mockClientDao.getClientGroupsByClientId(clientId)).andReturn(groups);
        EasyMock.replay(mockClientDao);
        List<ClientGroup> returnedGroups = clientService.getClientGroupsByClientId(clientId);
        Assert.assertTrue(returnedGroups.size() == 2);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldAddUserToClientGroup() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.findByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockClientDao.getClientGroupByClientIdAndGroupName(clientId, groupName)).andReturn(group);
        mockClientDao.addUserToClientGroup(user, group);
        EasyMock.replay(mockClientDao);
        clientService.addUserToClientGroup(username, group);
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddUserToClientGroupForBlankUsername() {
        ClientGroup group = getFakeClientGroup();
        clientService.addUserToClientGroup(null, group);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddUserToClientGroupForBadClientGroup() {
        ClientGroup group = getFakeClientGroup();
        group.setName(null);
        clientService.addUserToClientGroup(null, group);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddUserToClientGroupForBadClientGroup2() {
        ClientGroup group = getFakeClientGroup();
        clientService.addUserToClientGroup(null, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddUserToClientGroupForBadClientGroup3() {
        ClientGroup group = getFakeClientGroup();
        group.setClientId(null);
        clientService.addUserToClientGroup(null, group);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotAddUserToClientGroupForUserNotFound() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.findByUsername(username)).andReturn(null);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockClientDao.getClientGroupByClientIdAndGroupName(clientId, groupName)).andReturn(group);
        mockClientDao.addUserToClientGroup(user, group);
        EasyMock.replay(mockClientDao);
        clientService.addUserToClientGroup(username, group);
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotAddUserToClientGroupForGroupNotFound() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.findByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockClientDao.getClientGroupByClientIdAndGroupName(clientId, groupName)).andReturn(null);
        mockClientDao.addUserToClientGroup(user, group);
        EasyMock.replay(mockClientDao);
        clientService.addUserToClientGroup(username, group);
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldRemoveUserFromClientGroup() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.findByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockClientDao.getClientGroupByClientIdAndGroupName(clientId, groupName)).andReturn(group);
        mockClientDao.removeUserFromGroup(user, group);
        EasyMock.replay(mockClientDao);
        clientService.removeUserFromClientGroup(username, group);
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void ShouldAddClientGroup() {
        ClientGroup group = getFakeClientGroup();
        mockClientDao.addClientGroup(group);
        EasyMock.replay(mockClientDao);
        clientService.addClientGroup(group);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void ShouldDeleteClientGroup() {
        mockClientDao.deleteClientGroup(clientId, groupName);
        EasyMock.replay(mockClientDao);
        clientService.deleteClientGroup(clientId, groupName);
        EasyMock.verify(mockClientDao);
    }

    private Client getFakeClient() {
        return new Client(clientId, clientSecret, name, inum, iname,
            customerId, status, seeAlso, owner);
    }
    
    private Customer getFakeCustomer() {
        return new Customer(customerId, customerInum, customerIname,
        customerStatus, customerSeeAlso, owner);
    }
    
    private Permission getFakePermission() {
        Permission res = new Permission();
        res.setClientId(clientId);
        res.setCustomerId(customerId);
        res.setPermissionId(resourceId);
        res.setValue(resourceValue);
        return res;
    }
    
    private ClientGroup getFakeClientGroup() {
        ClientGroup group = new ClientGroup(clientId, customerId, groupName);
        group.setUniqueId(groupDN);
        return group;
    }
    
    private User getFakeUser() {
        User user = new User();
        user.setUsername(username);
        user.setUniqueId(userDN);
        return user;
    }
}
