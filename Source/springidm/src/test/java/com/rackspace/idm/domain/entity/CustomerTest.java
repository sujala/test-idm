package com.rackspace.idm.domain.entity;

import junit.framework.Assert;

import org.junit.Test;

public class CustomerTest {

    private final String customerId = "123";

    private Customer getTestCustomer() {
        Customer customer = new Customer();
        customer.setRCN(customerId);
        return customer;
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

        customer1.setRCN(null);

        customer2.setRCN(null);

        Assert.assertTrue(customer1.equals(customer2));
    }

    @Test
    public void shouldReturnFalseForEqual() {
        Customer customer1 = getTestCustomer();
        Customer customer2 = getTestCustomer();

        Assert.assertFalse(customer1.equals(null));
        Assert.assertFalse(customer1.equals(1));

        customer2.setRCN("NewId");
        Assert.assertFalse(customer1.equals(customer2));
        customer2.setRCN(null);
        Assert.assertFalse(customer2.equals(customer1));
    }
}
