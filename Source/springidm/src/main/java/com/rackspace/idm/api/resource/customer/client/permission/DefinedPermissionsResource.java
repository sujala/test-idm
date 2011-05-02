package com.rackspace.idm.api.resource.customer.client.permission;

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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.api.converter.PermissionConverter;
import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.api.resource.customer.client.AbstractClientConsumer;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.PermissionConflictException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Client defined permissions.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DefinedPermissionsResource extends AbstractClientConsumer {

    private final DefinedPermissionResource definedPermissionResource;
    private final ClientService clientService;
    private final PermissionConverter permissionConverter;
    private final InputValidator inputValidator;
    private final AuthorizationService authorizationService;
    private final ScopeAccessService scopeAccessService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public DefinedPermissionsResource(DefinedPermissionResource definedPermissionResource,
        ClientService clientService, PermissionConverter permissionConverter, InputValidator inputValidator,
        AuthorizationService authorizationService, ScopeAccessService scopeAccessService) {
        super(clientService);
        this.definedPermissionResource = definedPermissionResource;
        this.permissionConverter = permissionConverter;
        this.inputValidator = inputValidator;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.scopeAccessService = scopeAccessService;
    }

    /**
     * Gets a list of Client defined permissions.
     * 
     * @response.representation.200.qname http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId Client application ID
     */
    @GET
    public Response getClientDefinedPermissions(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId) {

        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients and Admins are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(), uriInfo)
            || authorizationService.authorizeAdmin(token, customerId);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        checkAndGetClient(customerId, clientId);

        List<PermissionObject> defineds = this.clientService.getDefinedPermissionsByClientId(clientId);

        return Response.ok(permissionConverter.toPermissionListJaxb(defineds)).build();
    }

    /**
     * Adds a client defined permission.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.200.qname http://docs.rackspacecloud.com/idm/api/v1.0}permission
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param clientId Client application ID
     * @param permission New permission
     */
    @POST
    public Response addClientDefinedPermission(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader, @PathParam("customerId") String customerId,
        @PathParam("clientId") String clientId, EntityHolder<com.rackspace.idm.jaxb.Permission> holder) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccessObject token = this.scopeAccessService
        .getAccessTokenByAuthHeader(authHeader);

        // Racker's or the specified client are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeCustomerIdm(token)
            || (authorizationService.authorizeClient(token, request.getMethod(), uriInfo) && (token instanceof ClientScopeAccessObject && token
                .getClientId().equalsIgnoreCase(
                    clientId)));

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        com.rackspace.idm.jaxb.Permission permission = holder.getEntity();
        validatePermissionRequest(customerId, clientId, permission);

        PermissionObject permissionDO = permissionConverter.toPermissionDO(permission);

        ApiError err = inputValidator.validate(permissionDO);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        try {
            this.clientService.addDefinedPermission(permissionDO);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.warn(errorMsg);
            throw new PermissionConflictException(errorMsg);
        } catch (IllegalStateException e) {
            String errMsg = e.getMessage();
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        String location = uriInfo.getPath() + permission.getPermissionId();

        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            logger.warn("Permission Location URI error");
        }

        return Response.ok(permission).location(uri).status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{permissionId}")
    public DefinedPermissionResource getDefinedPermissionResource() {
        return definedPermissionResource;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    private void validatePermissionRequest(String customerId, String clientId,
        com.rackspace.idm.jaxb.Permission permission) throws BadRequestException {
        
        if (!customerId.equalsIgnoreCase(permission.getCustomerId())) {
            String errMsg = "CustomerId mismatch";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        if (!clientId.equalsIgnoreCase(permission.getClientId())) {
            String errMsg = "ClientId mismatch";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
        
        if (StringUtils.isBlank(permission.getPermissionId())) {
            String errMsg = "PermissionId cannot be blank";
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }
    }
}
