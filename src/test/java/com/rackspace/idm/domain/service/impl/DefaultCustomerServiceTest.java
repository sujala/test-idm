package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.UserService;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.domain.dao.CustomerDao;
import org.junit.Test;

import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/12/12
 * Time: 3:48 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultCustomerServiceTest {
    @InjectMocks
    DefaultCustomerService defaultCustomerService = new DefaultCustomerService();
    @Mock
    ApplicationService applicationService;
    @Mock
    CustomerDao customerDao;
    @Mock
    UserService userService;

    @Test
    public void softDeleteCustomer_callsCustomerDao_softDeleteCustomer() throws Exception {
        defaultCustomerService.softDeleteCustomer(null);
        verify(customerDao).softDeleteCustomer(null);
    }
}
