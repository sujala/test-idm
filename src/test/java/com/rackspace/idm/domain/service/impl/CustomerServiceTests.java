package com.rackspace.idm.domain.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.UserService;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;

public class CustomerServiceTests {
    CustomerService service;

    ApplicationService applicationService;
    CustomerDao mockCustomerDao;
    UserService userService;

    String customerId = "CustomerId";
    String username = "username";
    String clientId = "clientId";
    
    String id = "XXX";

    @Before
    public void setUp() throws Exception {
        applicationService = EasyMock.createMock(ApplicationService.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        userService = EasyMock.createMock(UserService.class);

        service = new DefaultCustomerService();
        service.setApplicationService(applicationService);
        service.setCustomerDao(mockCustomerDao);
        service.setUserService(userService);
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
        EasyMock.expect(userService.getAllUsers(EasyMock.anyObject(FilterParam[].class), EasyMock.eq(0), EasyMock.eq(100))).andReturn(getFakeUsers());
        userService.deleteUser(username);
        EasyMock.replay(userService);
        EasyMock.expect(applicationService.getByCustomerId(customerId, 0, 100)).andReturn(getFakeClients());
        applicationService.delete(EasyMock.anyObject(String.class));
        EasyMock.replay(applicationService);
        service.deleteCustomer(customerId);
        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(userService);
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
