package com.rackspace.idm.domain.dao.impl;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/25/12
 * Time: 10:55 AM
 */
public class LdapCustomerRepositoryTest {

    LdapCustomerRepository ldapCustomerRepository;

    @Before
    public void setUp() throws Exception {
        ldapCustomerRepository = new LdapCustomerRepository(null,null);
    }

    @Test
    public void getCustomerByCustomerId_withNullId_returnsNull() throws Exception {
        ldapCustomerRepository.getCustomerByCustomerId(null);
    }

    @Test
    public void getCustomerByCustomerId_withEmptyString_returnsNull() throws Exception {
        ldapCustomerRepository.getCustomerByCustomerId("");
    }
}
