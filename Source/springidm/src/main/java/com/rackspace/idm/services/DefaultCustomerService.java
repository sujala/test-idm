package com.rackspace.idm.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.exceptions.DuplicateException;

public class DefaultCustomerService implements CustomerService {

    private ClientDao clientDao;
    private CustomerDao customerDao;
    private UserDao userDao;
    
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultCustomerService(ClientDao clientDao,
        CustomerDao customerDao, UserDao userDao) {
        
        this.clientDao = clientDao;
        this.customerDao = customerDao;
        this.userDao = userDao;
    }

    public void addCustomer(Customer customer) {
        logger.info("Adding Customer: {}", customer);

        Customer exists = customerDao
            .findByCustomerId(customer.getCustomerId());

        if (exists != null) {
            logger.warn(
                "Couldn't add customer {} because customerId already taken",
                customer);
            throw new DuplicateException(String.format(
                "CustomerId %s already exists", customer.getCustomerId()));
        }

        customer.setInum(customerDao.getUnusedCustomerInum());

        this.customerDao.add(customer);

        logger.info("Added Customer: {}", customer);
    }

    public void deleteCustomer(String customerId) {
        logger.info("Deleting Customer: {}", customerId);
        this.customerDao.delete(customerId);
        logger.info("Deleted Customer: {}", customerId);
    }

    public Customer getCustomer(String customerId) {
        logger.info("Getting Customer: {}", customerId);
        Customer customer = customerDao.findByCustomerId(customerId);
        logger.info("Got Customer: {}", customer);
        return customer;
    }
    
    public void setCustomerLocked(Customer customer, boolean locked) { 
        logger.info("Setting customer's locked state: {}", customer);
        
        String customerId = customer.getCustomerId();
        
        // lock all users under customer
        userDao.setAllUsersLocked(customerId, locked);
        
        // lock all applications under customer
        clientDao.setAllClientLocked(customerId, locked);
        
        // lock customer
        customer.setIsLocked(locked);
        customerDao.save(customer);
        
        logger.info("Locked customer: {}", customer);
    }
    
    public void softDeleteCustomer(String customerId) {
        logger.info("Soft Deleting customer: {}", customerId);
        Customer customer = this.customerDao.findByCustomerId(customerId);
        customer.setSoftDeleted(true);
        this.customerDao.save(customer);
        logger.info("Soft Deleted customer: {}", customerId);
    }

    public void updateCustomer(Customer customer) {
        logger.info("Updating Customer: {}", customer);
        this.customerDao.save(customer);
        logger.info("Updated Customer: {}", customer);
    }
}
