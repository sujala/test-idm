package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.impl.DefaultCustomerService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.test.stub.StubLogger;

public class CustomerServiceTests {
    CustomerService service;

    ClientDao mockClientDao;
    CustomerDao mockCustomerDao;
    UserDao mockUserDao;

    String customerId = "CustomerId";
    String customerName = "Name";
    String customerInum = "Inum";
    String customerIname = "Iname";
    CustomerStatus customerStatus = CustomerStatus.ACTIVE;
    String customerSeeAlso = "SeeAlso";
    String customerOwner = "Owner";
    String customerCountry = "USA";
    String username = "username";
    String clientId = "clientId";

    @Before
    public void setUp() throws Exception {

        mockClientDao = EasyMock.createMock(ClientDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        mockUserDao = EasyMock.createMock(UserDao.class);

        service = new DefaultCustomerService(mockClientDao, mockCustomerDao,
            mockUserDao);
    }

    @Test
    public void shouldAddCustomer() {
        Customer customer = getFakeCustomer();

        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(null);
        EasyMock.expect(mockCustomerDao.getUnusedCustomerInum()).andReturn(
            "Inum");
        mockCustomerDao.addCustomer(customer);
        EasyMock.replay(mockCustomerDao);

        service.addCustomer(customer);

        EasyMock.verify(mockCustomerDao);
    }

    @Test(expected = DuplicateException.class)
    public void shouldNotAddDuplicateCustomer() {
        Customer customer = getFakeCustomer();

        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(customer);
        EasyMock.expect(mockCustomerDao.getUnusedCustomerInum()).andReturn(
            "Inum");
        mockCustomerDao.addCustomer(customer);
        EasyMock.replay(mockCustomerDao);

        service.addCustomer(customer);

        EasyMock.verify(mockCustomerDao);
    }

    @Test
    public void shouldDeleteCustomer() {
        mockCustomerDao.deleteCustomer(customerId);
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId)).andReturn(getFakeCustomer());
        EasyMock.replay(mockCustomerDao);   
        EasyMock.expect(mockUserDao.getUsersByCustomerId(customerId, 0, 100)).andReturn(getFakeUsers());
        mockUserDao.deleteUser(username);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockClientDao.getClientsByCustomerId(customerId, 0, 100)).andReturn(getFakeClients());
        mockClientDao.deleteClient(getFakeClient());
        EasyMock.replay(mockClientDao);
        service.deleteCustomer(customerId);
        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockUserDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteNonExistentCustomer() {
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId)).andReturn(null);
        EasyMock.replay(mockCustomerDao);   
        service.deleteCustomer(customerId);
    }

    @Test
    public void shouldGetCustomer() {
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(getFakeCustomer());
        EasyMock.replay(mockCustomerDao);
        Customer customer = service.getCustomer(customerId);

        Assert.assertTrue(customer.getInum().equals(customerInum));
        EasyMock.verify(mockCustomerDao);
    }

    @Test
    public void shouldSetCustomerLocked() {

        Customer customer = getFakeCustomer();
        String customerId = customer.getCustomerId();
        boolean locked = true;

        mockUserDao.setUsersLockedFlagByCustomerId(customerId, locked);
        EasyMock.replay(mockUserDao);

        mockClientDao.setClientsLockedFlagByCustomerId(customerId, locked);
        EasyMock.replay(mockClientDao);

        mockCustomerDao.updateCustomer(customer);
        EasyMock.replay(mockCustomerDao);

        service.setCustomerLocked(customer, locked);

        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
        EasyMock.verify(mockCustomerDao);
    }

    @Test
    public void shouldSoftDeleteCustomer() {
        Customer customer = getFakeCustomer();

        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(customer);
        customer.setSoftDeleted(true);
        mockCustomerDao.updateCustomer(customer);
        EasyMock.replay(mockCustomerDao);

        service.softDeleteCustomer(customerId);

        EasyMock.verify(mockCustomerDao);
    }

    @Test
    public void shouldUpdateCustomer() {
        Customer customer = getFakeCustomer();
        mockCustomerDao.updateCustomer(customer);
        EasyMock.replay(mockCustomerDao);
        service.updateCustomer(customer);
        EasyMock.verify(mockCustomerDao);
    }

    private Customer getFakeCustomer() {
        return new Customer(customerId, customerInum, customerIname,
            customerStatus, customerSeeAlso, customerOwner);
    }
    
    private User getFakeUser() {
        return new User(username);
    }
    
    private Users getFakeUsers() {
        Users users = new Users();
        List<User> userList = new ArrayList<User>();
        userList.add(getFakeUser());
        users.setLimit(100);
        users.setOffset(0);
        users.setTotalRecords(1);
        users.setUsers(userList);
        return users;
    }
    
    private Client getFakeClient() {
        Client client = new Client();
        client.setClientId(clientId);
        return client;
    }
    
    private Clients getFakeClients() {
        Clients clients = new Clients();
        List<Client> clientList = new ArrayList<Client>();
        clientList.add(getFakeClient());
        clients.setLimit(100);
        clients.setOffset(0);
        clients.setTotalRecords(1);
        clients.setClients(clientList);
        return clients;
    }
}
