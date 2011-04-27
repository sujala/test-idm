package com.rackspace.idm.api.resource.customer.role;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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

import com.rackspace.idm.api.converter.RoleConverter;
import com.rackspace.idm.api.resource.customer.AbstractCustomerConsumer;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.Role;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.ForbiddenException;

/**
 * Customer roles
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class RolesResource extends AbstractCustomerConsumer {

    private ScopeAccessService scopeAccessService;
    private RoleService roleService;
    private RoleConverter roleConverter;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public RolesResource(ScopeAccessService scopeAccessService, CustomerService customerService,
        RoleService roleService, RoleConverter roleConverter, AuthorizationService authorizationService) {
        super(customerService);
        
        this.scopeAccessService = scopeAccessService;
        this.roleService = roleService;
        this.roleConverter = roleConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets a list of roles for a customer
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}roles
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     */
    @GET
    public Response getRoles(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId) {

        logger.debug("Getting Customer Roles: {}", customerId);

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients and Admins are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo)
            || authorizationService.authorizeAdmin(token, customerId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        checkAndGetCustomer(customerId);

        List<Role> roles = roleService.getByCustomerId(customerId);

        com.rackspace.idm.jaxb.Roles outputRoles = roleConverter.toRolesJaxb(roles);

        logger.debug("Got Customer Roles:{}", roles);
        return Response.ok(outputRoles).build();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
