package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultCustomerService implements CustomerService {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private UserService userService;
    @Autowired
    private CustomerDao customerDao;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void addCustomer(Customer customer) {
        logger.info("Adding Customer: {}", customer);

        Customer exists = customerDao
            .getCustomerByCustomerId(customer.getRcn());

        if (exists != null) {
            String errMsg = String.format("Couldn't add customer %s because customerId already taken", customer.getRcn());
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
            userService.deleteUser(user.getUsername());
        }
        
        List<Application> clients = this.getClientListForCustomerId(customerId);
        for (Application client : clients) {
            applicationService.delete(client.getClientId());
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
    public Customer loadCustomer(String customerId) {
        logger.debug("Loading Customer: {}", customerId);
        Customer customer = getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s", customerId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        logger.debug("Loaded Customer: {}", customer);
        
        return customer;
    }
    
    @Override
    public void updateCustomer(Customer customer) {
        logger.info("Updating Customer: {}", customer);
        this.customerDao.updateCustomer(customer);
        logger.info("Updated Customer: {}", customer);
    }
    
    @Override
    public void softDeleteCustomer(Customer customer) {
        logger.debug("SoftDeleting Customer: {}", customer);
        customerDao.softDeleteCustomer(customer);
        logger.debug("SoftDeleted Customer: {}", customer);
    }
    
//    /**
//     * does some processing once a customer like enable or disable all users of
//     * that belong to that customer, revoke tokens, etc
//     * @param customer
//     * @param locked - true/false
//     */
//    private void process(Customer customer, boolean locked) {
//        logger.info("Setting customer's locked state: {}", customer);
//
//        String customerId = customer.getRcn();
//
//        // locks/unlockes all users under customer
//        userService.setUsersLockedFlagByCustomerId(customerId, locked);
//
//        // locks/unlocks all applications under customer
//        applicationService.setClientsLockedFlagByCustomerId(customerId, locked);
//       
//        // revoke all tokens for customer
//    	oauthService.revokeAllTokensForCustomer(customerId);
//
//        logger.info("Locked customer: {}", customer);
//    }
    
    private List<User> getUserListForCustomerId(String customerId) {
    	FilterParam[] filters = new FilterParam[] { new FilterParam(FilterParamName.RCN, customerId)};
        int offset = 0;
        final int limit = 100;
        List<User> userList = new ArrayList<User>();
        Users users = null;

        do {
            users = userService.getAllUsers(filters, offset, limit);
            offset = offset + limit;
            userList.addAll(users.getUsers());

        } while (offset <= users.getTotalRecords());

        return userList;
    }
    
    private List<Application> getClientListForCustomerId(String customerId) {
        int offset = 0;
        final int limit = 100;
        List<Application> clientList = new ArrayList<Application>();
        Applications clients = null;

        do {
            clients = applicationService.getByCustomerId(customerId, offset, limit);
            offset = offset + limit;
            clientList.addAll(clients.getClients());

        } while (offset <= clients.getTotalRecords());

        return clientList;
    }


	@Override
	public void setApplicationService(ApplicationService applicationDao) {
		this.applicationService = applicationDao;
	}


	@Override
	public void setCustomerDao(CustomerDao customerDao) {
		this.customerDao = customerDao;
	}


	@Override
	public void setUserService(UserService userService) {
		this.userService = userService;
	}
}
