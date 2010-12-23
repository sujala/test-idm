package com.rackspace.idm.controllers;

import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.authorizationService.AuthorizationConstants;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.PasswordConverter;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.DuplicateUsernameException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.IdmException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.PasswordValidationException;
import com.rackspace.idm.exceptions.UserDisabledException;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.jaxb.UserApiKey;
import com.rackspace.idm.jaxb.UserSecret;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.InputValidator;

/**
 * Users resource
 */
@Path("/customers/{customerId}/users")
@NoCache
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UsersController {

    private OAuthService oauthService;
    private AccessTokenService accessTokenService;
    private UserService userService;
    private RoleService roleService;
    private PasswordComplexityService passwordComplexityService;
    private RoleConverter roleConverter;
    private UserConverter userConverter;
    private PasswordConverter passwordConverter;
    private TokenConverter tokenConverter;

    private InputValidator inputValidator;
    private IDMAuthorizationHelper authorizationHelper;
    private AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();
    private Logger logger;
    String errorMsg = String.format("Authorization Failed");

    @Autowired
    public UsersController(OAuthService oauthService,
        AccessTokenService accessTokenService, UserService userService,
        RoleService roleService, IDMAuthorizationHelper idmAuthHelper,
        PasswordComplexityService passwordComplexityService,
        UserConverter userConverter, RoleConverter roleConverter,
        PasswordConverter passwordConverter, TokenConverter tokenConverter,
        InputValidator inputValidator, LoggerFactoryWrapper logger) {
        this.oauthService = oauthService;
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.roleService = roleService;
        this.inputValidator = inputValidator;
        this.authorizationHelper = idmAuthHelper;
        this.passwordComplexityService = passwordComplexityService;
        this.userConverter = userConverter;
        this.roleConverter = roleConverter;
        this.passwordConverter = passwordConverter;
        this.tokenConverter = tokenConverter;
        this.logger = logger.getLogger(UsersController.class);
    }

    /**
     * Add a user.
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param user
     *            User representation
     * 
     * @return Newly created User representation
     * 
     * @HTTP 200 If user is added
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @ResponseHeader Location URI of the newly added user.
     */
    @POST
    @Path("")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.User addUser(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        com.rackspace.idm.jaxb.User user) {

        user.setCustomerId(customerId);

        User userDO = userConverter.toUserDO(user);
        userDO.setDefaults();

        validateParam(userDO);

        logger.info("Adding User: {}", user.getUsername());

        String httpMethodName = "POST";
        String requestURI = "/customers/" + user.getCustomerId() + "/users";

        if (!authorizeAdminOrRackspaceClient(authHeader, userDO, "addUser",
            httpMethodName, requestURI)) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        if (userDO.getPasswordObj() == null
            || StringUtils.isBlank(userDO.getPasswordObj().getValue())) {
            Password newpassword = Password.generateRandom();
            userDO.setPasswordObj(newpassword);
        } else {
            String password = userDO.getPasswordObj().getValue();
            if (!passwordComplexityService.checkPassword(password)
                .isValidPassword()) {
                String errorMsg = String
                    .format("Invalid password {}", password);
                logger.warn(errorMsg);
                throw new PasswordValidationException(errorMsg);
            }
        }

        try {
            this.userService.addUser(userDO);
        } catch (IllegalStateException ex) {
            String errorMsg = "User not added because customer doesn't exist.";
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.error(errorMsg);
            throw new DuplicateUsernameException(errorMsg);
        }

        logger.info("Added User: {}", user);

        String locationUri = String.format("/customers/%s/users/%s", userDO
            .getCustomerId(), userDO.getUsername());
        response.setHeader("Location", locationUri);
        response.setStatus(HttpServletResponse.SC_CREATED);

        return userConverter.toUserJaxb(userDO);
    }

    /**
     * Role resource.
     * 
     * Delete a user's role.
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            the username for whom we want to set the role.
     * @param roleName
     *            name of role to delete
     * 
     * 
     * @HTTP 200 If the user's role is set successfully.
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @DELETE
    @Path("{username}/roles/{roleName}")
    public void deleteRole(@Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("roleName") String roleName) {

        logger.info("Deleting role for User: {}", username);

        // get user to update
        User user = this.userService.getUser(customerId, username);

        if (user == null) {
            String errorMsg = String.format(
                "Set Role Failed - User not found: %s", username);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        if (!authorizeDeleteRole(authHeader, user, "deleteRole", roleName)) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {

                authorizationHelper.handleAuthorizationFailure();
            }
        }

        // get role to add user to
        Role role = this.roleService.getRole(roleName, user.getCustomerId());

        if (role == null) {
            String errorMsg = String.format(
                "Set Role Failed - Role not found: {}", roleName);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        this.roleService.deleteUserFromRole(user, role);

        logger.info("User {} deleted from role {}", user, role);
    }

    /**
     * Delete a user
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username of the user to delete
     * 
     * @HTTP 204 If the user is deleted
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @DELETE
    @Path("{username}")
    public void deleteUser(@Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.info("Deleting User :{}", username);
        User user = checkAndGetUser(customerId, username);

        if (!authorizeDeleteUser(authHeader, user, "deleteUser")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        this.userService.deleteUser(username);

        logger.info("Deleted User: {}", user);
    }

    /**
     * Role resource.
     * 
     * Get a user's roles.
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username for whom we want to get the roles.
     * 
     * 
     * @HTTP 200 If the user's roles are found.
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @GET
    @Path("{username}/roles/")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Roles getRoles(
        @Context HttpServletResponse response,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.debug("Getting roles for User: {}", username);

        User user = checkAndGetUser(customerId, username);

        // get roles for user
        List<Role> roles = this.roleService.getRolesForUser(username);
        logger.debug("Got roles for User: {} - {}", user, roles);

        com.rackspace.idm.jaxb.Roles outputRoles = roleConverter
            .toRolesJaxb(roles);

        return outputRoles;
    }

    /**
     * User resource.
     * 
     * Single user and its attributes.
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username of the user to retrieve
     * @return A user resource, if it exists
     * 
     * @HTTP 200 If an existing user is found
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @GET
    @Path("{username}")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.User getUser(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.debug("Getting User: {}", username);
        User user = checkAndGetUser(customerId, username);

        if (!checkUserOrAdminAuthorization(authHeader, user, "getUser")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        logger.debug("Got User :{}", user);
        return userConverter.toUserWithOnlyRolesJaxb(user);
    }

    /**
     * Password resource.
     * 
     * Get a user's password
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username of the password to retrieve
     * @return A password resource, if it exists
     * 
     * @HTTP 200 If an existing user is found
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @GET
    @Path("{username}/password")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.UserPassword getUserPassword(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        User user = checkAndGetUser(customerId, username);

        String httpMethodName = "GET";
        String requestURI = "/customers/" + user.getCustomerId() + "/users/"
            + user.getUsername() + "/password";

        if (!authorizeAdminOrRackspaceClient(authHeader, user,
            "getUserPassword", httpMethodName, requestURI)) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        Password password = Password.existingInstance(user
            .getPasswordNoPrefix());

        return passwordConverter.toJaxb(password);
    }

    @POST
    @Path("{username}/password/recoverytoken")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.Token getPasswordResetToken(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

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

        String httpMethodName = "POST";
        if (!authorizeGetPasswordResetToken(authHeader, user, httpMethodName)) {
            authorizationHelper.handleAuthorizationFailure();
        }

        String clientId = oauthService
            .getClientIdFromAuthHeaderToken(authHeader);

        AccessToken token = accessTokenService
            .createPasswordResetAccessTokenForUser(username, clientId);

        logger.debug("Got Password Reset Token for User :{}", user);

        return tokenConverter.toAccessTokenJaxb(token);

    }

    /**
     * Password resource.
     * 
     * Reset a user's password. A password is randomly generated.
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username of the user to reset the password
     * @return Newly reset password
     * 
     * @HTTP 200 If the user's password is reset successfully
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @POST
    @Path("{username}/password")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.UserPassword resetUserPassword(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.info("Reseting Password for User: {}", username);

        User user = checkAndGetUser(customerId, username);

        if (!checkUserOrAdminAuthorization(authHeader, user,
            "resetUserPassword")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        Password newpassword = Password.generateRandom();
        user.setPasswordObj(newpassword);
        this.userService.updateUser(user);
        logger.info("Updated password for user: {}", user);

        return passwordConverter.toJaxb(newpassword);
    }

    /**
     * Password resource.
     * 
     * Request a password recovery email
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username for whom we want to set the role.
     * @param callbackUrl
     *            The callback URL for password reset
     * 
     * 
     * @HTTP 204 If email is sent successfully.
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 403 If user is inactive or locked
     * @HTTP 404 If user is not found
     * @HTTP 409 If user doesn't have an email address
     */
    @POST
    @Path("{username}/password/recoveryemail")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public void sendRecoveryEmail(@Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username, PasswordRecovery recoveryParam) {

        logger.info("Sending password recovery email for User: {}", username);

        User user = checkAndGetUser(customerId, username);

        if (!authorizeSendRecoveryEmail(authHeader, user, "sendRecoveryEmail")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

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
    }

    /**
     * Role resource.
     * 
     * Set a user's role.
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username for whom we want to set the role.
     * 
     * 
     * @HTTP 200 If the user's role is set successfully.
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @PUT
    @Path("{username}/roles/{roleName}")
    public void setRole(@Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("roleName") String roleName) {

        if (StringUtils.isBlank(roleName)) {
            String errorMsg = "RoleName cannot be blank";
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        logger.info("Setting role {} for User {}", roleName, username);

        // get user to update
        User user = checkAndGetUser(customerId, username);

        if (!authorizeSetRole(authHeader, user, "setRole", roleName)) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        // get role to add user to
        Role role = this.roleService.getRole(roleName, user.getCustomerId());

        if (role == null) {
            String errorMsg = String.format(
                "Set Role Failed - Role not found: {}", roleName);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        this.roleService.addUserToRole(user, role);

        logger.info("Set the role {} for user {}", role, user);
    }

    /**
     * Set a user's password
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username of the user to update
     * @param oldpassword
     *            The user's old password, used for authentication
     * @param newpassword
     *            The new password to set
     * 
     * @HTTP 204 If the user's password is updated successfully
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @PUT
    @Path("{username}/password")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.UserCredentials setUserPassword(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.UserCredentials userCred,
        @QueryParam("recovery") boolean isRecovery) {

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

        if (!checkUserAuthorization(authHeader, user.getUsername(),
            "setUserPassword")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        user.setPasswordObj(Password.newInstance(userCred.getNewPassword()
            .getPassword()));
        this.userService.updateUser(user);
        logger.info("Updated password for user: {}", user);
        return userCred;
    }

    /**
     * Set a user's secret question and answer
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username of the user to update
     * @param secretQuestion
     *            The new secret question to set
     * @param secretAnswer
     *            The new secret answer to set
     * 
     * @HTTP 204 If the user's secret is updated successfully
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @PUT
    @Path("{username}/secret")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.UserSecret setUserSecret(
        @Context HttpServletResponse response,
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

        return userSecret;
    }

    /**
     * Set a user's status. The possible status values are: ACTIVE, INACTIVE
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username of the user to update
     * @param the
     *            status The new status to set
     * 
     * @HTTP 204 If the user's status is updated successfully
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @PUT
    @Path("{username}/status")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.User setUserStatus(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.User inputUser) {

        logger.info("Updating Status for User: {}", username);

        // get user to update
        User user = checkAndGetUser(customerId, username);

        if (!authorizeSetUserStatus(authHeader, user.getCustomerId(),
            "setUserStatus")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        String statusStr = inputUser.getStatus().value();
        try {
            this.userService.updateUserStatus(user, statusStr);
        } catch (IllegalArgumentException ex) {
            String errorMsg = String.format("Invalid status value: %s",
                statusStr);
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        logger.info("Updated status for user: {}", user);

        User outputUser = this.userService.getUser(customerId, username);
        return userConverter.toUserWithOnlyStatusJaxb(outputUser);
    }

    /**
     * Set a user's softDelete.
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username of the user to update
     * @param softDelete
     *            The new softDelete value
     * 
     * @HTTP 204 If the user's status is updated successfully
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @PUT
    @Path("{username}/softdelete")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.User setUserSoftDelete(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.User inputUser) {

        logger.info("Updating SoftDelete for User: {} - {}", username,
            inputUser.isSoftDeleted());

        boolean softDelete = inputUser.isSoftDeleted();

        // get user to update
        User user;
        if (softDelete) {
            user = checkAndGetUser(customerId, username);
        } else {
            user = this.userService.getSoftDeletedUser(customerId, username);
        }

        if (user == null) {
            handleUserNotFoundError(customerId, username);
        }

        user.setSoftDeleted(softDelete);

        if (!authorizeDeleteUser(authHeader, user, "deleteUser")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        if (softDelete) {
            this.userService.softDeleteUser(username);
        } else {
            this.userService.restoreSoftDeletedUser(user);
        }

        logger.info("Updated SoftDelete for user: {} - []", user);

        return userConverter.toUserWithOnlySoftDeletedJaxb(user);
    }

    @PUT
    @Path("{username}/lock")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.User setUserLock(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.User inputUser) {

        logger.info("Locking User: {} - {}", username);

        boolean lockStatus = inputUser.isLocked();

        User user = checkAndGetUser(customerId, username);

        if (user == null) {
            handleUserNotFoundError(customerId, username);
        }

        String methodName = "setUserLock";
        String httpMethodName = "PUT";
        String requestURI = "/customers/" + user.getCustomerId() + "/users/"
            + user.getUsername() + "/lock";
        if (!authorizeRackerOrRackspaceApplication(authHeader, user,
            methodName, httpMethodName, requestURI)) {
            authorizationHelper.handleAuthorizationFailure();
        }

        user.setIsLocked(lockStatus);

        this.userService.updateUser(user);

        return userConverter.toUserWithOnlyLockJaxb(user);
    }

    /**
     * Update a user's attributes.
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            Username of the user to update
     * @param updatedUser
     * 
     * @HTTP 200 If user is updated successfully
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     */
    @PUT
    @Path("{username}")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.User updateUser(
        @Context HttpServletResponse response,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        com.rackspace.idm.jaxb.User inputUser) {

        User updatedUser = userConverter.toUserDO(inputUser);

        logger.info("Updating User: {}", username);

        User user = checkAndGetUser(customerId, username);

        if (!checkUserOrAdminAuthorization(authHeader, user, "udpateUser")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        user.copyChanges(updatedUser);
        validateParam(user);

        try {
            this.userService.updateUser(user);
        } catch (DuplicateException ex) {
            String errorMsg = ex.getMessage();
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        logger.info("Updated User: {}", user);
        return userConverter.toUserWithOnlyRolesJaxb(user);
    }

    /**
     * Cloud Auth Service API key resource.
     * 
     * Reset the API key. A key is randomly generated.
     * 
     * @RequestHeader Authorization Authorization header
     * 
     * @param customerId
     *            the customerId
     * @param username
     *            The username of the user to reset the password
     * @return Newly reset API key
     * 
     * @HTTP 200 If the user's API key is reset successfully
     * @HTTP 400 If parameters are missing or malformed
     * @HTTP 401 If unauthorized
     * @HTTP 404 If user is not found
     */
    @POST
    @Path("{username}/key")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.UserApiKey resetApiKey(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.info("Reseting Cloud Auth service API key for User: {}",
            username);

        // get user to update
        User user = checkAndGetUser(customerId, username);
        if (!checkUserOrAdminAuthorization(authHeader, user, "resetApiKey")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        // generate random api key
        String apiKey = userService.generateApiKey();
        user.setApiKey(apiKey);
        this.userService.updateUser(user);

        logger.info("Reset Cloud Auth service API key for user: {}", user);

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setApiKey(apiKey);

        return userApiKey;
    }
    
    @GET
    @Path("{username}/key")
    @Mapped(namespaceMap = {@XmlNsMap(namespace = GlobalConstants.XML_NAMESPACE, jsonName = GlobalConstants.JSON_NAMESPACE)})
    public com.rackspace.idm.jaxb.UserApiKey getApiKey(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {
        logger.debug("Reseting Cloud Auth service API key for User: {}",
            username);

        //FIXME: We need to discuss who should be authorized to get an apiKey
        // get user to update
        User user = checkAndGetUser(customerId, username);
        if (!checkUserOrAdminAuthorization(authHeader, user, "resetApiKey")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        logger.debug("Retrieved Cloud Auth service API key for user: {}", user);

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setApiKey(user.getApiKey());

        return userApiKey;
    }

    // private functions
    private boolean authorizeAdminOrRackspaceClient(String authHeader,
        User user, String methodName, String httpMethodName, String requestURI) {

        String userCompanyId = user.getCustomerId();
        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        if (subjectUsername == null) {
            // Condition 1: RACKSPACE Company can add user.

            if (!authorizationHelper.checkPermission(authHeader,
                httpMethodName, requestURI)) {
                return authorizationHelper.checkRackspaceClientAuthorization(
                    authHeader, methodName);
            } else {
                return true;
            }

        } else {
            // Condition 2: Admin can add a user.
            return authorizationHelper.checkAdminAuthorizationForUser(
                subjectUsername, userCompanyId, methodName);
        }
    }

    private boolean authorizeRackerOrRackspaceApplication(String authHeader,
        User user, String methodName, String httpMethodName, String requestURI) {

        if (!authorizationHelper.checkPermission(authHeader, httpMethodName,
            requestURI)) {

            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                return authorizationHelper.checkRackspaceClientAuthorization(
                    authHeader, methodName);
            }
        }
        return true;
    }

    private boolean authorizeDeleteUser(String authHeader, User user,
        String methodName) {
        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        String companyId = user.getCustomerId();

        if (subjectUsername == null) {
            return false;
        }

        // Condition 1: Admin can delete user.
        if (authorizationHelper.checkAdminAuthorizationForUser(subjectUsername,
            companyId, methodName)) {

            // Condition 2: Admin cannot delete himself/herself.
            if (!subjectUsername.equalsIgnoreCase(user.getUsername())) {
                return true;
            }
        }
        return false;
    }

    private boolean authorizeDeleteRole(String authHeader, User user,
        String methodName, String rolename) {

        String requestor = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        String targetUser = user.getUsername();

        // Condition 1: An admin can delete other user's any role (including
        // admin role).
        if (!authorizationHelper.checkAdminAuthorizationForUser(requestor, user
            .getCustomerId(), methodName)) {
            return false;
        }

        // Condition 2: An admin cannot delete his/her own admin role.
        if (requestor.equalsIgnoreCase(targetUser)
            && rolename.equalsIgnoreCase(AuthorizationConstants.ADMIN_ROLE)) {
            return false;
        }
        return true;
    }

    private boolean authorizeSendRecoveryEmail(String authHeader, User user,
        String methodName) {
        return authorizationHelper.checkRackspaceClientAuthorization(
            authHeader, methodName);
    }

    private boolean authorizeGetPasswordResetToken(String authHeader,
        User user, String httpMethodName) {

        String requestURI = "/customers/" + user.getCustomerId() + "/users/"
            + user.getUsername() + "/password/recoverytoken";

        return authorizationHelper.checkPermission(authHeader, httpMethodName,
            requestURI);
    }

    private boolean authorizeSetRole(String authHeader, User user,
        String methodName, String roleName) {

        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        if (subjectUsername == null) {
            String httpMethodName = "PUT";
            String requestURI = "/customers/" + user.getCustomerId()
                + "/users/" + user.getUsername() + "/roles/" + roleName;

            return authorizationHelper.checkPermission(authHeader,
                httpMethodName, requestURI);
        } else {
            return authorizationHelper.checkAdminAuthorizationForUser(
                subjectUsername, user.getCustomerId(), methodName);
        }
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

    private boolean authorizeSetUserStatus(String authHeader,
        String userCompanyId, String methodName) {

        // Admin can change user status, Or if there is no admin then a
        // company's client can change user status.
        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);
        if (subjectUsername == null) {
            // Condition 1: User's company can set user's status.
            return authorizationHelper.checkCompanyAuthorization(authHeader,
                userCompanyId, methodName);
        } else {
            // Condition 2: Admin can set user's status.
            return authorizationHelper.checkAdminAuthorizationForUser(
                subjectUsername, userCompanyId, methodName);
        }
    }

    private boolean checkUserOrAdminAuthorization(String authHeader, User user,
        String methodName) {

        String userName = user.getUsername();
        String companyId = user.getCustomerId();

        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        if (subjectUsername == null) {
            return false;
        }

        // Condition 1: User can do stuff.
        if (!authorizationHelper.checkUserAuthorization(subjectUsername,
            userName, methodName)) {

            // Condition 2: An Admin can do stuff.
            if (!authorizationHelper.checkAdminAuthorizationForUser(
                subjectUsername, companyId, methodName)) {
                return false;
            }
        }
        return true;
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

    private void validateParam(Object inputParam) {
        ApiError err = inputValidator.validate(inputParam);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }
    }

    private void validateUserSecretParam(UserSecret userSecret) {

        if (userSecret.getSecretQuestion() == null
            || userSecret.getSecretAnswer() == null) {
            String errMsg = "";
            if (userSecret.getSecretQuestion() == null) {
                errMsg = "Secret Question cannot be null.";
            }
            if (userSecret.getSecretAnswer() == null) {
                errMsg = "Secret Answer cannot be null.";
            }
            throw new BadRequestException(errMsg);
        }

    }
}
