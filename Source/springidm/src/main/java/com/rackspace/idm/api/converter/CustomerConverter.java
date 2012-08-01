package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.IdentityProfile;
import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.Customer;

import javax.xml.bind.JAXBElement;

public class CustomerConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    public CustomerConverter() {
    }

    public Customer toCustomerDO(com.rackspace.api.idm.v1.IdentityProfile jaxbCustomer) {

        Customer customer = new Customer();

        customer.setId(jaxbCustomer.getId());
        customer.setRCN(jaxbCustomer.getCustomerId());
        if (jaxbCustomer.isEnabled() != null) {
            customer.setEnabled(jaxbCustomer.isEnabled());
        }

        return customer;
    }

    public JAXBElement<com.rackspace.api.idm.v1.IdentityProfile> toJaxbCustomer(Customer customer) {

        IdentityProfile jaxbCustomer = objectFactory.createIdentityProfile();
        jaxbCustomer.setId(customer.getId());
        jaxbCustomer.setCustomerId(customer.getRCN());
        jaxbCustomer.setEnabled(customer.isEnabled());

        return objectFactory.createCustomerIdentityProfile(jaxbCustomer);
    }
}
