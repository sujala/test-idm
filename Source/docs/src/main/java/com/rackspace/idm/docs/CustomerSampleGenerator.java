package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.Customer;
import com.rackspace.idm.jaxb.Customers;

public class CustomerSampleGenerator extends SampleGenerator {
    private CustomerSampleGenerator() {
        super();
    }
    
    public static void main(String[] args) throws JAXBException, IOException {
        CustomerSampleGenerator sampleGen = new CustomerSampleGenerator();

        sampleGen.marshalToFiles(sampleGen.getCustomer(), "customer");
        sampleGen.marshalToFiles(sampleGen.getCustomers(), "customers");
    }
    
    private Customer getCustomer() {
        Customer customer = of.createCustomer();
        
        customer.setCustomerId("RCN-123-549-034");
        customer.setIname("@Rackspace*Customer");
        customer.setInum("@FFFF.FFFF.FFFF.FFFF!0ABE.34EC");
        customer.setLocked(Boolean.FALSE);
        customer.setSoftDeleted(Boolean.FALSE);
        
        return customer;
    }
    
    private Customer getCustomer2() {
        Customer customer = of.createCustomer();
        
        customer.setCustomerId("RCN-123-549-034");
        customer.setIname("@Rackspace*AnotherCustomer");
        customer.setInum("@FFFF.FFFF.FFFF.FFFF!428B.0A49");
        customer.setLocked(Boolean.FALSE);
        customer.setSoftDeleted(Boolean.FALSE);
        
        return customer;
    }
    
    private Customers getCustomers() {
        Customers customers = of.createCustomers();
        Customer customer = getCustomer();
        customers.getCustomers().add(customer);
        Customer customer2 = getCustomer2();
        customers.getCustomers().add(customer2);
        return customers;
    }
}