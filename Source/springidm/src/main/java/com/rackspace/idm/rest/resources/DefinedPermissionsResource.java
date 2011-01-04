package com.rackspace.idm.rest.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.PermissionSet;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.PermissionConflictException;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DefinedPermissionsResource {

    private DefinedPermissionResource definedPermissionResource;
    private ClientService clientService;
    private PermissionConverter permissionConverter;
    private InputValidator inputValidator;
    private Logger logger;

    @Autowired
    public DefinedPermissionsResource(
        DefinedPermissionResource definedPermissionResource,
        ClientService clientService, PermissionConverter permissionConverter,
        InputValidator inputValidator, LoggerFactoryWrapper logger) {
        this.definedPermissionResource = definedPermissionResource;
        this.permissionConverter = permissionConverter;
        this.inputValidator = inputValidator;
        this.clientService = clientService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @response.representation.200.qname http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getClientDefinedPermissions(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        List<Permission> defineds = this.clientService
            .getDefinedPermissionsByClientId(clientId);

        PermissionSet permset = new PermissionSet();

        permset.setDefineds(defineds);

        return Response.ok(permissionConverter.toPermissionsJaxb(permset))
            .build();
    }

    /**
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.200.qname http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @POST
    public Response addClientPermission(@Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        com.rackspace.idm.jaxb.Permission permission) {

        permission.setClientId(clientId);
        permission.setCustomerId(customerId);

        Permission permissionDO = permissionConverter
            .toPermissionDO(permission);

        ApiError err = inputValidator.validate(permissionDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        try {
            this.clientService.addDefinedPermission(permissionDO);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.error(errorMsg);
            throw new PermissionConflictException(errorMsg);
        }

        String location = uriInfo.getPath() + permission.getPermissionId();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.error("Permission Location URI error");
        }

        return Response.ok(permission).location(uri)
            .status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{permissionId}")
    public DefinedPermissionResource getDefinedPermissionResource() {
        return definedPermissionResource;
    }
}
