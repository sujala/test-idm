package com.rackspace.idm.api.resource.customer;

import org.slf4j.Logger;

import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.exception.NotFoundException;

public abstract class AbstractCustomerConsumer {
    private CustomerService customerService;

    protected AbstractCustomerConsumer(CustomerService customerService) {
        this.customerService = customerService;
    }

    protected Customer checkAndGetCustomer(String customerId) throws NotFoundException {
        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s", customerId);
            getLogger().warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        return customer;
    }

    protected abstract Logger getLogger();
}
