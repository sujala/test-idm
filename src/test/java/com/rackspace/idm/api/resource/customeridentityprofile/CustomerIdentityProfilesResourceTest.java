package com.rackspace.idm.api.resource.customeridentityprofile;

import com.rackspace.api.idm.v1.IdentityProfile;
import com.rackspace.idm.api.converter.CustomerConverter;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.exception.CustomerConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/18/12
 * Time: 5:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class CustomerIdentityProfilesResourceTest {

    CustomerIdentityProfileResource customerIdentityProfileResource;
    CustomerService customerService;
    CustomerConverter customerConverter;
    AuthorizationService authorizationService;
    InputValidator inputValidator;
    CustomerIdentityProfilesResource customerIdentityProfilesResource;

    @Before
    public void setUp() throws Exception {
        customerIdentityProfileResource = mock(CustomerIdentityProfileResource.class);
        customerService = mock(CustomerService.class);
        customerConverter = mock(CustomerConverter.class);
        authorizationService = mock(AuthorizationService.class);
        inputValidator = mock(InputValidator.class);

        customerIdentityProfilesResource = new CustomerIdentityProfilesResource(customerIdentityProfileResource, customerService, inputValidator, customerConverter, authorizationService);
    }

    @Test
    public void addCustomer_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        EntityHolder<IdentityProfile> customer = mock(EntityHolder.class);
        when(customer.hasEntity()).thenReturn(true);
        Customer customerDo = new Customer();
        customerDo.setRcn("rcn");
        when(customerConverter.toCustomerDO(any(IdentityProfile.class))).thenReturn(customerDo);
        customerIdentityProfilesResource.addCustomer(null, customer);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void addCustomer_callsCustomerService_addCustomer() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        EntityHolder<IdentityProfile> customer = mock(EntityHolder.class);
        when(customer.hasEntity()).thenReturn(true);
        Customer customerDo = new Customer();
        customerDo.setRcn("rcn");
        when(customerConverter.toCustomerDO(any(IdentityProfile.class))).thenReturn(customerDo);
        customerIdentityProfilesResource.addCustomer(null, customer);
        verify(customerService).addCustomer(any(Customer.class));
    }

    @Test(expected = CustomerConflictException.class)
    public void addCustomer_withDuplicateException_throwsCustomerConflictException() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        EntityHolder<IdentityProfile> customer = mock(EntityHolder.class);
        when(customer.hasEntity()).thenReturn(true);
        Customer customerDo = new Customer();
        customerDo.setRcn("rcn");
        when(customerConverter.toCustomerDO(any(IdentityProfile.class))).thenReturn(customerDo);
        doThrow(new DuplicateException()).when(customerService).addCustomer(any(Customer.class));
        customerIdentityProfilesResource.addCustomer(null, customer);
    }

    @Test
    public void getCustomerResource_returnsCustomerResource() throws Exception {
        CustomerIdentityProfileResource customerResource = customerIdentityProfilesResource.getCustomerResource();
        assertThat("customer resource", customerResource, equalTo(customerIdentityProfileResource));
    }
}
