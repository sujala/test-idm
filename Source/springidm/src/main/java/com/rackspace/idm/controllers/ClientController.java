package com.rackspace.idm.controllers;

import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.PermissionSet;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ClientConflictException;
import com.rackspace.idm.exceptions.CustomerConflictException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.PermissionConflictException;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.validation.InputValidator;

/**
 * Clients resource
 */
@Path("/customers/{customerId}/clients")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@NoCache
@Component
public class ClientController {
    private ClientService clientService;
    private InputValidator inputValidator;
    private IDMAuthorizationHelper idmAuthHelper;
    private ClientConverter clientConverter;
    private PermissionConverter permissionConverter;
    private Logger logger;

    @Autowired
    public ClientController(ClientService clientService,
        IDMAuthorizationHelper idmAuthHelper, InputValidator inputValidator,
        ClientConverter clientConverter, PermissionConverter permissionConverter, LoggerFactoryWrapper logger) {
        this.clientService = clientService;
        this.inputValidator = inputValidator;
        this.idmAuthHelper = idmAuthHelper;
        this.clientConverter = clientConverter;
        this.permissionConverter = permissionConverter;
        this.logger = logger.getLogger(ClientController.class);
    }

    /**
     * Add a client.
     * 
     * @RequestHeader Authorization Authorization header, For Example - Token
     *                token="XXXX"
     * 
     * @param client
     *            Client representation
     * @return Newly created Client representation
     * 
     * @HTTP 201 If client is added
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @ResponseHeader Location URI of the newly added user.
     */
    @POST
    @Path("")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Client addClient(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        com.rackspace.idm.jaxb.Client client) {

        if (!client.getCustomerId().toLowerCase().equals(
            customerId.toLowerCase())) {
            String errorMsg = String.format(
                "Client's customerId (%s) does not match customerId (%s)",
                client.getCustomerId(), customerId);
            logger.error(errorMsg);
            throw new CustomerConflictException(errorMsg);
        }

        Client clientDO = clientConverter.toClientDO(client);
        clientDO.setDefaults();

        ApiError err = inputValidator.validate(clientDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        String methodName = "addClient";

        if (!authorizeAddClient(authHeader, customerId, methodName)) {
            if (!idmAuthHelper.checkRackspaceEmployeeAuthorization(authHeader)) {
                idmAuthHelper.handleAuthorizationFailure();
            }
        }

        try {
            this.clientService.add(clientDO);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.error(errorMsg);
            throw new ClientConflictException(errorMsg);
        }
        String locationUri = String.format("/clients/%s", clientDO
            .getClientId());
        response.setHeader("Location", locationUri);

        logger.info("Added Client: {}", clientDO);
        response.setStatus(HttpServletResponse.SC_CREATED);

        client = clientConverter
            .toClientJaxbWithPermissionsAndCredentials(clientDO);

        return client;
    }

    /**
     * Client resource.
     * 
     * Single client and its attributes.
     * 
     * @RequestHeader Authorization Authorization header. For Example - Token
     *                token="XXXX"
     * @param clientId
     *            The clientId of the client to retrieve
     * @return A client resource, if it exists
     * 
     * @HTTP 200 If an existing client is found
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If client is not found
     */
    @GET
    @Path("{clientId}")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Client getClient(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {
        logger.debug("Getting Client: {}", clientId);

        Client client = this.clientService.getById(clientId);

        if (client == null
            || !client.getCustomerId().toLowerCase().equals(
                customerId.toLowerCase())) {
            String errorMsg = String.format("Client Not Found: %s", clientId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        logger.debug("Got Client: {}", client);

        com.rackspace.idm.jaxb.Client returnedClient = clientConverter
            .toClientJaxbWithPermissions(client);

        return returnedClient;
    }

    @GET
    @Path("{clientId}/permissions")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Permissions getClientPermissions(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        List<Permission> defineds = this.clientService.getDefinedPermissionsByClientId(clientId);
        
        PermissionSet permset = new PermissionSet();
        
        permset.setDefineds(defineds);
        
        return permissionConverter.toPermissionsJaxb(permset);
    }

    @GET
    @Path("{clientId}/permissions/defined")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Permissions getClientDefinedPermissions(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        List<Permission> defineds = this.clientService.getDefinedPermissionsByClientId(clientId);
        
        PermissionSet permset = new PermissionSet();
        
        permset.setDefineds(defineds);
        
        return permissionConverter.toPermissionsJaxb(permset);
    }

    @GET
    @Path("{clientId}/permissions/granted")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Permissions getClientGrantedPermissions(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        return null;
    }

    @POST
    @Path("{clientId}/permissions/defined")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Permission addClientPermission(
        @Context HttpServletResponse response,
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

        String locationUri = String.format(
            "/customers/%s/clients/%s/permissions/defined/%s", permission
                .getCustomerId(), permission.getClientId(), permission
                .getPermissionId());
        response.setHeader("Location", locationUri);

        response.setStatus(HttpServletResponse.SC_CREATED);

        return permission;
    }

    @PUT
    @Path("{clientId}/permissions/defined/{permissionId}")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Permission updateClientPermission(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId,
        com.rackspace.idm.jaxb.Permission permission) {
        
        Permission permissionDO = this.clientService.getDefinedPermissionByClientIdAndPermissionId(clientId, permissionId);
        
        if (!customerId.equals(permissionDO.getCustomerId())) {
            String errorMsg = String.format("Permission Not Found: %s", permissionId);
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

        return permission;
    }

    @DELETE
    @Path("{clientId}/permissions/defined/{permissionId}")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public void deleteClientPermission(@Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId) {

        Permission permission = this.clientService.getDefinedPermissionByClientIdAndPermissionId(clientId, permissionId);
        
        if (!customerId.equals(permission.getCustomerId())) {
            String errorMsg = String.format("Permission Not Found: %s", permissionId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        this.clientService.deleteDefinedPermission(permission);
    }

    @GET
    @Path("{clientId}/permissions/defined/{permissionId}")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Permission getClientPermission(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId,
        @PathParam("permissionId") String permissionId) {

        Permission permission = this.clientService
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);
        
        if (!customerId.equals(permission.getCustomerId())) {
            String errorMsg = String.format("Permission Not Found: %s", permissionId);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        return permissionConverter.toPermissionJaxb(permission);
    }

    // private functions
    private boolean authorizeAddClient(String authHeader, String userCompanyId,
        String methodName) throws ForbiddenException {
        return idmAuthHelper.checkAdminAuthorization(authHeader, userCompanyId,
            methodName);
    }
}
