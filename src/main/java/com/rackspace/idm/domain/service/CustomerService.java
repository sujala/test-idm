package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Customer;

public interface CustomerService {

    void addCustomer(Customer customer);

    Customer getCustomer(String customerId);
    
    Customer loadCustomer(String customerId);

    void deleteCustomer(String customerId);
    
//    void setCustomerLocked(Customer customer, boolean locked);

//    void softDeleteCustomer(String customerId);

    void updateCustomer(Customer customer);

    void softDeleteCustomer(Customer customer);

	void setApplicationDao(ApplicationDao applicationDao);

	void setCustomerDao(CustomerDao customerDao);

	void setUserDao(UserDao userDao);
}
