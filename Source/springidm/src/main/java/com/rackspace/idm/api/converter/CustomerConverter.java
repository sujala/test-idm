package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.Customer;

public class CustomerConverter {

    private final ObjectFactory of = new ObjectFactory();

    public CustomerConverter() {
    }

    public Customer toCustomerDO(com.rackspace.api.idm.v1.Customer jaxbCustomer) {

        Customer customer = new Customer();

        customer.setIname(jaxbCustomer.getIname());
        customer.setCustomerId(jaxbCustomer.getCustomerId());
        customer.setInum(jaxbCustomer.getInum());
        if (jaxbCustomer.isLocked() != null) {
            customer.setLocked(jaxbCustomer.isLocked());
        }

        if (jaxbCustomer.isSoftDeleted() != null) {
            customer.setSoftDeleted(jaxbCustomer.isSoftDeleted());
        }

        return customer;
    }

    public com.rackspace.api.idm.v1.Customer toJaxbCustomer(Customer customer) {

        com.rackspace.api.idm.v1.Customer jaxbCustomer = of.createCustomer();
        jaxbCustomer.setCustomerId(customer.getCustomerId());
        jaxbCustomer.setIname(customer.getIname());
        jaxbCustomer.setInum(customer.getInum());
        jaxbCustomer.setLocked(customer.isLocked());
        jaxbCustomer.setSoftDeleted(customer.getSoftDeleted());

        return jaxbCustomer;
    }
    
  
}
