package com.rackspace.idm.rest.resources;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PasswordConverter;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.entities.*;
import com.rackspace.idm.exceptions.*;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.services.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * User Password.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserPasswordResource {
    private AccessTokenService accessTokenService;
    private UserService userService;
    private PasswordComplexityService passwordComplexityService;
    private PasswordConverter passwordConverter;
    private TokenConverter tokenConverter;
    private AuthorizationService authorizationService;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    String errorMsg = String.format("Authorization Failed");

    @Autowired
    public UserPasswordResource(AccessTokenService accessTokenService,
        UserService userService, 
        PasswordComplexityService passwordComplexityService,
        PasswordConverter passwordConverter, TokenConverter tokenConverter,
        AuthorizationService authorizationService) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.passwordComplexityService = passwordComplexityService;
        this.passwordConverter = passwordConverter;
        this.tokenConverter = tokenConverter;
        this.authorizationService = authorizationService;
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

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(token,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        Password password = Password.existingInstance(user.getPassword());

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

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Specific Clients and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId);

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        Password newPassword = Password.generateRandom();
        user.setPasswordObj(newPassword);
        this.userService.updateUser(user);
        logger.debug("Updated password for user: {}", user);

        Password password = Password.existingInstance(user.getPassword());

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
        com.rackspace.idm.jaxb.UserCredentials userCred,
        @QueryParam("recovery") boolean isRecovery) {

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        boolean authorized = false;

        if (isRecovery) {
            authorized = token.isRestrictedToSetPassword()
                && token.getTokenUser().getCustomerId().equals(customerId)
                && token.getTokenUser().getUsername().equals(userCred);
        } else {
            authorized = authorizationService.authorizeRacker(token)
                || authorizationService.authorizeClient(token,
                    request.getMethod(), uriInfo.getPath())
                || authorizationService.authorizeAdmin(token, customerId)
                || authorizationService.authorizeUser(token, customerId,
                    username);
        }

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token.getTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (!passwordComplexityService.checkPassword(
            userCred.getNewPassword().getPassword()).isValidPassword()) {
            String errorMsg = String.format("Invalid password %s", userCred
                .getNewPassword().getPassword());
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        logger.debug("Updating Password for User: {}", username);

        if (!isRecovery) {
            if (userCred.getCurrentPassword() == null
                || StringUtils.isBlank(userCred.getCurrentPassword()
                    .getPassword())) {
                String errMsg = "Value for Current Password cannot be blank";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }

            // authenticate using old password
            UserAuthenticationResult uaResult = this.userService.authenticate(
                username, userCred.getCurrentPassword().getPassword());
            if (!uaResult.isAuthenticated()) {
                String errorMsg = String.format("Current password does not match for user: %s",
                    username);
                logger.warn(errorMsg);
                throw new NotAuthenticatedException(errorMsg);
            }
        }

        User user = checkAndGetUser(customerId, username);

        user.setPasswordObj(Password.newInstance(userCred.getNewPassword()
            .getPassword()));
        this.userService.updateUser(user);
        logger.debug("Updated password for user: {}", user);
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

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Only Rackspace Clients and Specific Clients are authorized
        boolean authorized = authorizationService
            .authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

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

        AccessToken resetToken = accessTokenService
            .createPasswordResetAccessTokenForUser(user, token.getTokenClient().getClientId());

        logger.debug("Got Password Reset Token for User :{}", user);

        return Response.ok(tokenConverter.toAccessTokenJaxb(resetToken))
            .build();
    }

    /**
     * Sends an email to a user to allow the user to reset their password.
     *
     * @param authHeader    HTTP Authorization header for authenticating the caller.
     * @param customerId    RCN
     * @param username      username
     * @param recoveryParam Password recovery email parameters
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}passwordRecovery
     * @response.representation.204.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @POST
    @Path("recoveryemail")
    public Response sendRecoveryEmail(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username, PasswordRecovery recoveryParam) {

        logger.debug("Sending password recovery email for User: {}", username);

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        boolean authorized = authorizationService
            .authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            String errorMsg = "User is not active";
            logger.warn(errorMsg);
            throw new ForbiddenException(errorMsg);
        }

        if (user.isLocked()) {
            String errorMsg = "User is locked";
            logger.warn(errorMsg);
            throw new ForbiddenException(errorMsg);
        }

        if (StringUtils.isBlank(user.getEmail())) {
            String errorMsg = "User doesn't have an email address";
            logger.warn(errorMsg);
            throw new IdmException(errorMsg);
        }

        if (StringUtils.isBlank(recoveryParam.getCallbackUrl())) {
            String errorMsg = "callbackUrl is a required parameter.";
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        AccessToken resetToken = accessTokenService
            .createPasswordResetAccessTokenForUser(user, token.getTokenClient().getClientId());

        try {
            userService.sendRecoveryEmail(username, user.getEmail(),
                recoveryParam, resetToken.getTokenString());
        } catch (IllegalArgumentException e) {
            String errorMsg = e.getMessage();
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        } catch (IllegalStateException ise) {
            String errorMsg = "Could not send password recovery email.";
            logger.warn(errorMsg);
            throw new IdmException(errorMsg);
        }

        logger.debug("Sent password recovery email for User: {}", username);

        return Response.noContent().build();
    }

    private User checkAndGetUser(String customerId, String username) {
        User user = this.userService.getUser(customerId, username);
        if (user == null) {
            handleUserNotFoundError(customerId, username);
        }
        return user;
    }

    private void handleUserNotFoundError(String customerId, String username) {
        String errorMsg = String.format("User not found: %s - %s", customerId,
            username);
        logger.warn(errorMsg);
        throw new NotFoundException(errorMsg);
    }
}
