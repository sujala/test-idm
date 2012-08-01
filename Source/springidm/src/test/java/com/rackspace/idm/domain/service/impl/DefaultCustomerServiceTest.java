package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.service.TokenService;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/12/12
 * Time: 3:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultCustomerServiceTest {
    DefaultCustomerService defaultCustomerService;
    ApplicationDao clientDao;
    CustomerDao customerDao;
    UserDao userDao;

    @Before
    public void setUp() throws Exception {
        clientDao = mock(ApplicationDao.class);
        customerDao = mock(CustomerDao.class);
        userDao = mock(UserDao.class);

        defaultCustomerService = new DefaultCustomerService(clientDao, customerDao, userDao);
    }

    @Test
    public void softDeleteCustomer_callsCustomerDao_softDeleteCustomer() throws Exception {
        defaultCustomerService.softDeleteCustomer(null);
        verify(customerDao).softDeleteCustomer(null);
    }
}
