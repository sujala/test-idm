package com.rackspace.idm.rest.resources;

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PasswordConverter;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.IdmException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.UserDisabledException;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.util.AuthHeaderHelper;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserPasswordResource {

    private OAuthService oauthService;
    private AccessTokenService accessTokenService;
    private UserService userService;
    private PasswordComplexityService passwordComplexityService;
    private PasswordConverter passwordConverter;
    private TokenConverter tokenConverter;
    private AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();
    private AuthorizationService authorizationService;
    private Logger logger;
    String errorMsg = String.format("Authorization Failed");

    @Autowired
    public UserPasswordResource(OAuthService oauthService,
        AccessTokenService accessTokenService, UserService userService,
        PasswordComplexityService passwordComplexityService,
        PasswordConverter passwordConverter, TokenConverter tokenConverter,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.oauthService = oauthService;
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.passwordComplexityService = passwordComplexityService;
        this.passwordConverter = passwordConverter;
        this.tokenConverter = tokenConverter;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
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

        // Racker's, Specific Clients and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
            || authorizationService.authorizeClient(authHeader,
                request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(authHeader, customerId);

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        Password password = Password.existingInstance(user
            .getPasswordNoPrefix());

        return Response.ok(passwordConverter.toJaxb(password)).build();
    }

    /**
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
        logger.info("Reseting Password for User: {}", username);

        // Racker's, Specific Clients and Admins are authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
            || authorizationService.authorizeClient(authHeader,
                request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(authHeader, customerId);

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        Password newpassword = Password.generateRandom();
        user.setPasswordObj(newpassword);
        this.userService.updateUser(user);
        logger.info("Updated password for user: {}", user);

        return Response.ok(passwordConverter.toJaxb(newpassword)).build();
    }

    /**
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

        // Racker's, Specific Clients, Admins and User's are authorized
        boolean authorized = authorizationService.authorizeRacker(authHeader)
            || authorizationService.authorizeClient(authHeader,
                request.getMethod(), uriInfo.getPath())
            || authorizationService.authorizeAdmin(authHeader, customerId)
            || authorizationService.authorizeUser(authHeader, customerId,
                username);

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        if (!passwordComplexityService.checkPassword(
            userCred.getNewPassword().getPassword()).isValidPassword()) {
            String errorMsg = String.format("Invalid password %s", userCred
                .getNewPassword().getPassword());
            logger.warn(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        logger.info("Updating Password for User: {}", username);

        if (isRecovery) {

            // Check for restricted token
            logger.debug("Evaluating auth header {}", authHeader);

            String tokenString = authHeaderHelper.parseTokenParams(authHeader)
                .get("token");
            if (StringUtils.isBlank(tokenString)) {
                String errorMsg = String.format(
                    "No token string found for Authorization %s", authHeader);
                logger.error(errorMsg);
                throw new NotAuthorizedException(errorMsg);
            }

            AccessToken token = accessTokenService
                .getTokenByTokenString(tokenString);
            if (token == null) {
                String errorMsg = String.format("No token found for %s",
                    tokenString);
                logger.error(errorMsg);
                throw new NotAuthorizedException(errorMsg);
            }

            String tokenUser = oauthService
                .getUsernameFromAuthHeaderToken(authHeader);

            // TODO jeo this logic is redundant, as the authorization logic
            // below should handle it
            if (token.isRestrictedToSetPassword()) {
                if (!username.equalsIgnoreCase(tokenUser)) {
                    // Restricted token trying to reset another user's password
                    String errorMsg = String
                        .format(
                            "Restricted token %s can only reset its user's password",
                            tokenString);
                    logger.error(errorMsg);
                    throw new NotAuthorizedException(errorMsg);
                }
            } else {
                if (username.equalsIgnoreCase(tokenUser)) {
                    // Normal user token trying to reset own user's password
                    String errorMsg = String
                        .format(
                            "Unrestricted user token %s cannot reset its user's password",
                            tokenString);
                    logger.error(errorMsg);
                    throw new NotAuthorizedException(errorMsg);
                }
            }

            // NOTE: A normal token trying to reset another user's password is
            // okay, if the token owner is the password user's admin.

        } else {
            if (userCred.getCurrentPassword() == null
                || StringUtils.isBlank(userCred.getCurrentPassword()
                    .getPassword())) {
                String errMsg = "Value for oldPassword cannot be blank";
                logger.error(errMsg);
                throw new BadRequestException(errMsg);
            }

            // authenticate using old password
            if (!this.oauthService.authenticateUser(username, userCred
                .getCurrentPassword().getPassword())) {
                String errorMsg = String.format("Bad credential for user: %s",
                    username);
                logger.debug(errorMsg);
                throw new NotAuthenticatedException(errorMsg);
            }
        }

        User user = checkAndGetUser(customerId, username);

        user.setPasswordObj(Password.newInstance(userCred.getNewPassword()
            .getPassword()));
        this.userService.updateUser(user);
        logger.info("Updated password for user: {}", user);
        return Response.ok(userCred).build();
    }

    /**
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

        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(authHeader,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            String errorMsg = "User is not active";
            logger.error(errorMsg);
            throw new ForbiddenException(errorMsg);
        }

        if (user.getIsLocked()) {
            String errorMsg = "User is locked";
            logger.error(errorMsg);

            throw new UserDisabledException(errorMsg);
        }

        String clientId = oauthService
            .getClientIdFromAuthHeaderToken(authHeader);

        AccessToken token = accessTokenService
            .createPasswordResetAccessTokenForUser(username, clientId);

        logger.debug("Got Password Reset Token for User :{}", user);

        return Response.ok(tokenConverter.toAccessTokenJaxb(token)).build();

    }

    /**
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

        logger.info("Sending password recovery email for User: {}", username);
        
        // Only Specific Clients are authorized
        boolean authorized = authorizationService.authorizeClient(authHeader,
            request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String token = authHeader.split(" ")[1];
            String errMsg = String.format("Token %s Forbidden from this call",
                token);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            String errorMsg = "User is not active";
            logger.error(errorMsg);
            throw new ForbiddenException(errorMsg);
        }

        if (user.getIsLocked()) {
            String errorMsg = "User is locked";
            logger.error(errorMsg);
            throw new ForbiddenException(errorMsg);
        }

        if (StringUtils.isBlank(user.getEmail())) {
            String errorMsg = "User doesn't have an email address";
            logger.error(errorMsg);
            throw new IdmException(errorMsg);
        }

        if (StringUtils.isBlank(recoveryParam.getCallbackUrl())) {
            String errorMsg = "callbackUrl is a required parameter.";
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        String clientId = oauthService
            .getClientIdFromAuthHeaderToken(authHeader);

        AccessToken token = accessTokenService
            .createPasswordResetAccessTokenForUser(username, clientId);

        try {
            userService.sendRecoveryEmail(username, user.getEmail(),
                recoveryParam, token.getTokenString());
        } catch (IllegalStateException ise) {
            String errorMsg = "Could not send password recovery email.";
            logger.error(errorMsg);
            throw new IdmException(errorMsg);
        }

        logger.info("Sent password recovery email for User: {}", username);

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
        logger.error(errorMsg);
        throw new NotFoundException(errorMsg);
    }
}
