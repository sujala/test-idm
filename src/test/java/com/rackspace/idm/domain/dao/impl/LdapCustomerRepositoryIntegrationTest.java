package com.rackspace.idm.domain.dao.impl;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rackspace.idm.domain.entity.Customer;
import com.unboundid.ldap.sdk.Modification;
import org.junit.*;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class LdapCustomerRepositoryIntegrationTest extends InMemoryLdapIntegrationTest{

    @Autowired
    private LdapCustomerRepository repo;
    @Autowired
    private LdapConnectionPools connPools;

    static String customerId = "DELETE_My_CustomerId";
    String id = "XXXX";

    @Before
    public void preTestSetUp() throws Exception {
        Customer deleteme = repo.getCustomerByCustomerId(customerId);
        if (deleteme != null) {
            repo.deleteCustomer(customerId);
        }
        addNewTestCustomer(customerId);
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
        Assert.assertEquals(customerId, checkCustomer.getRcn());
        repo.deleteCustomer(checkCustomer.getRcn());
    }

    @Test
    public void shouldFindOneCustomerThatExists() {
        Customer customer = repo.getCustomerByCustomerId(customerId);
        Assert.assertNotNull(customer);
        Assert.assertEquals(customerId, customer.getRcn());
    }

    @Test
    public void shouldNotFindCustomerThatDoesNotExist() {
        Customer customer = repo.getCustomerByCustomerId("hi. i don't exist.");
        Assert.assertNull(customer);
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

    private Customer addNewTestCustomer(String customerId) {

        Customer newCustomer = createTestCustomerInstance(customerId);
        repo.addCustomer(newCustomer);
        return newCustomer;
    }

    private Customer createTestCustomerInstance(String customerId) {

        Customer newCustomer = new Customer();
        newCustomer.setRcn(customerId);
        newCustomer.setId(id);
        return newCustomer;
    }
}
