package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 4/18/12
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class RackerResource extends ParentResource {

    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final ScopeAccessService scopeAccessService;

    @Autowired(required = true)
    public RackerResource(UserService userService,
                          AuthorizationService authorizationService,
                          ScopeAccessService scopeAccessService,
                          InputValidator inputValidator) {
        super(inputValidator);
        this.userService = userService;
        this.authorizationService = authorizationService;
        this.scopeAccessService = scopeAccessService;
    }

    @DELETE
    @Path("{racker}")
    public Response deleteRacker(
        @PathParam("racker") String racker,
        @HeaderParam("X-Auth-Token") String authHeader) {
        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);
        userService.deleteRacker(racker);
        return Response.ok().build();
    }
}
