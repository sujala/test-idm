package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.Customer;

public class CustomerConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    public CustomerConverter() {
    }

    public Customer toCustomerDO(com.rackspace.api.idm.v1.CustomerIdentityProfile jaxbCustomer) {

        Customer customer = new Customer();

        customer.setId(jaxbCustomer.getId());
        customer.setRCN(jaxbCustomer.getRcn());
        if (jaxbCustomer.isEnabled() != null) {
            customer.setEnabled(jaxbCustomer.isEnabled());
        }

        return customer;
    }

    public com.rackspace.api.idm.v1.CustomerIdentityProfile toJaxbCustomer(Customer customer) {

        com.rackspace.api.idm.v1.CustomerIdentityProfile jaxbCustomer = objectFactory.createCustomerIdentityProfile();
        jaxbCustomer.setId(customer.getId());
        jaxbCustomer.setRcn(customer.getRCN());
        jaxbCustomer.setEnabled(customer.isEnabled());

        return jaxbCustomer;
    }
}
