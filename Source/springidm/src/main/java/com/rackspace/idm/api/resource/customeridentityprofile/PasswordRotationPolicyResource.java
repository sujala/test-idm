package com.rackspace.idm.api.resource.customeridentityprofile;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class PasswordRotationPolicyResource extends ParentResource {
    private final CustomerService customerService;
    private final AuthorizationService authorizationService;
    private final ScopeAccessService scopeAccessService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public PasswordRotationPolicyResource(CustomerService customerService, ScopeAccessService scopeAccessService, 
        AuthorizationService authorizationService, InputValidator inputValidator) {
    	
    	super(inputValidator);
        this.customerService = customerService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
    }
    
    /**
     * Gets customer's password rotation policy
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @GET
    public Response getPasswordRotationPolicy(
    		@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader, 
        @PathParam("customerId") String customerId) {
        
        logger.debug("Getting Customer's Password Rotation Policy: {}", customerId);
        
        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);
        // Racker's and Rackspace Clients are authorized
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
	
        Customer customer = this.customerService.loadCustomer(customerId);
        
        //TODO: should probably put in some sort of converter
        com.rackspace.api.idm.v1.PasswordRotationPolicy jaxbPasswordRotationPolicy =
        	new com.rackspace.api.idm.v1.PasswordRotationPolicy();
        jaxbPasswordRotationPolicy.setDuration(customer.getPasswordRotationDuration() == null ? 0 : customer.getPasswordRotationDuration());
        jaxbPasswordRotationPolicy.setEnabled(!customer.isLocked());
        
        logger.debug("Updated password rotation policy for customer {}", customerId);
        return Response.ok(jaxbPasswordRotationPolicy).build();
    }
    
    /**
     * Updates customer's password rotation policy
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @PUT
    public Response updatePasswordRotationPolicy(
    		@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader, 
        @PathParam("customerId") String customerId,
        EntityHolder<com.rackspace.api.idm.v1.PasswordRotationPolicy> holder) {
        
    	validateRequestBody(holder);
        logger.debug("Updating Customer's Password Rotation Policy: {}", customerId);
        
        ScopeAccess token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);
        // Racker's and Rackspace Clients are authorized
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        com.rackspace.api.idm.v1.PasswordRotationPolicy passwordRotationPolicy 
	     = (com.rackspace.api.idm.v1.PasswordRotationPolicy) holder.getEntity();
	
        //TODO: should probably put in some sort of converter
        Customer customer = this.customerService.loadCustomer(customerId);
        customer.setPasswordRotationEnabled(passwordRotationPolicy.isEnabled());
        customer.setPasswordRotationDuration(passwordRotationPolicy.getDuration());

        this.customerService.updateCustomer(customer);

        logger.debug("Updated password rotation policy for customer {}", customerId);
        return Response.noContent().build();
    }
    
    @Override
    protected void validateRequestBody(EntityHolder<?> holder) {
    	super.validateRequestBody(holder);
    	
    	com.rackspace.api.idm.v1.PasswordRotationPolicy passwordRotationPolicy 
    	     = (com.rackspace.api.idm.v1.PasswordRotationPolicy) holder.getEntity();
    	
		int duration = passwordRotationPolicy.getDuration();
		
		if (duration < 0) {
			String errorMsg = String.format("Password rotation duration cannot be negative.");
			logger.warn(errorMsg);
			throw new BadRequestException(errorMsg);
		}
    } 
}
