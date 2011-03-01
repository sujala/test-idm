package com.rackspace.idm.entities;

import junit.framework.Assert;

import org.junit.Test;

import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;

public class CustomerTest {

    private String customerId = "123";
    private String inum = "TestInum";
    private String iname = "TestIname";
    private CustomerStatus status = CustomerStatus.ACTIVE;
    private String seeAlso = "TestSeeAlso";
    private String owner = "TestOwner";

    private Customer getTestCustomer() {
        return new Customer(customerId, inum, iname, status, seeAlso, owner);
    }

    @Test
    public void shouldReturnToString() {
        Customer customer = getTestCustomer();
        Assert.assertNotNull(customer.toString());
    }

    @Test
    public void shouldReturnHashCode() {
        Customer customer = getTestCustomer();
        Assert.assertNotNull( customer.hashCode());
    }

    @Test
    public void shouldReturnTrueForEquals() {
        Customer customer1 = getTestCustomer();
        Customer customer2 = getTestCustomer();
        Assert.assertTrue(customer1.equals(customer2));
        Assert.assertTrue(customer1.equals(customer1));

        customer1.setCustomerId(null);
        customer1.setIname(null);
        customer1.setInum(null);
        customer1.setOwner(null);
        customer1.setSeeAlso(null);
        customer1.setStatus(null);

        customer2.setCustomerId(null);
        customer2.setIname(null);
        customer2.setInum(null);
        customer2.setOwner(null);
        customer2.setSeeAlso(null);
        customer2.setStatus(null);

        Assert.assertTrue(customer1.equals(customer2));
    }

    @Test
    public void shouldReturnFalseForEqual() {
        Customer customer1 = getTestCustomer();
        Customer customer2 = getTestCustomer();

        Assert.assertFalse(customer1.equals(null));
        Assert.assertFalse(customer1.equals(1));

        customer2.setOwner("NewOwner");
        Assert.assertFalse(customer1.equals(customer2));
        customer2.setOwner(null);
        Assert.assertFalse(customer2.equals(customer1));
        customer2.setOwner(customer1.getOwner());

        customer2.setSeeAlso("NewSeeAlso");
        Assert.assertFalse(customer1.equals(customer2));
        customer2.setSeeAlso(null);
        Assert.assertFalse(customer2.equals(customer1));
        customer2.setSeeAlso(customer1.getSeeAlso());

        customer2.setStatus(CustomerStatus.INACTIVE);
        Assert.assertFalse(customer1.equals(customer2));
        customer2.setStatus(null);
        Assert.assertFalse(customer2.equals(customer1));
        customer2.setStatus(customer1.getStatus());

        customer2.setIname("NewIname");
        Assert.assertFalse(customer1.equals(customer2));
        customer2.setIname(null);
        Assert.assertFalse(customer2.equals(customer1));
        customer2.setIname(customer1.getIname());

        customer2.setInum("NewInum");
        Assert.assertFalse(customer1.equals(customer2));
        customer2.setInum(null);
        Assert.assertFalse(customer2.equals(customer1));
        customer2.setInum(customer1.getInum());

        customer2.setCustomerId("NewId");
        Assert.assertFalse(customer1.equals(customer2));
        customer2.setCustomerId(null);
        Assert.assertFalse(customer2.equals(customer1));
    }
}
