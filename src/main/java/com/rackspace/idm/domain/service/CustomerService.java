package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.CustomerDao;
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

	void setApplicationService(ApplicationService applicationService);

	void setCustomerDao(CustomerDao customerDao);

	void setUserService(UserService userService);
}
