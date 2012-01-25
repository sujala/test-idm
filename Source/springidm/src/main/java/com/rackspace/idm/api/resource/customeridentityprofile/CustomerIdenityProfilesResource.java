package com.rackspace.idm.api.resource.customeridentityprofile;

import com.rackspace.idm.api.converter.CustomerConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.CustomerConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Rackspace Customers.
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class CustomerIdenityProfilesResource extends ParentResource {
    
    private final CustomerIdentityProfileResource customerIdentityProfileResource;
    private final CustomerService customerService;
    private final ScopeAccessService scopeAccessService;
    private final CustomerConverter customerConverter;
    private final AuthorizationService authorizationService;

    @Autowired
    public CustomerIdenityProfilesResource(CustomerIdentityProfileResource customerResource, CustomerService customerService, 
        ScopeAccessService scopeAccessService, InputValidator inputValidator, CustomerConverter customerConverter,
        AuthorizationService authorizationService) {
       
    	super(inputValidator);
        this.customerIdentityProfileResource = customerResource;
        this.customerService = customerService;
        this.scopeAccessService = scopeAccessService;
        this.customerConverter = customerConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Adds an identity profile for a customer
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customer New Customer
     */
    @POST
    public Response addCustomer(
        @HeaderParam("X-Auth-Token") String authHeader, 
        EntityHolder<com.rackspace.api.idm.v1.IdentityProfile> customer) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        validateRequestBody(customer);

        com.rackspace.api.idm.v1.IdentityProfile inputCustomer = customer.getEntity();
        Customer customerDO = customerConverter.toCustomerDO(inputCustomer);
        customerDO.setDefaults();
        validateDomainObject(customerDO);

        getLogger().debug("Adding Customer Identity Profile: {}", customerDO.getRCN());

        try {
            this.customerService.addCustomer(customerDO);
        } catch (DuplicateException ex) {
            String errorMsg = String.format("A customer with that customerId already exists: %s",
                customerDO.getRCN());
            getLogger().warn(errorMsg);
            throw new CustomerConflictException(errorMsg);
        }

        getLogger().debug("Added Customer Identity Profile: {}", customerDO);

        String location = customerDO.getRCN();

		return Response.created(URI.create(location)).build();
    }

    @Path("{customerId}")
    public CustomerIdentityProfileResource getCustomerResource() {
        // No customerId lookup from here to prevent probing attacks.
        return customerIdentityProfileResource;
    }
}
