package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.PasswordConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.AuthorizationContext;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.ScopeAccess;
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

/**
 * User Password.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserPasswordCredentialsResource extends ParentResource {
    private final ScopeAccessService scopeAccessService;
    private final UserService userService;
    private final PasswordConverter passwordConverter;
    private final AuthorizationService authorizationService;


    @Autowired
    public UserPasswordCredentialsResource(
            ScopeAccessService scopeAccessService,
            UserService userService,
            PasswordConverter passwordConverter,
            AuthorizationService authorizationService,
            InputValidator inputValidator) {

        super(inputValidator);
        this.scopeAccessService = scopeAccessService;
        this.userService = userService;
        this.passwordConverter = passwordConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets the user's password.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId     userId
     */
    @GET
    public Response getUserPassword(
            @HeaderParam("X-Auth-Token") String authHeader,
            @PathParam("userId") String userId) {


        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(authHeader);
        AuthorizationContext context = authorizationService.getAuthorizationContext(scopeAccess);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(context);

        User user = this.userService.loadUser(userId);

        Password password = user.getPasswordObj();

        return Response.ok(passwordConverter.toJaxb(password)).build();
    }
}
