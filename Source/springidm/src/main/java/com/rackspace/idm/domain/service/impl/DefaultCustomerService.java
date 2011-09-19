package com.rackspace.idm.domain.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;

public class DefaultCustomerService implements CustomerService {

    private final ClientDao clientDao;
    private final CustomerDao customerDao;
    private final UserDao userDao;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultCustomerService(ClientDao clientDao, CustomerDao customerDao,
        UserDao userDao) {

        this.clientDao = clientDao;
        this.customerDao = customerDao;
        this.userDao = userDao;
    }

    
    @Override
    public void addCustomer(Customer customer) {
        logger.info("Adding Customer: {}", customer);

        Customer exists = customerDao
            .getCustomerByCustomerId(customer.getRCN());

        if (exists != null) {
            String errMsg = String.format("Couldn't add customer %s because customerId already taken", customer.getRCN());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }

        customer.setId(this.customerDao.getNextCustomerId());

        this.customerDao.addCustomer(customer);

        logger.info("Added Customer: {}", customer);
    }

    
    @Override
    public void deleteCustomer(String customerId) {
        logger.info("Deleting Customer: {}", customerId);
        
        Customer customer = customerDao.getCustomerByCustomerId(customerId);
        if (customer == null) {
            String errMsg = String.format("Customer with customerId %s not found.", customerId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        List<User> users = this.getUserListForCustomerId(customerId);
        for (User user : users) {
            userDao.deleteUser(user.getUsername());
        }
        
        List<Client> clients = this.getClientListForCustomerId(customerId);
        for (Client client : clients) {
            clientDao.deleteClient(client);
        }
        
        this.customerDao.deleteCustomer(customerId);
        logger.info("Deleted Customer: {}", customerId);
    }

    
    @Override
    public Customer getCustomer(String customerId) {
        logger.debug("Getting Customer: {}", customerId);
        Customer customer = customerDao.getCustomerByCustomerId(customerId);
        logger.debug("Got Customer: {}", customer);
        return customer;
    }

    
    @Override
    public void setCustomerLocked(Customer customer, boolean locked) {
        logger.info("Setting customer's locked state: {}", customer);

        String customerId = customer.getRCN();

        // lock all users under customer
        userDao.setUsersLockedFlagByCustomerId(customerId, locked);

        // lock all applications under customer
        clientDao.setClientsLockedFlagByCustomerId(customerId, locked);

        // lock customer
        customer.setLocked(locked);
        customerDao.updateCustomer(customer);

        logger.info("Locked customer: {}", customer);
    }

    
    @Override
    public void softDeleteCustomer(String customerId) {
        logger.info("Soft Deleting customer: {}", customerId);
        Customer customer = this.customerDao.getCustomerByCustomerId(customerId);
        customer.setSoftDeleted(true);
        this.customerDao.updateCustomer(customer);
        logger.info("Soft Deleted customer: {}", customerId);
    }

    
    @Override
    public void updateCustomer(Customer customer) {
        logger.info("Updating Customer: {}", customer);
        this.customerDao.updateCustomer(customer);
        logger.info("Updated Customer: {}", customer);
    }
    
    private List<User> getUserListForCustomerId(String customerId) {
        int offset = 0;
        int limit = 100;
        List<User> userList = new ArrayList<User>();
        Users users = null;

        do {
            users = userDao.getUsersByCustomerId(customerId, offset, limit);
            offset = offset + limit;
            userList.addAll(users.getUsers());

        } while (offset <= users.getTotalRecords());

        return userList;
    }
    
    private List<Client> getClientListForCustomerId(String customerId) {
        int offset = 0;
        int limit = 100;
        List<Client> clientList = new ArrayList<Client>();
        Clients clients = null;

        do {
            clients = clientDao.getClientsByCustomerId(customerId, offset, limit);
            offset = offset + limit;
            clientList.addAll(clients.getClients());

        } while (offset <= clients.getTotalRecords());

        return clientList;
    }
}
