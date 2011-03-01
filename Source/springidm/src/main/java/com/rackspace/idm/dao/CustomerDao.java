package com.rackspace.idm.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.Customer;

public interface CustomerDao {
    void add(Customer customer);

    Customer findByCustomerId(String customerId);

    Customer findByInum(String inum);

    List<Customer> findAll();

    void save(Customer customer);

    void delete(String customerId);

    String getCustomerDnByCustomerId(String customerId);

    String getUnusedCustomerInum();
}
