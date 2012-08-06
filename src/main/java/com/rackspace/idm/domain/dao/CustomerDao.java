package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Customer;

import java.util.List;

public interface CustomerDao {
    void addCustomer(Customer customer);
    
    Customer getCustomerById(String id);

    Customer getCustomerByCustomerId(String customerId);

    List<Customer> getAllCustomers();

    void updateCustomer(Customer customer);

    void deleteCustomer(String customerId);
    
    String getNextCustomerId();

    void softDeleteCustomer(Customer customer);

    void unSoftDeleteCustomer(Customer customer);

    Customer getSoftDeletedUserByCustomerId(String customerId);

    Customer getSoftDeletedCustomerById(String id);
}
