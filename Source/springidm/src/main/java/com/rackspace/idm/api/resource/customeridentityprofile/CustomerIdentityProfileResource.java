package com.rackspace.idm.api.resource.customeridentityprofile;

import com.rackspace.idm.api.converter.CustomerConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A Rackspace Customer.
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerIdentityProfileResource extends ParentResource {

    private final PasswordRotationPolicyResource passwordRotationPolicyResource;
    private final UsersResource usersResource;
    private final CustomerService customerService;
    private final ScopeAccessService scopeAccessService;
    private final CustomerConverter customerConverter;
    private final AuthorizationService authorizationService;
    private final ApplicationsResource applicationsResource;

    @Autowired
    public CustomerIdentityProfileResource(PasswordRotationPolicyResource passwordRotationPolicyResource,
        UsersResource usersResource,
        CustomerService customerService, ScopeAccessService scopeAccessService, CustomerConverter customerConverter,
        AuthorizationService authorizationService,
        ApplicationsResource applicationsResource, InputValidator inputValidator) {
        
    	super(inputValidator);
        this.passwordRotationPolicyResource = passwordRotationPolicyResource;
        this.usersResource = usersResource;
        this.customerService = customerService;
        this.scopeAccessService = scopeAccessService;
        this.customerConverter = customerConverter;
        this.authorizationService = authorizationService;
        this.applicationsResource = applicationsResource;
    }

    /**
     * Gets a customer's identity profile
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @GET
    public Response getCustomerIdentityProfile(
        @HeaderParam("X-Auth-Token") String authHeader, 
        @PathParam("customerId") String customerId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        getLogger().debug("Getting Customer Identity Profile: {}", customerId);

        Customer customer = this.customerService.loadCustomer(customerId);

        getLogger().debug("Got Customer Identity Profile:{}", customer);
        return Response.ok(customerConverter.toJaxbCustomer(customer)).build();
    }

    /**
     * Deletes a customer's identity profile.
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @DELETE
    public Response deleteCustomerIdentityProfile(
        @HeaderParam("X-Auth-Token") String authHeader, 
        @PathParam("customerId") String customerId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        getLogger().info("Deleting Customer Identity Profile :{}", customerId);

        // do this to ensure customer exists already
        this.customerService.loadCustomer(customerId);

        this.customerService.deleteCustomer(customerId);
        getLogger().debug("Deleted Customer Identity Profile: {}", customerId);

        return Response.noContent().build();
    }

    /**
     * Updates a customer's identity profile.
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @PUT
    public Response updateCusotmerIdentityProfile(
        @PathParam("customerId") String customerId, 
        @HeaderParam("X-Auth-Token") String authHeader,
        EntityHolder<com.rackspace.api.idm.v1.IdentityProfile> holder) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
    	
    	validateRequestBody(holder);

        com.rackspace.api.idm.v1.IdentityProfile inputCustomer = holder.getEntity();
        getLogger().debug("Getting Customer Identity Profile: {}", customerId);
        
        //TODO: all this should be in a copy command, refactor
        Customer customer = this.customerService.loadCustomer(customerId);
        customer.setEnabled(inputCustomer.isEnabled());

        getLogger().debug("Successfully Updated Customer Identity Profile: {}", customer);

        return Response.noContent().build();
    }
    
    @Path("passwordrotationpolicy")
    public PasswordRotationPolicyResource getPasswordRotationPolicyResource() {
    	return passwordRotationPolicyResource;
    }

    @Path("applications")
    public ApplicationsResource getApplicationsResource() {
        return applicationsResource;
    }

    @Path("users")
    public UsersResource getUsersResource() {
        return usersResource;
    }
}
