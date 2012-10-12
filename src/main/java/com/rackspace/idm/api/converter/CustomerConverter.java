package com.rackspace.idm.api.converter;

import org.springframework.stereotype.Component;

import com.rackspace.api.idm.v1.IdentityProfile;
import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.Customer;

import javax.xml.bind.JAXBElement;

@Component
public class CustomerConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    public Customer toCustomerDO(com.rackspace.api.idm.v1.IdentityProfile jaxbCustomer) {

        Customer customer = new Customer();

        customer.setId(jaxbCustomer.getId());
        customer.setRcn(jaxbCustomer.getCustomerId());
        if (jaxbCustomer.isEnabled() != null) {
            customer.setEnabled(jaxbCustomer.isEnabled());
        }

        return customer;
    }

    public JAXBElement<com.rackspace.api.idm.v1.IdentityProfile> toJaxbCustomer(Customer customer) {

        IdentityProfile jaxbCustomer = objectFactory.createIdentityProfile();
        jaxbCustomer.setId(customer.getId());
        jaxbCustomer.setCustomerId(customer.getRcn());
        jaxbCustomer.setEnabled(customer.isEnabled());

        return objectFactory.createCustomerIdentityProfile(jaxbCustomer);
    }
}
