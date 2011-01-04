package com.rackspace.idm.rest.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.services.RoleService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class RolesResource {

    private CustomerService customerService;
    private RoleService roleService;
    private RoleConverter roleConverter;
    private Logger logger;

    @Autowired
    public RolesResource(CustomerService customerService,
        RoleService roleService, RoleConverter roleConverter,
        LoggerFactoryWrapper logger) {
        this.customerService = customerService;
        this.roleService = roleService;
        this.roleConverter = roleConverter;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}roles
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getRoles(
        @PathParam("customerId") String customerId) {

        logger.debug("Getting Customer Roles: {}", customerId);

        Customer customer = this.customerService.getCustomer(customerId);
        if (customer == null) {
            String errorMsg = String.format("Customer not found: %s",
                customerId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        List<Role> roles = roleService.getByCustomerId(customerId);

        com.rackspace.idm.jaxb.Roles outputRoles = roleConverter
            .toRolesJaxb(roles);

        logger.debug("Got Customer Roles:{}", roles);
        return Response.ok(outputRoles).build();
    }
}
