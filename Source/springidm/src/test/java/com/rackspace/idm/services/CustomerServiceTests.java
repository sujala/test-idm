package com.rackspace.idm.services;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.RoleDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.CustomerStatus;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.test.stub.StubLogger;

public class CustomerServiceTests {
    CustomerService service;
    
    ClientDao mockClientDao;
    CustomerDao mockCustomerDao;
    RoleDao mockRoleDao;
    UserDao mockUserDao;
    
    String customerId = "CustomerId";
    String customerName = "Name";
    String customerInum = "Inum";
    String customerIname = "Iname";
    CustomerStatus customerStatus = CustomerStatus.ACTIVE;
    String customerSeeAlso = "SeeAlso";
    String customerOwner = "Owner";
    String customerCountry = "USA";
    
    @Before
    public void setUp() throws Exception {

        mockClientDao = EasyMock.createMock(ClientDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        mockRoleDao = EasyMock.createMock(RoleDao.class);
        mockUserDao = EasyMock.createMock(UserDao.class);

        service = new DefaultCustomerService(mockClientDao, mockCustomerDao, 
            mockRoleDao, mockUserDao,
            new StubLogger());
    }
    
    @Test
    public void shouldAddCustomer() {
        Customer customer = getFakeCustomer();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId)).andReturn(null);
        EasyMock.expect(mockCustomerDao.getUnusedCustomerInum()).andReturn("Inum");
        mockCustomerDao.add(customer);
        EasyMock.replay(mockCustomerDao);
        
        mockRoleDao.add(EasyMock.anyObject(Role.class));
        EasyMock.replay(mockRoleDao);
        
        service.addCustomer(customer);
        
        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockRoleDao);
    }
    
    @Test(expected = DuplicateException.class)
    public void shouldNotAddDuplicateCustomer() {
        Customer customer = getFakeCustomer();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId)).andReturn(customer);
        EasyMock.expect(mockCustomerDao.getUnusedCustomerInum()).andReturn("Inum");
        mockCustomerDao.add(customer);
        EasyMock.replay(mockCustomerDao);
        
        mockRoleDao.add(EasyMock.anyObject(Role.class));
        EasyMock.replay(mockRoleDao);
        
        service.addCustomer(customer);
        
        EasyMock.verify(mockCustomerDao);
        EasyMock.verify(mockRoleDao);
    }
    
    @Test
    public void shouldDeleteCustomer() {
        mockCustomerDao.delete(customerId);
        EasyMock.replay(mockCustomerDao);
        service.deleteCustomer(customerId);
        EasyMock.verify(mockCustomerDao);
    }
    
    @Test
    public void shouldGetCustomer() {
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId)).andReturn(getFakeCustomer());
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
        
        mockUserDao.setAllUsersLocked(customerId, locked);
        EasyMock.replay(mockUserDao);
        
        mockClientDao.setAllClientLocked(customerId, locked);
        EasyMock.replay(mockClientDao);
        
        mockCustomerDao.save(customer);
        EasyMock.replay(mockCustomerDao);
        
        service.setCustomerLocked(customer, locked);
        
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientDao);
        EasyMock.verify(mockCustomerDao);
    }
    
    @Test
    public void shouldSoftDeleteCustomer() {
        Customer customer = getFakeCustomer();
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId)).andReturn(customer);
        customer.setSoftDeleted(true);
        mockCustomerDao.save(customer);
        EasyMock.replay(mockCustomerDao);
        
        service.softDeleteCustomer(customerId);
        
        EasyMock.verify(mockCustomerDao);
    }
    
    @Test
    public void shouldUpdateCustomer() {
        Customer customer = getFakeCustomer();
        mockCustomerDao.save(customer);
        EasyMock.replay(mockCustomerDao);
        service.updateCustomer(customer);
        EasyMock.verify(mockCustomerDao);
    }
    
    private Customer getFakeCustomer() {
        return new Customer(customerId, customerInum, customerIname,
        customerStatus, customerSeeAlso, customerOwner);
    }
}
