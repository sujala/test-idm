package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientSecret;
import com.rackspace.idm.entities.ClientStatus;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.CustomerStatus;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.DefaultClientService;
import com.rackspace.idm.test.stub.StubLogger;

public class ClientServiceTests {
    ClientDao mockClientDao;
    CustomerDao mockCustomerDao;
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

    @Before
    public void setUp() throws Exception {

        mockClientDao = EasyMock.createMock(ClientDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);

        clientService = new DefaultClientService(mockClientDao, mockCustomerDao,
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
            mockClientDao.authenticate(clientId, clientSecret.getValue()))
            .andReturn(true);
        EasyMock.replay(mockClientDao);

        boolean authenticated = clientService.authenticate(clientId,
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
    public void shouldGetUsersByCustomerId() {
        List<Client> clients = new ArrayList<Client>();
        clients.add(getFakeClient());
        clients.add(getFakeClient());
        
        EasyMock.expect(mockClientDao.getByCustomerId(customerId)).andReturn(clients);
        EasyMock.replay(mockClientDao);
        
        List<Client> returned = clientService.getByCustomerId(customerId);
        
        Assert.assertTrue(returned.size() == 2);
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
}
