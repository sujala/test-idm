package com.rackspace.idm.domain.dao.impl;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.unboundid.ldap.sdk.Modification;

public class LdapCustomerRepositoryTest {

    private LdapCustomerRepository repo;
    private LdapConnectionPools connPools;

    static String customerId = "DELETE_My_CustomerId";
    String customerName = "DELETE_My_Name";
    String inum = "@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String iname = "@Rackspae.TESTING";
    CustomerStatus status = CustomerStatus.ACTIVE;
    String seeAlso = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String owner = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String country = "USA";
    boolean softDeleted = false;

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
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapCustomerRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        return new LdapConfiguration(new PropertyFileConfiguration()
            .getConfigFromClasspath()).connectionPools();
    }

    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);

        addNewTestCustomer(customerId, customerName, inum, iname, status,
            seeAlso, owner, country);
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
        String newCustomerId = "RCN-000-111-iamnew";
        String newCustomerName = "I_am_new";
        String newInum = "@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC.NEW";
        String newIname = "@Rackspae.TESTING.NEW";
        String newOwner = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC.NEW";

        Customer newCustomer = addNewTestCustomer(newCustomerId,
            newCustomerName, newInum, newIname, status, seeAlso, newOwner,
            country);
        Customer checkCustomer = repo.getCustomerByCustomerId(newCustomer
            .getCustomerId());
        Assert.assertNotNull(checkCustomer);
        Assert.assertEquals(newCustomerId, checkCustomer.getCustomerId());
        repo.deleteCustomer(checkCustomer.getCustomerId());
    }

    @Test
    public void shouldFindOneCustomerThatExists() {
        Customer customer = repo.getCustomerByCustomerId(customerId);
        Assert.assertNotNull(customer);
        Assert.assertEquals(customerId, customer.getCustomerId());
    }

    @Test
    public void shouldNotFindCustomerThatDoesNotExist() {
        Customer customer = repo.getCustomerByCustomerId("hi. i don't exist.");
        Assert.assertNull(customer);
    }

    @Test
    public void shouldGetUnusedCustomerInum() {
        String unusedInum = repo.getUnusedCustomerInum();
        Assert.assertFalse(unusedInum.equals(inum));
    }

    @Test
    public void shouldFindOneCustomerThatExistsByInum() {
        Customer customer = repo.getCustomerByInum(inum);
        Assert.assertNotNull(customer);
        Assert.assertEquals(customerId, customer.getCustomerId());
    }

    @Test
    public void shouldNotFindCustomerThatDoesNotExistByInum() {
        Customer customer = repo
            .getCustomerByInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.Idontexist");
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

        String delCustomerId = "RCN-000-111-deleteme";
        String delCustomerName = "deleteme";
        String delInum = "@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC.DELETE";
        String delIname = "@Rackspae.TESTING.DELETE";
        String delOwner = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC.DELETE";

        Customer customerToDelete = addNewTestCustomer(delCustomerId,
            delCustomerName, delInum, delIname, status, seeAlso, delOwner,
            country);
        repo.deleteCustomer(customerToDelete.getCustomerId());
        Customer idontexist = repo.getCustomerByCustomerId(customerToDelete
            .getCustomerId());
        Assert.assertNull(idontexist);
    }

    @Test
    public void shouldUpdateNonDnAttrOfCustomer() {
        Customer testCustomer = repo.getCustomerByCustomerId(customerId);

        // Update all non-DN attributes
        testCustomer.setIname("My_New_Iname");
        testCustomer.setStatus(CustomerStatus.INACTIVE);

        try {
            repo.updateCustomer(testCustomer);
        } catch (IllegalStateException e) {
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        Customer changedCustomer = repo.getCustomerByCustomerId(customerId);
        Assert.assertEquals(testCustomer, changedCustomer);

        // Update only one attribute
        testCustomer.setIname("My_Changed_Name");

        try {
            repo.updateCustomer(testCustomer);
        } catch (IllegalStateException e) {
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        changedCustomer = repo.getCustomerByCustomerId(customerId);
        Assert.assertEquals(testCustomer, changedCustomer);
    }

    @Test
    public void shouldGenerateModifications() {

        Customer client = createTestCustomerInstance(customerId, inum, iname,
            status, seeAlso, owner);
        Customer cClient = createTestCustomerInstance(customerId, inum, iname,
            status, seeAlso, owner);

        cClient.setStatus(CustomerStatus.INACTIVE);

        List<Modification> mods = repo.getModifications(client, cClient);

        Assert.assertEquals(1, mods.size());
        Assert.assertEquals("INACTIVE", mods.get(0).getAttribute().getValue());
    }

    private Customer addNewTestCustomer(String customerId, String name,
        String inum, String iname, CustomerStatus status, String seeAlso,
        String owner, String country) {

        Customer newCustomer = createTestCustomerInstance(customerId, inum,
            iname, status, seeAlso, owner);
        newCustomer.setSoftDeleted(softDeleted);
        repo.addCustomer(newCustomer);
        return newCustomer;
    }

    private Customer createTestCustomerInstance(String customerId, String inum,
        String iname, CustomerStatus status, String seeAlso, String owner) {

        Customer newCustomer = new Customer(customerId, inum, iname, status,
            seeAlso, owner);
        newCustomer.setSoftDeleted(softDeleted);
        return newCustomer;
    }
}
