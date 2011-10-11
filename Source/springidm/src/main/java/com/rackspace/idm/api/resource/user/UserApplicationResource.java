package com.rackspace.idm.api.resource.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserApplicationResource extends ParentResource {

    //private final UserTenantsRolesResource userApplicationRolesResource;
    private final ScopeAccessService scopeAccessService;
    private final ApplicationService applicationService;
    private final UserService userService;
    private final AuthorizationService authorizationService;
    
    @Autowired
    public UserApplicationResource(
        ScopeAccessService scopeAccessService, ApplicationService applicationService,
        UserService userService, AuthorizationService authorizationService,
        InputValidator inputValidator) {
    	
    	super(inputValidator);
        this.applicationService = applicationService;
        this.scopeAccessService = scopeAccessService;
        this.authorizationService = authorizationService;
        this.userService = userService;
        //this.userApplicationRolesResource = userPermissionsResource;
    }

    /**
     * Provision an application for a user.
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     * @param application New Application.
     */
    @PUT
    public Response provisionApplicationForUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("userId") String userId,
        @PathParam("applicationId") String applicationId) {

        getLogger().info("Provisioning application {} for user {}", applicationId, userId);

        ScopeAccess token = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        // Rackers can add any applications to a user
        // Rackspace Clients can add their own applications to a user
        // Specific Clients can add their own applications to a user
        // Customer IdM can add any service to user
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        Application application = this.applicationService.loadApplication(applicationId);
        User user = this.userService.loadUser(userId);

        //TODO: probably should be in the application service. Refactor when get chance
        UserScopeAccess sa = new UserScopeAccess();
        sa.setUsername(user.getUsername());
        sa.setUserRCN(user.getCustomerId());
        sa.setClientId(application.getClientId());
        sa.setClientRCN(application.getRCN());

        this.scopeAccessService.addDirectScopeAccess(user.getUniqueId(), sa);

        getLogger().info("Provisioned application {} to user {}", applicationId, userId);

        return Response.noContent().build();
    }
    
    /**
     * Remove an application from a user
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     * @param applicationId  Application Id
     */
    @DELETE
    public Response removeApplicationFromUser(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("userId") String userId,
        @PathParam("applicationId") String applicationId) {

        getLogger().info("Removing application {} from user {}", applicationId, userId);

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);
        // Rackers can add any service to a user
        // Rackspace Clients can add their own service to a user
        // Specific Clients can add their own service to a user
        // Customer IdM can add any service to user
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        Application application = this.applicationService.loadApplication(applicationId);
        User user = this.userService.loadUser(userId);

        //TODO: refactor. Probably should be in the applciation service.
        this.scopeAccessService.deleteScopeAccessesForParentByApplicationId(user.getUniqueId(), application.getClientId());
        
        getLogger().info("Removed application {} from user {}", applicationId, userId);

        return Response.noContent().build();
    }
}
