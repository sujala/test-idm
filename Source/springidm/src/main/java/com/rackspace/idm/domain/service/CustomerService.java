package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Customer;

public interface CustomerService {

    void addCustomer(Customer customer);

    Customer getCustomer(String customerId);

    void deleteCustomer(String customerId);
    
    void setCustomerLocked(Customer customer, boolean locked);

    void softDeleteCustomer(String customerId);

    void updateCustomer(Customer customer);
}
