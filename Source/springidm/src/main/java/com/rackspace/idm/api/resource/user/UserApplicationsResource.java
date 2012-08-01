package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserApplicationsResource extends ParentResource {

    private final UserApplicationResource applicationResource;
    private final ApplicationConverter applicationConverter;
    private final UserService userService;
    private final AuthorizationService authorizationService;

    @Autowired
    public UserApplicationsResource(UserApplicationResource applicationResource,
        ApplicationConverter clientConverter,
        UserService userService,
        AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
    	super(inputValidator);
        this.applicationResource = applicationResource;
        this.applicationConverter = clientConverter;
        this.authorizationService = authorizationService;
        this.userService = userService;
    }

    /**
     * Gets the applications a user has provisioned.
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     */
    @GET
    public Response getApplicationsForUser(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        getLogger().debug("Getting applications for user {}", userId);


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
