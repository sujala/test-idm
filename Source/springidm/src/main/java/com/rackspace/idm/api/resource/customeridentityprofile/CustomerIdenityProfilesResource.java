package com.rackspace.idm.api.resource.customeridentityprofile;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.CustomerConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.CustomerConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

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
     * @param inputCustomer New Customer
     */
    @POST
    public Response addCustomer(@Context UriInfo uriInfo, @Context Request request,
        @HeaderParam("X-Auth-Token") String authHeader, 
        EntityHolder<com.rackspace.api.idm.v1.IdentityProfile> holder) {
    	
    	validateRequestBody(holder);
        
        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        com.rackspace.api.idm.v1.IdentityProfile inputCustomer = holder.getEntity();
        Customer customer = customerConverter.toCustomerDO(inputCustomer);
        customer.setDefaults();
        validateDomainObject(customer);

        getLogger().debug("Adding Customer Identity Profile: {}", customer.getRCN());

        try {
            this.customerService.addCustomer(customer);
        } catch (DuplicateException ex) {
            String errorMsg = String.format("A customer with that customerId already exists: %s",
                customer.getRCN());
            getLogger().warn(errorMsg);
            throw new CustomerConflictException(errorMsg);
        }

        getLogger().debug("Added Customer Identity Profile: {}", customer);

        String location = customer.getRCN();

		return Response.created(URI.create(location)).build();
    }

    @Path("{customerId}")
    public CustomerIdentityProfileResource getCustomerResource() {
        // No customerId lookup from here to prevent probing attacks.
        return customerIdentityProfileResource;
    }
}
