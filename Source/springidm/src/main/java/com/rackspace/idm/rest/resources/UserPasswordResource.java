package com.rackspace.idm.rest.resources;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PasswordConverter;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.entities.*;
import com.rackspace.idm.exceptions.*;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.services.*;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.RegexPatterns;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User Password.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserPasswordResource {
    private AccessTokenService accessTokenService;
    private UserService userService;
    private ClientService clientService;
    private PasswordComplexityService passwordComplexityService;
    private PasswordConverter passwordConverter;
    private TokenConverter tokenConverter;
    private AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();
    private AuthorizationService authorizationService;
    private Logger logger;
    String errorMsg = String.format("Authorization Failed");

    @Autowired
    public UserPasswordResource(AccessTokenService accessTokenService,
        UserService userService, ClientService clientService,
        PasswordComplexityService passwordComplexityService,
        PasswordConverter passwordConverter, TokenConverter tokenConverter,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.clientService = clientService;
        this.passwordComplexityService = passwordComplexityService;
        this.passwordConverter = passwordConverter;
        this.tokenConverter = tokenConverter;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets the user's password.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userPassword
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
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
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        Password password = Password.existingInstance(user
            .getPasswordNoPrefix());

        return Response.ok(passwordConverter.toJaxb(password)).build();
    }

    /**
     * Resets a user's password.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userPassword
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
     */
    @POST
    public Response resetUserPassword(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.info("Reseting Password for User: {}", username);

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
     * Sets a user's password.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userCredentials
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
     * @param userCred The user's current password and new password.
     * @param isRecovery If true, this request is a password reset attempt.
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

        // Racker's, Specific Clients, Admins and User's are authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath())
            || authorizationService.authorizeAdmin(token, customerId)
            || authorizationService.authorizeUser(token, customerId, username);

        if (!authorized) {
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

            AccessToken resettoken = accessTokenService
                .getAccessTokenByTokenString(tokenString);
            if (resettoken == null) {
                String errorMsg = String.format("No token found for %s",
                    tokenString);
                logger.error(errorMsg);
                throw new NotAuthorizedException(errorMsg);
            }

            String tokenUser = getUsernameByTokenString(tokenString);

            // TODO jeo this logic is redundant, as the authorization logic
            // below should handle it
            if (resettoken.isRestrictedToSetPassword()) {
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
            if (!this.userService.authenticate(username, userCred
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
     * Gets a token restricted to resetting a user's password.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}token
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}severError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
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
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        User user = checkAndGetUser(customerId, username);

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            String errorMsg = "User is not active";
            logger.error(errorMsg);
            throw new ForbiddenException(errorMsg);
        }

        if (user.isLocked()) {
            String errorMsg = "User is locked";
            logger.error(errorMsg);

            throw new UserDisabledException(errorMsg);
        }

        String tokenStr = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        String clientId = getClientIdByTokenString(tokenStr);
        AccessToken resetToken = accessTokenService
            .createPasswordResetAccessTokenForUser(username, clientId);

        logger.debug("Got Password Reset Token for User :{}", user);

        return Response.ok(tokenConverter.toAccessTokenJaxb(resetToken))
            .build();
    }

    /**
     * Sends an email to a user to allow the user to reset their password.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}passwordRecovery
     * @response.representation.204.doc Successful request
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId RCN
     * @param username username
     * @param recoveryParam Password recovery email parameters
     */
    @POST
    @Path("recoveryemail")
    public Response sendRecoveryEmail(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username, PasswordRecovery recoveryParam) {

        logger.info("Sending password recovery email for User: {}", username);

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        boolean authorized = authorizationService
            .authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath());

        if (!authorized) {
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

        if (user.isLocked()) {
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

        // validate from address
        String fromEmail = recoveryParam.getFrom();
        Pattern p = Pattern.compile(RegexPatterns.EMAIL_ADDRESS);
        Matcher m = p.matcher(fromEmail);
        boolean matchFound = m.matches();
        if (!matchFound) {
            String errorMsg = "Invalid from address";
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        // validate reply-to address
        String replyToEmail = recoveryParam.getReplyTo();
        if (replyToEmail == null) {
            replyToEmail = fromEmail;
            recoveryParam.setReplyTo(replyToEmail);
        }

        Matcher replyToMatcher = p.matcher(replyToEmail);
        matchFound = replyToMatcher.matches();
        if (!matchFound) {
            String errorMsg = "Invalid reply-to address";
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        String tokenStr = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        String clientId = getClientIdByTokenString(tokenStr);

        AccessToken resetToken = accessTokenService
            .createPasswordResetAccessTokenForUser(username, clientId);

        try {
            userService.sendRecoveryEmail(username, user.getEmail(),
                recoveryParam, resetToken.getTokenString());
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

    private String getUsernameByTokenString(String tokenString) {
        logger.debug("Getting Username From Token: {}", tokenString);
        AccessToken token = accessTokenService
            .getAccessTokenByTokenString(tokenString);
        if (token == null) {
            return null;
        }

        // Get the Owner and filter out the inum= prefix
        String ownerUsername = token.getOwner();
        if (token.getIsTrusted()) {
            return ownerUsername;
        }

        User user = userService.getUser(ownerUsername);

        if (user != null) {
            String username = user.getUsername();
            logger.debug("Got Username From Token: {} : {}", tokenString,
                username);
            return username;
        }
        logger.debug("No User Associated With Token: {}", tokenString);
        return null;
    }

    private String getClientIdByTokenString(String tokenString) {
        logger.debug("Getting ClientId From Token: {}", tokenString);
        AccessToken token = accessTokenService
            .getAccessTokenByTokenString(tokenString);
        if (token == null) {
            return null;
        }

        // Get the Owner and filter out the inum= prefix
        String requestor = token.getRequestor();
        Client client = clientService.getById(requestor);

        if (client != null) {
            String clientId = client.getClientId();
            logger.debug("Got clientId From Token: {} : {}", tokenString,
                clientId);
            return clientId;
        }
        logger.debug("No Client Associated With Token: {}", tokenString);
        return null;
    }
}
