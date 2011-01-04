package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.RoleDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.exceptions.DuplicateException;

public class DefaultCustomerService implements CustomerService {

    private ClientDao clientDao;
    private CustomerDao customerDao;
    private RoleDao roleDao;
    private UserDao userDao;
    
    private Logger logger;

    public DefaultCustomerService(ClientDao clientDao,
        CustomerDao customerDao, RoleDao roleDao, UserDao userDao,
        Logger logger) {
        
        this.clientDao = clientDao;
        this.customerDao = customerDao;
        this.roleDao = roleDao;
        this.userDao = userDao;
        
        this.logger = logger;
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

        for (Role role : getDefaultRoles(customer)) {
            this.roleDao.add(role);
        }

        logger.info("Added Customer: {}", customer);
    }

    public void deleteCustomer(String customerId) {
        logger.info("Deleting Customer: {}", customerId);
        this.customerDao.delete(customerId);
        logger.info("Deleted Customer: {}", customerId);
    }

    public Customer getCustomer(String customerId) {
        logger.debug("Getting Customer: {}", customerId);
        Customer customer = customerDao.findByCustomerId(customerId);
        logger.debug("Got Customer: {}", customer);
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

    private List<Role> getDefaultRoles(Customer customer) {

        List<Role> roles = new ArrayList<Role>();

        // Default Admin Role
        Role role = new Role();
        role.setCustomerId(customer.getCustomerId());
        role.setInum(customer.getInum() + "!0001");
        role.setName(GlobalConstants.IDM_ADMIN_ROLE_NAME);
        role.setOrgInum(customer.getInum());
        role.setOwner(GlobalConstants.INUM_PREFIX + customer.getInum());
         role.setType("rackspacePredefined");

        roles.add(role);

        return roles;
    }
}
