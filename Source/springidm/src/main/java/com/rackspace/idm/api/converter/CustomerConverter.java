package com.rackspace.idm.api.converter;

import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.jaxb.ObjectFactory;

public class CustomerConverter {

    protected ObjectFactory of = new ObjectFactory();

    public CustomerConverter() {
    }

    public Customer toCustomerDO(com.rackspace.idm.jaxb.Customer jaxbCustomer) {

        Customer customer = new Customer();

        customer.setIname(jaxbCustomer.getIname());
        customer.setCustomerId(jaxbCustomer.getCustomerId());
        customer.setInum(jaxbCustomer.getInum());
        if (jaxbCustomer.isLocked() != null) {
            customer.setIsLocked(jaxbCustomer.isLocked());
        }

        if (jaxbCustomer.isSoftDeleted() != null) {
            customer.setSoftDeleted(jaxbCustomer.isSoftDeleted());
        }

        return customer;
    }

    public com.rackspace.idm.jaxb.Customer toJaxbCustomer(Customer customer) {

        com.rackspace.idm.jaxb.Customer jaxbCustomer = new com.rackspace.idm.jaxb.Customer();
        jaxbCustomer.setCustomerId(customer.getCustomerId());
        jaxbCustomer.setIname(customer.getIname());
        jaxbCustomer.setInum(customer.getInum());
        jaxbCustomer.setLocked(customer.getIsLocked());
        jaxbCustomer.setSoftDeleted(customer.getSoftDeleted());

        return jaxbCustomer;
    }

}
