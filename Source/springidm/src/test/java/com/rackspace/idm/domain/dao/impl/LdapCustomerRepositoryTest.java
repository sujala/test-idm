package com.rackspace.idm.domain.dao.impl;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.unboundid.ldap.sdk.Modification;

public class LdapCustomerRepositoryTest {

    private LdapCustomerRepository repo;
    private LdapConnectionPools connPools;

    static String customerId = "DELETE_My_CustomerId";
    String customerName = "DELETE_My_Name";
    CustomerStatus status = CustomerStatus.ACTIVE;
    String country = "USA";
    boolean softDeleted = false;
    String id = "XXXX";

    @BeforeClass
    public static void cleanUpData() {
        final LdapConnectionPools pools = getConnPools();
        LdapCustomerRepository cleanUpRepo = getRepo(pools);
        Customer deleteme = cleanUpRepo.getCustomerByCustomerId(customerId);
        if (deleteme != null) {
            cleanUpRepo.deleteCustomer(customerId);
        }
        pools.close();
    }

    private static LdapCustomerRepository getRepo(LdapConnectionPools connPools) {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapCustomerRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapConfiguration(appConfig).connectionPools();
    }

    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);

        addNewTestCustomer(customerId, customerName, status, country);
    }

    @After
    public void tearDown() {
        connPools.close();
        cleanUpData();
    }

    @Test
    public void shouldNotAcceptNullOrBlankCustomerId() {
        try {
            repo.getCustomerByCustomerId(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getCustomerByCustomerId("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.deleteCustomer("");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.updateCustomer(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldAddNewCustomer() {
        Customer checkCustomer = repo.getCustomerByCustomerId(customerId);
        Assert.assertNotNull(checkCustomer);
        Assert.assertEquals(customerId, checkCustomer.getRCN());
        repo.deleteCustomer(checkCustomer.getRCN());
    }

    @Test
    public void shouldFindOneCustomerThatExists() {
        Customer customer = repo.getCustomerByCustomerId(customerId);
        Assert.assertNotNull(customer);
        Assert.assertEquals(customerId, customer.getRCN());
    }

    @Test
    public void shouldNotFindCustomerThatDoesNotExist() {
        Customer customer = repo.getCustomerByCustomerId("hi. i don't exist.");
        Assert.assertNull(customer);
    }

    @Test
    @Ignore
    public void shouldRetrieveAllCustomersThatExist() {
        List<Customer> customers = repo.getAllCustomers();
        Assert.assertTrue(customers.size() >= 1);
    }

    @Test
    public void shouldDeleteCustomer() {
        repo.deleteCustomer(customerId);
        Customer idontexist = repo.getCustomerByCustomerId(customerId);
        Assert.assertNull(idontexist);
    }

    @Test
    public void shouldGenerateModifications() {

        Customer client = createTestCustomerInstance(customerId,
            status );
        Customer cClient = createTestCustomerInstance(customerId,
            status);

        List<Modification> mods = repo.getModifications(client, cClient);

        Assert.assertEquals(1, mods.size());
        Assert.assertEquals("INACTIVE", mods.get(0).getAttribute().getValue());
    }

    private Customer addNewTestCustomer(String customerId, String name,
        CustomerStatus status, String country) {

        Customer newCustomer = createTestCustomerInstance(customerId, status);
        repo.addCustomer(newCustomer);
        return newCustomer;
    }

    private Customer createTestCustomerInstance(String customerId, CustomerStatus status) {

        Customer newCustomer = new Customer();
        newCustomer.setRCN(customerId);
        newCustomer.setId(id);
        return newCustomer;
    }
}
