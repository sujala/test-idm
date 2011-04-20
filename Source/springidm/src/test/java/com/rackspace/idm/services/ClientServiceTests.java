package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.impl.DefaultClientService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;

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
    String groupType = "groupType";

    String userDN = "userDN";
    String groupDN = "groupDN";
    String username = "username";
    
    String uniqueId = "uniqueId";

    @Before
    public void setUp() throws Exception {

        mockClientDao = EasyMock.createMock(ClientDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        mockUserDao = EasyMock.createMock(UserDao.class);

        clientService = new DefaultClientService(mockClientDao,
            mockCustomerDao, mockUserDao);
    }

    @Test
    public void shouldGetClientByName() {

        Client client = getFakeClient();
        EasyMock.expect(mockClientDao.getClientByClientname(name)).andReturn(client);
        EasyMock.replay(mockClientDao);

        Client retrievedClient = clientService.getByName(name);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldGetClientById() {

        Client client = getFakeClient();
        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(
            client);
        EasyMock.replay(mockClientDao);

        Client retrievedClient = clientService.getById(clientId);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldGetClientByCustomerIdAndClientId() {

        Client client = getFakeClient();
        EasyMock.expect(mockClientDao.getClientByCustomerIdAndClientId(customerId, clientId)).andReturn(
            client);
        EasyMock.replay(mockClientDao);

        Client retrievedClient = clientService.getClient(customerId, clientId);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientByName() {
        EasyMock.expect(mockClientDao.getClientByClientname(name)).andReturn(null);
        EasyMock.replay(mockClientDao);

        Client retrievedClient = clientService.getByName(name);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientById() {
        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(null);
        EasyMock.replay(mockClientDao);

        Client retrievedClient = clientService.getById(clientId);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldAuthenticateClient() {
        EasyMock.expect(
            mockClientDao.authenticate(clientId, clientSecret.getValue()))
            .andReturn(new ClientAuthenticationResult(getFakeClient(), true));
        EasyMock.replay(mockClientDao);

        ClientAuthenticationResult authenticated = clientService.authenticate(
            clientId, clientSecret.getValue());

        Assert.assertTrue(authenticated.isAuthenticated());
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldAddClient() {
        Client client = getFakeClient();
        Customer customer = getFakeCustomer();

        EasyMock.expect(
            mockCustomerDao.getCustomerByCustomerId(client.getCustomerId()))
            .andReturn(customer);
        EasyMock.replay(mockCustomerDao);

        EasyMock.expect(mockClientDao.getClientByClientname(client.getName()))
            .andReturn(null);
        EasyMock.expect(mockClientDao.getUnusedClientInum(customer.getInum()))
            .andReturn(customer.getInum() + "!8888.8888");
        mockClientDao.addClient((Client) EasyMock.anyObject(),
            (String) EasyMock.anyObject());
        EasyMock.replay(mockClientDao);

        clientService.add(client);

        EasyMock.verify(mockClientDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotAddClientIfCustomerNotExist() {
        Client client = getFakeClient();

        EasyMock.expect(
            mockCustomerDao.getCustomerByCustomerId(client.getCustomerId()))
            .andReturn(null);
        EasyMock.replay(mockCustomerDao);

        clientService.add(client);
    }

    @Test(expected = DuplicateException.class)
    public void shouldNotAddClientIfClientNameAlreadyTaken() {
        Client client = getFakeClient();
        Customer customer = getFakeCustomer();

        EasyMock.expect(
            mockCustomerDao.getCustomerByCustomerId(client.getCustomerId()))
            .andReturn(customer);
        EasyMock.replay(mockCustomerDao);

        EasyMock.expect(mockClientDao.getClientByClientname(client.getName()))
            .andReturn(client);
        EasyMock.replay(mockClientDao);

        clientService.add(client);

        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldDeleteClient() {
        mockClientDao.deleteClient(clientId);
        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(
            getFakeClient());
        EasyMock
            .expect(mockClientDao.getDefinedPermissionsByClientId(clientId))
            .andReturn(getFakePermissionList());
        mockClientDao.deleteDefinedPermission(getFakePermission());
        EasyMock.expect(mockClientDao.getClientGroupsByClientId(clientId))
            .andReturn(getFakeClientGroupList());
        mockClientDao.deleteClientGroup(customerId, clientId, groupName);

        EasyMock.replay(mockClientDao);

        clientService.delete(clientId);

        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundExceptionForDeleteNonExistentClient() {
        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(
            null);

        EasyMock.replay(mockClientDao);

        clientService.delete(clientId);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundExceptionForDeleteNonExistentPermission() {
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(
            null);

        EasyMock.replay(mockClientDao);

        clientService.deleteDefinedPermission(getFakePermission());
    }

    @Test
    public void shouldSaveClient() {
        Client client = getFakeClient();
        mockClientDao.updateClient(client);
        EasyMock.replay(mockClientDao);

        clientService.save(client);

        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldSoftDeleteClient() {
        Client client = getFakeClient();

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(
            client);
        mockClientDao.updateClient(client);
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

        EasyMock.expect(mockClientDao.getClientsByCustomerId(customerId, 0, 100))
            .andReturn(clients);
        EasyMock.replay(mockClientDao);

        Clients returned = clientService.getByCustomerId(customerId, 0, 100);

        Assert.assertTrue(returned.getClients().size() == 2);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldGetPermissionByClientIdAndPermissionId() {
        Permission resource = getFakePermission();

        EasyMock.expect(
            mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                clientId, resourceId)).andReturn(resource);
        EasyMock.replay(mockClientDao);

        Permission returned = clientService
            .getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId);

        Assert
            .assertTrue(resource.getClientId().equals(returned.getClientId()));
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldGetPermissionsByClientId() {
        List<Permission> resources = new ArrayList<Permission>();
        resources.add(getFakePermission());
        resources.add(getFakePermission());

        EasyMock
            .expect(mockClientDao.getDefinedPermissionsByClientId(clientId))
            .andReturn(resources);
        EasyMock.replay(mockClientDao);

        List<Permission> returned = clientService
            .getDefinedPermissionsByClientId(clientId);

        Assert.assertTrue(returned.size() == 2);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldAddPermission() {
        Client client = getFakeClient();
        client.setUniqueId(uniqueId);
        Customer customer = getFakeCustomer();
        Permission resource = getFakePermission();

        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(customer);
        EasyMock.replay(mockCustomerDao);

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(
            client);
        EasyMock.expect(
            mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                clientId, resourceId)).andReturn(null);
        mockClientDao.addDefinedPermission(resource,uniqueId);
        EasyMock.replay(mockClientDao);

        clientService.addDefinedPermission(resource);

        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockClientDao);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAddPermissionIfCustomerNotFound() {
        Client client = getFakeClient();
        client.setUniqueId(uniqueId);
        Customer customer = getFakeCustomer();
        Permission resource = getFakePermission();

        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(null);
        EasyMock.replay(mockCustomerDao);

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(
            client);
        EasyMock.expect(
            mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                clientId, resourceId)).andReturn(null);
        mockClientDao.addDefinedPermission(resource,uniqueId);
        EasyMock.replay(mockClientDao);

        clientService.addDefinedPermission(resource);

        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockClientDao);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAddPermissionIfClientNotFound() {
        Client client = getFakeClient();
        client.setUniqueId(uniqueId);
        Customer customer = getFakeCustomer();
        Permission resource = getFakePermission();

        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(customer);
        EasyMock.replay(mockCustomerDao);

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(null);
        EasyMock.expect(
            mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                clientId, resourceId)).andReturn(null);
        mockClientDao.addDefinedPermission(resource,uniqueId);
        EasyMock.replay(mockClientDao);

        clientService.addDefinedPermission(resource);

        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockClientDao);
    }

    @Test(expected = DuplicateException.class)
    public void shouldNotAddPermissionIfDuplicate() {
        Client client = getFakeClient();
        client.setUniqueId(uniqueId);
        Customer customer = getFakeCustomer();
        Permission resource = getFakePermission();

        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(customer);
        EasyMock.replay(mockCustomerDao);

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(
            client);
        EasyMock.expect(
            mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                clientId, resourceId)).andReturn(resource);
        mockClientDao.addDefinedPermission(resource,uniqueId);
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
        List<Client> clients = new ArrayList<Client>();
        clients.add(getFakeClient());
        
        EasyMock.expect(
            mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                resource.getClientId(), resource.getPermissionId())).andReturn(
            resource);
        EasyMock.expect(mockClientDao.getClientsThatHavePermission(resource))
            .andReturn(clients);
        mockClientDao.revokePermissionFromClient(getFakePermission(), getFakeClient());

        mockClientDao.deleteDefinedPermission(resource);
        EasyMock.replay(mockClientDao);

        clientService.deleteDefinedPermission(resource);

        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldGrantPermission() {
        Permission resource = getFakePermission();
        Client client = getFakeClient();
        String clientId = client.getClientId();

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(
            client);
        EasyMock.expect(
            mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                clientId, resourceId)).andReturn(resource);

        mockClientDao.grantPermissionToClient(getFakePermission(), client);
        EasyMock.replay(mockClientDao);

        clientService.grantPermission(clientId, resource);

        EasyMock.verify(mockClientDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGrantPermissionBecauseTargetClientDoesNotExist() {
        Permission resource = getFakePermission();
        Client client = getFakeClient();
        String clientId = client.getClientId();

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(null);

        mockClientDao.updateClient(client);
        EasyMock.replay(mockClientDao);

        clientService.grantPermission(clientId, resource);

        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotGrantPermissionBecausePermissionDoesNotExist() {
        Permission resource = getFakePermission();
        Client client = getFakeClient();
        String clientId = client.getClientId();

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(client);
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(null);

        mockClientDao.updateClient(client);
        EasyMock.replay(mockClientDao);

        clientService.grantPermission(clientId, resource);

        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotRevokePermissionBecauseTargetClientDoesNotExist() {
        Permission resource = getFakePermission();
        Client client = getFakeClient();
        String clientId = client.getClientId();

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(null);

        mockClientDao.updateClient(client);
        EasyMock.replay(mockClientDao);

        clientService.revokePermission(clientId, resource);

        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotRevokePermissionBecausePermissionDoesNotExist() {
        Permission resource = getFakePermission();
        Client client = getFakeClient();
        String clientId = client.getClientId();

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(client);
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(null);

        mockClientDao.updateClient(client);
        EasyMock.replay(mockClientDao);

        clientService.revokePermission(clientId, resource);

        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldGetClientGroupsForUser() {
        String[] groupIds = {"first", "second"};
        EasyMock.expect(mockUserDao.getGroupIdsForUser(username)).andReturn(groupIds);
        EasyMock.replay(mockUserDao);
        
        ClientGroup group1 = getFakeClientGroup();
        ClientGroup group2 = getFakeClientGroup();
        group2.setName("Some Other Name");
        group2.setType("Another Type");
        
        EasyMock.expect(mockClientDao.getClientGroupByUniqueId("first")).andReturn(group1);
        EasyMock.expect(mockClientDao.getClientGroupByUniqueId("second")).andReturn(group2);
        EasyMock.replay(mockClientDao);
        
        List<ClientGroup> groups = clientService.getClientGroupsForUser(username);
        
        Assert.assertTrue(groups.size() == 2);
        Assert.assertTrue(groups.get(0).getName().equals(groupName));
    }
    
    @Test
    public void shouldGetNoClientGroupsForUserWithNoMemberships() {
        EasyMock.expect(mockUserDao.getGroupIdsForUser(username)).andReturn(null);
        EasyMock.replay(mockUserDao);
        
        List<ClientGroup> groups = clientService.getClientGroupsForUser(username);
        
        Assert.assertTrue(groups.size() == 0);
    }
    
    @Test
    public void shouldGetClientsThatHavePermission() {
        List<Client> clients = new ArrayList<Client>();
        clients.add(getFakeClient());
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(getFakePermission());
        EasyMock.expect(mockClientDao.getClientsThatHavePermission(getFakePermission())).andReturn(clients);
        EasyMock.replay(mockClientDao);
        
        List<Client> returnedClients = clientService.getClientsThatHavePermission(getFakePermission());
        
        Assert.assertTrue(returnedClients.size() == 1);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotGetClientsThatHavePermissionForNonExistentPermission() {
        EasyMock.expect(mockClientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, resourceId)).andReturn(null);
        EasyMock.replay(mockClientDao);
        
        List<Client> returnedClients = clientService.getClientsThatHavePermission(getFakePermission());
        
        Assert.assertTrue(returnedClients.size() == 1);
    }
    @Test
    public void shouldRevokePermission() {
        Permission resource = getFakePermission();
        Client client = getFakeClient();
        String clientId = client.getClientId();

        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(
            client);
        EasyMock.expect(
            mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                clientId, resourceId)).andReturn(resource);

        mockClientDao.revokePermissionFromClient(resource, client);
        EasyMock.replay(mockClientDao);

        clientService.revokePermission(clientId, resource);

        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldResetClientSecret() {

        Client client = getFakeClient();
        String oldSecret = client.getClientSecret();

        mockClientDao.updateClient(client);
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
    public void shouldUpdateClientGroup() {
        ClientGroup resource = getFakeClientGroup();
        mockClientDao.updateClientGroup(resource);
        EasyMock.replay(mockClientDao);

        clientService.updateClientGroup(resource);

        EasyMock.verify(mockClientDao);
    }

    @Test
    public void ShouldGetClientGroup() {
        ClientGroup group = getFakeClientGroup();
        EasyMock.expect(
            mockClientDao.getClientGroup(customerId, clientId, groupName))
            .andReturn(group);
        EasyMock.replay(mockClientDao);
        ClientGroup returnedGroup = clientService.getClientGroup(customerId,
            clientId, groupName);
        Assert.assertTrue(returnedGroup.getName().equals(group.getName()));
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void ShouldGetClientGroups() {
        ClientGroup group = getFakeClientGroup();
        List<ClientGroup> groups = new ArrayList<ClientGroup>();
        groups.add(group);
        groups.add(group);
        EasyMock.expect(mockClientDao.getClientGroupsByClientId(clientId))
            .andReturn(groups);
        EasyMock.replay(mockClientDao);
        List<ClientGroup> returnedGroups = clientService
            .getClientGroupsByClientId(clientId);
        Assert.assertTrue(returnedGroups.size() == 2);
        EasyMock.verify(mockClientDao);
    }
  
    @Test
    public void shouldAddUserToCustomerClientGroup() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        String customerId = "RACK-123-456";
        EasyMock.expect(mockUserDao.getUserByUsername(user.getUsername())).andReturn(user);
        EasyMock.replay(mockUserDao);
        
        EasyMock.expect(mockClientDao.getClientGroup(customerId, group.getClientId(), group.getName())).andReturn(group); 
        mockClientDao.addUserToClientGroup(user.getUniqueId(), group);
        EasyMock.replay(mockClientDao);
        
        clientService.addUserToClientGroup(user.getUsername(), customerId, group.getClientId(), group.getName());
        
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = com.rackspace.idm.exception.NotFoundException.class)
    public void shouldNotAddUserToClientGroupForBlankUsername() {
        ClientGroup group = getFakeClientGroup();
        clientService.addUserToClientGroup(null, "RACKCUSTOMER", "RACKCLIENT", group.getName());
    }

    @Test(expected = com.rackspace.idm.exception.NotFoundException.class)
    public void shouldNotAddUserToClientGroupForBadClientGroup() {
        ClientGroup group = getFakeClientGroup();
        group.setName(null);
        clientService.addUserToClientGroup("bob", "RACKCUSTOMER", "RACKCLIENT", null);
    }

    @Test(expected = com.rackspace.idm.exception.NotFoundException.class)
    public void shouldNotAddUserToClientGroupForBadClientGroup3() {
        ClientGroup group = getFakeClientGroup();
        group.setClientId(null);
        clientService.addUserToClientGroup("bob", "RACKCUSTOMER", null, group.getName());
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotAddUserToClientGroupForUserNotFound() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(null);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockClientDao.getClientGroup("RACKCUSTOMER", "RACKCLIENT", group.getName())).andReturn(group);
        EasyMock.replay(mockClientDao);
        clientService.addUserToClientGroup(username, "RACKCUSTOMER", "RACKCLIENT", group.getName());
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotAddUserToClientGroupForGroupNotFound() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andThrow(new NotFoundException());
        EasyMock.replay(mockUserDao);
        clientService.addUserToClientGroup(username, "RACKCUSTOMER", "RACKCLIENT", group.getName());
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldAddUserToClientGroupForGroupNotFoundThatUserIsAlreadyAMemberOf() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockClientDao.getClientGroup("RACKCUSTOMER", "RACKCLIENT", group.getName())).andReturn(group);
        mockClientDao.addUserToClientGroup(user.getUniqueId(), group);
        EasyMock.replay(mockClientDao);
        clientService.addUserToClientGroup(username, "RACKCUSTOMER", "RACKCLIENT", group.getName());
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldRemoveUserFromClientGroup() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);
        mockClientDao.removeUserFromGroup(user.getUniqueId(), group);
        EasyMock.replay(mockClientDao);
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId)).andReturn(getFakeCustomer());
        EasyMock.replay(mockCustomerDao);
        clientService.removeUserFromClientGroup(username, group);
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotRemoveUserFromClientGroupForBlankUserName() {
        clientService.removeUserFromClientGroup("", null);
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotRemoveUserFromClientGroupForNonExistentUser() {
        ClientGroup group = getFakeClientGroup();
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(null);
        EasyMock.replay(mockUserDao);
        clientService.removeUserFromClientGroup(username, group);
    }
    
    @Test
    public void shouldRemoveUserFromClientGroupThatUserIsNotAMemberOf() {
        ClientGroup group = getFakeClientGroup();
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId)).andReturn(getFakeCustomer());
        EasyMock.replay(mockCustomerDao);
        mockClientDao.removeUserFromGroup(user.getUniqueId(), group);
        EasyMock.expectLastCall().andThrow(new NotFoundException());
        EasyMock.replay(mockClientDao);
        clientService.removeUserFromClientGroup(username, group);
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void ShouldAddClientGroup() {
        Client client = getFakeClient();
        client.setUniqueId(uniqueId);
        
        ClientGroup group = getFakeClientGroup();
        EasyMock.expect(mockClientDao.getClientByClientId(clientId)).andReturn(client);
        mockClientDao.addClientGroup(group, uniqueId);
        EasyMock.replay(mockClientDao);
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId)).andReturn(getFakeCustomer());
        EasyMock.replay(mockCustomerDao);
        clientService.addClientGroup(group);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void ShouldDeleteClientGroup() {
        mockClientDao.deleteClientGroup(customerId, clientId, groupName);
        EasyMock.replay(mockClientDao);
        clientService.deleteClientGroup(customerId, clientId, groupName);
        EasyMock.verify(mockClientDao);
    }
    
    @Test 
    public void ShouldGetNoClientGroupsForUserWithNoMemberships() {
        EasyMock.expect(mockUserDao.getGroupIdsForUser(username)).andReturn(null);
        EasyMock.replay(mockUserDao);
        
        List<ClientGroup> groups = clientService.getClientGroupsForUserByClientIdAndType(username, clientId, groupType);
        
        Assert.assertTrue(groups.size() == 0);
    }
    
    @Test 
    public void ShouldGetClientGroupsForUserWithFilteredByClientIdAndType() {
        String[] groupIds = {"first", "second"};
        EasyMock.expect(mockUserDao.getGroupIdsForUser(username)).andReturn(groupIds);
        EasyMock.replay(mockUserDao);
        
        ClientGroup group1 = getFakeClientGroup();
        ClientGroup group2 = getFakeClientGroup();
        group2.setName("Some Other Name");
        group2.setType("Another Type");
        
        EasyMock.expect(mockClientDao.getClientGroupByUniqueId("first")).andReturn(group1);
        EasyMock.expect(mockClientDao.getClientGroupByUniqueId("second")).andReturn(group2);
        EasyMock.replay(mockClientDao);
        
        List<ClientGroup> groups = clientService.getClientGroupsForUserByClientIdAndType(username, clientId, groupType);
        
        Assert.assertTrue(groups.size() == 1);
        Assert.assertTrue(groups.get(0).getName().equals(groupName));
    }
    
    @Test 
    public void ShouldGetClientGroupsForUserWithFilteredByClientId() {
        String[] groupIds = {"first", "second"};
        EasyMock.expect(mockUserDao.getGroupIdsForUser(username)).andReturn(groupIds);
        EasyMock.replay(mockUserDao);
        
        ClientGroup group1 = getFakeClientGroup();
        ClientGroup group2 = getFakeClientGroup();
        group2.setName("Some Other Name");
        group2.setType("Another Type");
        
        EasyMock.expect(mockClientDao.getClientGroupByUniqueId("first")).andReturn(group1);
        EasyMock.expect(mockClientDao.getClientGroupByUniqueId("second")).andReturn(group2);
        EasyMock.replay(mockClientDao);
        
        List<ClientGroup> groups = clientService.getClientGroupsForUserByClientIdAndType(username, clientId, null);
        
        Assert.assertTrue(groups.size() == 2);
    }
    
    @Test 
    public void ShouldGetClientGroupsForUserWithNoFilters() {
        String[] groupIds = {"first", "second"};
        EasyMock.expect(mockUserDao.getGroupIdsForUser(username)).andReturn(groupIds);
        EasyMock.replay(mockUserDao);
        
        ClientGroup group1 = getFakeClientGroup();
        ClientGroup group2 = getFakeClientGroup();
        group2.setName("Some Other Name");
        group2.setType("Another Type");
        
        EasyMock.expect(mockClientDao.getClientGroupByUniqueId("first")).andReturn(group1);
        EasyMock.expect(mockClientDao.getClientGroupByUniqueId("second")).andReturn(group2);
        EasyMock.replay(mockClientDao);
        
        List<ClientGroup> groups = clientService.getClientGroupsForUserByClientIdAndType(username, null, null);
        
        Assert.assertTrue(groups.size() == 2);
    }

    private Client getFakeClient() {
        return new Client(clientId, clientSecret, name, inum, iname,
            customerId, status);
    }

    private Customer getFakeCustomer() {
        return new Customer(customerId, customerInum, customerIname,
            customerStatus, customerSeeAlso, owner);
    }

    private List<Permission> getFakePermissionList() {
        List<Permission> perms = new ArrayList<Permission>();
        perms.add(getFakePermission());
        return perms;
    }

    private Permission getFakePermission() {
        Permission res = new Permission();
        res.setClientId(clientId);
        res.setCustomerId(customerId);
        res.setPermissionId(resourceId);
        res.setValue(resourceValue);
        return res;
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
        return user;
    }
}
