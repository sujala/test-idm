package com.rackspace.idm.domain.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;

public class CustomerServiceTests {
    CustomerService service;

    ApplicationDao applicationDao;
    CustomerDao mockCustomerDao;
    UserDao mockUserDao;

    String customerId = "CustomerId";
    String username = "username";
    String clientId = "clientId";
    
    String id = "XXX";

    @Before
    public void setUp() throws Exception {
        applicationDao = EasyMock.createMock(ApplicationDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        mockUserDao = EasyMock.createMock(UserDao.class);

        service = new DefaultCustomerService();
        service.setApplicationDao(applicationDao);
        service.setCustomerDao(mockCustomerDao);
        service.setUserDao(mockUserDao);
    }

    @Test
    public void shouldAddCustomer() {
        Customer customer = getFakeCustomer();

        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(null);
        EasyMock.expect(mockCustomerDao.getNextCustomerId()).andReturn(id);
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
        EasyMock.expect(mockUserDao.getAllUsers(EasyMock.anyObject(FilterParam[].class), EasyMock.eq(0), EasyMock.eq(100))).andReturn(getFakeUsers());
        mockUserDao.deleteUser(username);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(applicationDao.getClientsByCustomerId(customerId, 0, 100)).andReturn(getFakeClients());
        applicationDao.deleteClient(EasyMock.anyObject(Application.class));
        EasyMock.replay(applicationDao);
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
    	Customer fakeCustomer = getFakeCustomer();
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
            .andReturn(fakeCustomer);
        EasyMock.replay(mockCustomerDao);
        Customer customer = service.getCustomer(customerId);

        assertEquals(fakeCustomer, customer);
        EasyMock.verify(mockCustomerDao);
    }
    
    @Test
    public void shouldLoadCustomer() {
    	Customer fakeCustomer = getFakeCustomer();
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId)).andReturn(fakeCustomer);
        EasyMock.replay(mockCustomerDao);
        Customer customer = service.loadCustomer(customerId);

        assertEquals(fakeCustomer, customer);
        EasyMock.verify(mockCustomerDao);
    }
    
    @Test(expected=NotFoundException.class)
    public void shouldThrowExceptionWhenLoadingNonExistentCustomer() {
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId)).andReturn(null);
        EasyMock.replay(mockCustomerDao);
        service.loadCustomer(customerId);
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
        Customer customer = new Customer();
        customer.setRcn(customerId);
        return customer;
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
    
    private Application getFakeClient() {
        Application client = new Application();
        client.setClientId(clientId);
        return client;
    }
    
    private Applications getFakeClients() {
        Applications clients = new Applications();
        List<Application> clientList = new ArrayList<Application>();
        clientList.add(getFakeClient());
        clients.setLimit(100);
        clients.setOffset(0);
        clients.setTotalRecords(1);
        clients.setClients(clientList);
        return clients;
    }
}
