package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.Customer;

public interface CustomerDao {
    void addCustomer(Customer customer);

    Customer getCustomerByCustomerId(String customerId);

    Customer getCustomerByInum(String inum);

    List<Customer> getAllCustomers();

    void updateCustomer(Customer customer);

    void deleteCustomer(String customerId);

    String getUnusedCustomerInum();
}
