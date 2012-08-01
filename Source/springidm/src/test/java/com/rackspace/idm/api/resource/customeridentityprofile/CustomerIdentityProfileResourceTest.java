package com.rackspace.idm.api.resource.customeridentityprofile;

import com.rackspace.api.idm.v1.IdentityProfile;
import com.rackspace.idm.api.converter.CustomerConverter;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/22/12
 * Time: 10:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class CustomerIdentityProfileResourceTest {
    private CustomerIdentityProfileResource customerIdentityProfileResource;
    private PasswordRotationPolicyResource passwordRotationPolicyResource;
    private UsersResource usersResource;
    private CustomerService customerService;
    private CustomerConverter customerConverter;
    private AuthorizationService authorizationService;
    private ApplicationsResource applicationsResource;
    private InputValidator inputValidator;

    @Before
    public void setUp() throws Exception {
        passwordRotationPolicyResource = mock(PasswordRotationPolicyResource.class);
        usersResource = mock(UsersResource.class);
        customerService = mock(CustomerService.class);
        customerConverter = mock(CustomerConverter.class);
        authorizationService = mock(AuthorizationService.class);
        applicationsResource = mock(ApplicationsResource.class);
        inputValidator = mock(InputValidator.class);

        customerIdentityProfileResource = new CustomerIdentityProfileResource(passwordRotationPolicyResource, usersResource, customerService,
                customerConverter, authorizationService, applicationsResource, inputValidator);
    }

    @Test
    public void getCustomerIdentityProfile_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        customerIdentityProfileResource.getCustomerIdentityProfile("authHeader", "customerId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void getCustomerIdentityProfile_callsCustomerService_loadCustomer() throws Exception {
        customerIdentityProfileResource.getCustomerIdentityProfile("authHeader", "customerId");
        verify(customerService).loadCustomer("customerId");
    }

    @Test
    public void getCustomerIdentityProfile_callsCustomerConverter_toJaxbCustomer() throws Exception {
        customerIdentityProfileResource.getCustomerIdentityProfile("authHeader", "customerId");
        verify(customerConverter).toJaxbCustomer(any(Customer.class));
    }

    @Test
    public void getCustomerIdentityProfile_responseOk_returns200() throws Exception {
        Response response = customerIdentityProfileResource.getCustomerIdentityProfile("authHeader", "customerId");
        assertThat("reponse code", response.getStatus(), equalTo(200));
    }

    @Test
    public void deleteCustomerIdentityProfile_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        customerIdentityProfileResource.deleteCustomerIdentityProfile("authHeader", "customerId");
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void deleteCustomerIdentityProfile_callsCustomerService_loadCustomer() throws Exception {
        customerIdentityProfileResource.deleteCustomerIdentityProfile("authHeader", "customerId");
        verify(customerService).loadCustomer("customerId");
    }

    @Test
    public void deleteCustomerIdentityProfile_callsCustomerService_deleteCustomer() throws Exception {
        customerIdentityProfileResource.deleteCustomerIdentityProfile("authHeader", "customerId");
        verify(customerService).deleteCustomer("customerId");
    }

    @Test
    public void deleteCustomerIdentityProfile_responseNoContent_returns204() throws Exception {
        Response response = customerIdentityProfileResource.deleteCustomerIdentityProfile("authHeader", "customerId");
        assertThat("reponse code", response.getStatus(), equalTo(204));
    }

    @Test
    public void updateCustomerIdentityProfile_callsAuthService_verifyIdmSuperAdminAccess() throws Exception {
        IdentityProfile identityProfile = new IdentityProfile();
        identityProfile.setEnabled(true);
        EntityHolder<IdentityProfile> holder = new EntityHolder<IdentityProfile>(identityProfile);
        when(customerService.loadCustomer("customerId")).thenReturn(new Customer());
        customerIdentityProfileResource.updateCustomerIdentityProfile("customerId", "authHeader", holder);
        verify(authorizationService).verifyIdmSuperAdminAccess("authHeader");
    }

    @Test
    public void updateCustomerIdentityProfile_callsCustomerService_updateCustomer() throws Exception {
        IdentityProfile identityProfile = new IdentityProfile();
        identityProfile.setEnabled(true);
        EntityHolder<IdentityProfile> holder = new EntityHolder<IdentityProfile>(identityProfile);
        when(customerService.loadCustomer("customerId")).thenReturn(new Customer());
        customerIdentityProfileResource.updateCustomerIdentityProfile("customerId", "authHeader", holder);
        verify(customerService).updateCustomer(any(Customer.class));
    }

    @Test
    public void updateCustomerIdentityProfile_responseNoContent_returns204() throws Exception {
        IdentityProfile identityProfile = new IdentityProfile();
        identityProfile.setEnabled(true);
        EntityHolder<IdentityProfile> holder = new EntityHolder<IdentityProfile>(identityProfile);
        when(customerService.loadCustomer("customerId")).thenReturn(new Customer());
        Response response = customerIdentityProfileResource.updateCustomerIdentityProfile("customerId", "authHeader", holder);
        assertThat("reponse code", response.getStatus(), equalTo(204));
    }

    @Test
    public void getPasswordRotationPolicyResource_returnsPasswordRotationPolicyResource() throws Exception {
        PasswordRotationPolicyResource resource = customerIdentityProfileResource.getPasswordRotationPolicyResource();
        assertThat("password rotation policy resource", resource, equalTo(passwordRotationPolicyResource));
    }

    @Test
    public void getApplicationsResource_returnsApplicationsResource() throws Exception {
        ApplicationsResource resource = customerIdentityProfileResource.getApplicationsResource();
        assertThat("application resource", resource, equalTo(applicationsResource));
    }

    @Test
    public void getUsersResource_returnsUsersResource() throws Exception {
        UsersResource resource = customerIdentityProfileResource.getUsersResource();
        assertThat("users resource", resource, equalTo(usersResource));
    }
}
