package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.jaxb.UserSecret;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.UserService;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserSecretResource {

    private UserService userService;
    private IDMAuthorizationHelper authorizationHelper;
    private OAuthService oauthService;
    private Logger logger;

    @Autowired
    public UserSecretResource(UserService userService,
        IDMAuthorizationHelper authorizationHelper, OAuthService oauthService,
        LoggerFactoryWrapper logger) {
        this.userService = userService;
        this.authorizationHelper = authorizationHelper;
        this.oauthService = oauthService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userSecret
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getUserSecret(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.info("Getting Secret Q&A for User: {}", username);

        // get user to update
        User user = checkAndGetUser(customerId, username);

        if (!checkUserAuthorization(authHeader, user.getUsername(),
            "setUserSecret")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        com.rackspace.idm.jaxb.UserSecret secret = new com.rackspace.idm.jaxb.UserSecret();
        secret.setSecretAnswer(user.getSecretAnswer());
        secret.setSecretQuestion(user.getSecretQuestion());

        logger.info("Got Secret Q&A for user: {}", user);

        return Response.ok(secret).build();
    }

    /**
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userSecret
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userSecret
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @PUT
    public Response setUserSecret(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.UserSecret userSecret) {

        validateUserSecretParam(userSecret);

        logger.info("Updating Secret Q&A for User: {}", username);

        // get user to update
        User user = checkAndGetUser(customerId, username);

        if (!checkUserAuthorization(authHeader, user.getUsername(),
            "setUserSecret")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        user.setSecretQuestion(userSecret.getSecretQuestion());
        user.setSecretAnswer(userSecret.getSecretAnswer());
        this.userService.updateUser(user);

        logger.info("Updated Secret Q&A for user: {}", user);

        return Response.ok(userSecret).build();
    }

    private void validateUserSecretParam(UserSecret userSecret) {

        if (StringUtils.isBlank(userSecret.getSecretQuestion())
            || StringUtils.isBlank(userSecret.getSecretAnswer())) {
            String errMsg = "";
            if (StringUtils.isBlank(userSecret.getSecretQuestion())) {
                errMsg = "Secret Question cannot be blank. ";
            }
            if (StringUtils.isBlank(userSecret.getSecretAnswer())) {
                errMsg += "Secret Answer cannot be blank.";
            }
            throw new BadRequestException(errMsg);
        }
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

    private boolean checkUserAuthorization(String authHeader, String username,
        String methodName) {

        String subjectUserName = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        if (subjectUserName == null) {
            return false;
        }

        return authorizationHelper.checkUserAuthorization(subjectUserName,
            username, methodName);
    }
}
