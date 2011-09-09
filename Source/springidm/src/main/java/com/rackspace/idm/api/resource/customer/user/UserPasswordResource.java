package com.rackspace.idm.api.resource.customer.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.api.idm.v1.UserCredentials;
import com.rackspace.idm.api.converter.PasswordConverter;
import com.rackspace.idm.api.converter.TokenConverter;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.exception.UserDisabledException;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * User Password.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserPasswordResource {
    private final ScopeAccessService scopeAccessService;
    private final UserService userService;
    private final PasswordComplexityService passwordComplexityService;
    private final PasswordConverter passwordConverter;
    private final TokenConverter tokenConverter;
    private final AuthorizationService authorizationService;
    private final Configuration config;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    String errorMsg = String.format("Authorization Failed");

    @Autowired
    public UserPasswordResource(ScopeAccessService scopeAccessService,
        UserService userService,
        PasswordComplexityService passwordComplexityService,
        PasswordConverter passwordConverter, TokenConverter tokenConverter,
        AuthorizationService authorizationService, Configuration config) {
        this.scopeAccessService = scopeAccessService;
        this.userService = userService;
        this.passwordComplexityService = passwordComplexityService;
        this.passwordConverter = passwordConverter;
        this.tokenConverter = tokenConverter;
        this.authorizationService = authorizationService;
        this.config = config;
    }

    /**
     * Gets the user's password.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username   username
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userPassword
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getUserPassword(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        User user = this.userService.checkAndGetUser(customerId, username);

        Password password = user.getPasswordObj();

        return Response.ok(passwordConverter.toJaxb(password)).build();
    }

    /**
     * Resets a user's password.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username   username
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userPassword
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @POST
    public Response resetUserPassword(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.debug("Reseting Password for User: {}", username);

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients and Admins are authorized
        boolean isSelfUpdate = (token instanceof UserScopeAccess && username
            .equals(((UserScopeAccess) token).getUsername()));
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo)
            || (authorizationService.authorizeAdmin(token, customerId) && !isSelfUpdate);

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        User user = this.userService.checkAndGetUser(customerId, username);
        Password password = userService.resetUserPassword(user);
        if (password == null) {
            logger
                .warn("Could not get the updated password for user: {}", user);
            throw new IdmException("Could not reset password.");
        }

        logger.debug("Updated password for user: {}", user);
        return Response.ok(passwordConverter.toJaxb(password)).build();
    }

    /**
     * Sets a user's password.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username   username
     * @param userCred   The user's current password and new password.
     * @param isRecovery If true, this request is a password reset attempt.
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userCredentials
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @PUT
    public Response setUserPassword(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        EntityHolder<com.rackspace.api.idm.v1.UserCredentials> holder,
        @QueryParam("recovery") boolean isRecovery) {
        if (!holder.hasEntity()) {
            throw new BadRequestException("Request body missing.");
        }
        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        boolean authorized = false;

        if (isRecovery) {
            authorized = token instanceof PasswordResetScopeAccess
                && ((PasswordResetScopeAccess) token).getUserRCN()
                    .equalsIgnoreCase(customerId)
                && ((PasswordResetScopeAccess) token).getUsername()
                    .equals(username);
        } else {
            authorized = authorizationService.authorizeRacker(token)
                || authorizationService.authorizeClient(token,
                    request.getMethod(), uriInfo)
                || authorizationService.authorizeAdmin(token, customerId)
                || authorizationService.authorizeUser(token, customerId,
                    username);
        }

        authorizationService.checkAuthAndHandleFailure(authorized, token);

        UserCredentials userCred = holder.getEntity();

        if (isPasswordRulesEnforced() && !passwordComplexityService.checkPassword(
            userCred.getNewPassword().getPassword()).isValidPassword()) {
            String errorMsg = String.format("Invalid password %s", userCred
                .getNewPassword().getPassword());
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        this.userService.setUserPassword(customerId, username, userCred, token,
            isRecovery);

        return Response.ok(userCred).build();
    }

    /**
     * Gets a token restricted to resetting a user's password.
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username   username
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}token
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}severError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @POST
    @Path("recoverytoken")
    public Response getPasswordResetToken(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Rackspace Clients and Specific Clients are authorized
        boolean authorized = authorizationService
            .authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo);

        authorizationService.checkAuthAndHandleFailure(authorized, token);
        User user = this.userService.checkAndGetUser(customerId, username);

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            String errorMsg = "User is not active";
            logger.warn(errorMsg);
            throw new ForbiddenException(errorMsg);
        }

        if (user.isLocked()) {
            String errorMsg = "User is locked";
            logger.warn(errorMsg);

            throw new UserDisabledException(errorMsg);
        }

        PasswordResetScopeAccess prsa = this.scopeAccessService
            .getOrCreatePasswordResetScopeAccessForUser(user);

        logger.debug("Got Password Reset Token for User :{}", user);

        return Response.ok(
            tokenConverter.toTokenJaxb(prsa.getAccessTokenString(),
                prsa.getAccessTokenExp())).build();
    }
    
    private boolean isPasswordRulesEnforced() {
        return config.getBoolean("password.rules.enforced", true);
    }
}
