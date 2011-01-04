package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DefinedPermissionResource {

    private ClientService clientService;
    private PermissionConverter permissionConverter;
    private InputValidator inputValidator;
    private Logger logger;

    @Autowired
    public DefinedPermissionResource(ClientService clientService,
        PermissionConverter permissionConverter, InputValidator inputValidator,
        LoggerFactoryWrapper logger) {
        this.permissionConverter = permissionConverter;
        this.inputValidator = inputValidator;
        this.clientService = clientService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @PUT
    public Response updateClientPermission(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId,
        com.rackspace.idm.jaxb.Permission permission) {

        Permission permissionDO = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);

        if (!customerId.equals(permissionDO.getCustomerId())) {
            String errorMsg = String.format("Permission Not Found: %s",
                permissionId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        permissionDO.setType(permission.getType());
        permissionDO.setValue(permission.getValue());

        ApiError err = inputValidator.validate(permissionDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        this.clientService.updateDefinedPermission(permissionDO);

        return Response.ok(permission).build();
    }

    /**
     * @response.representation.204.doc
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @DELETE
    public Response deleteClientPermission(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId) {

        Permission permission = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);

        if (!customerId.equals(permission.getCustomerId())) {
            String errorMsg = String.format("Permission Not Found: %s",
                permissionId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        this.clientService.deleteDefinedPermission(permission);
        
        return Response.noContent().build();
    }

    /**
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getClientPermission(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId) {

        Permission permission = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);

        if (!customerId.equals(permission.getCustomerId())) {
            String errorMsg = String.format("Permission Not Found: %s",
                permissionId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        return Response.ok(permissionConverter.toPermissionJaxb(permission)).build();
    }
}
