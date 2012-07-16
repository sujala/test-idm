package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.unboundid.ldap.sdk.Modification;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.*;

import java.util.List;

public class LdapCustomerRepositoryIntegrationTest extends InMemoryLdapIntegrationTest{

    private static LdapCustomerRepository repo;
    private static LdapConnectionPools connPools;

    static String customerId = "DELETE_My_CustomerId";
    String customerName = "DELETE_My_Name";
    CustomerStatus status = CustomerStatus.ACTIVE;
    String country = "USA";
    boolean softDeleted = false;
    String id = "XXXX";

    private static LdapCustomerRepository getRepo(LdapConnectionPools connPools) {
        return new LdapCustomerRepository(connPools, new PropertyFileConfiguration().getConfig());
    }

    private static LdapConnectionPools getConnPools() {
        return new LdapConfiguration(new PropertyFileConfiguration().getConfig()).connectionPools();
    }

    @BeforeClass
    public static void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
    }

    @Before
    public void preTestSetUp() throws Exception {
        Customer deleteme = repo.getCustomerByCustomerId(customerId);
        if (deleteme != null) {
            repo.deleteCustomer(customerId);
        }
        addNewTestCustomer(customerId, customerName, status, country);
    }

    @AfterClass
    public static void tearDown() {
        connPools.close();
    }

    @Test
    public void shouldNotAcceptNullOrBlankCustomerId() {
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

        Customer client = createTestCustomerInstance(customerId);
        client.setEnabled(true);
        Customer cClient = createTestCustomerInstance(customerId);
        cClient.setEnabled(false);

        List<Modification> mods = repo.getModifications(client, cClient);

        Assert.assertEquals(1, mods.size());
    }

    private Customer addNewTestCustomer(String customerId, String name,
        CustomerStatus status, String country) {

        Customer newCustomer = createTestCustomerInstance(customerId);
        repo.addCustomer(newCustomer);
        return newCustomer;
    }

    private Customer createTestCustomerInstance(String customerId) {

        Customer newCustomer = new Customer();
        newCustomer.setRCN(customerId);
        newCustomer.setId(id);
        return newCustomer;
    }
}
