package com.rackspace.idm.api.resource.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;


@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserApplicationsResource extends ParentResource {

    private final UserApplicationResource applicationResource;
    private final ScopeAccessService scopeAccessService;
    private final ApplicationConverter applicationConverter;
    private final UserService userService;
    private final AuthorizationService authorizationService;

    @Autowired
    public UserApplicationsResource(UserApplicationResource applicationResource,
        ScopeAccessService scopeAccessService,
        ApplicationConverter clientConverter,
        UserService userService,
        AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
    	super(inputValidator);
        this.applicationResource = applicationResource;
        this.scopeAccessService = scopeAccessService;
        this.applicationConverter = clientConverter;
        this.authorizationService = authorizationService;
        this.userService = userService;
    }

    /**
     * Gets the applications a user has provisioned.
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     * @param client New Client.
     */
    @GET
    public Response getApplicationsForUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId) {

        getLogger().debug("Getting applications for user {}", userId);

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);
        // Rackers can add any service to a user
        // Rackspace Clients can add their own service to a user
        // Specific Clients can add their own service to a user
        // Customer IdM can add any service to user
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        User user = this.userService.loadUser(userId);
        Applications applications = this.userService.getUserApplications(user);

        getLogger().debug("Got applications for user {} - {}", userId, applications);

        return Response.ok(applicationConverter.toApplicationJaxbMin(applications)).build();
    }
    
    @Path("{applicationId}")
    public UserApplicationResource getApplicationResource() {
        return applicationResource;
    }
}
