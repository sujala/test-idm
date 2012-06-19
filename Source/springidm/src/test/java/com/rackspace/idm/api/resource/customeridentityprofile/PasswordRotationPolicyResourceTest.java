package com.rackspace.idm.api.resource.customeridentityprofile;

import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/19/12
 * Time: 10:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class PasswordRotationPolicyResourceTest {

    CustomerService customerService;
    AuthorizationService authorizationService;
    PasswordRotationPolicyResource passwordRotationPolicyResource;

    @Before
    public void setUp() throws Exception {
        customerService = mock(CustomerService.class);
        authorizationService = mock(AuthorizationService.class);

        passwordRotationPolicyResource = new PasswordRotationPolicyResource(customerService, authorizationService, null);
    }

    @Test
    public void getPasswordRotationPolicy_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        Customer customer = new Customer();
        customer.setEnabled(false);
        when(customerService.loadCustomer(null)).thenReturn(customer);
        passwordRotationPolicyResource.getPasswordRotationPolicy(null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void getPasswordRotationPolicy_returns200Status() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        Customer customer = new Customer();
        customer.setEnabled(false);
        when(customerService.loadCustomer(null)).thenReturn(customer);
        Response response = passwordRotationPolicyResource.getPasswordRotationPolicy(null, null);
        assertThat("response status", response.getStatus(), equalTo(200));
    }

    @Ignore
    @Test
    public void updatePasswordRotationPolicy_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        EntityHolder holder = mock(EntityHolder.class);
        when(holder.hasEntity()).thenReturn(true);
        passwordRotationPolicyResource.updatePasswordRotationPolicy(null, null, holder);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test(expected = BadRequestException.class)
    public void updatePasswordRotationPolicy_withNoEntityInHolder_throwsBadRequestException() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        EntityHolder holder = mock(EntityHolder.class);
        when(holder.hasEntity()).thenReturn(false);
        passwordRotationPolicyResource.updatePasswordRotationPolicy(null, null, holder);
    }
}
