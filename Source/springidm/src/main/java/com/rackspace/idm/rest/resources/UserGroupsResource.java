package com.rackspace.idm.rest.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.GroupConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.ClientGroup;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.UserService;

/**
 * A users groups.
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserGroupsResource {

    private AccessTokenService accessTokenService;
    private UserService userService;
    private ClientService clientService;
    private GroupConverter groupConverter;
    private AuthorizationService authorizationService;
    private Logger logger;
    private Configuration config;

    @Autowired
    public UserGroupsResource(AccessTokenService accessTokenService,
        UserService userService, ClientService clientService,
        GroupConverter groupConverter, Configuration config,
        AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.clientService = clientService;
        this.groupConverter = groupConverter;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
        this.config = config;
    }

    /**
     * Gets a list of the groups a user is a member of.
     * 
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}roles
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
    public Response getGroups(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username) {

        logger.debug("Getting groups for User: {}", username);

        AccessToken token = this.accessTokenService
            .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients, Specific Clients, Admins and User's are
        // authorized
        boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
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

        User user = checkAndGetUser(customerId, username);

        // get roles for user
        List<ClientGroup> groups = this.clientService
            .getClientGroupsForUser(username);
        logger.debug("Got groups for User: {} - {}", user, groups);

        com.rackspace.idm.jaxb.ClientGroups outputGroups = groupConverter
            .toClientGroupsJaxb(groups);

        return Response.ok(outputGroups).build();
    }

    /**
     * Add a user to a Customer Idm group
     * 
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
     * @param groupName Group to add user to
     */
    @PUT
    @Path("{groupName}")
    public Response addUserToGroup(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("groupName") String groupName) {

        if (StringUtils.isBlank(groupName)) {
            String errorMsg = "Group name cannot be blank";
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        logger.info("Adding user {} to group {}", username, groupName);

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

        // get user to update
        User user = checkAndGetUser(customerId, username);

        if (user == null) {
            String errorMsg = String.format(
                "Add User to Group Failed - User not found: %s", username);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        ClientGroup group = this.clientService.getClientGroup(
            getRackspaceCustomerId(), getIdmClientId(), groupName);

        if (group == null) {
            String errorMsg = String.format(
                "Add User to Group Failed - Group not found: {}", groupName);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        this.clientService.addUserToClientGroup(username, group);

        logger.info("Added user {} to group {}", user, group);

        return Response.noContent().build();
    }

    /**
     * Remove a user from a Customer Idm group
     * 
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
     * @param groupName Group to delete user from
     */
    @DELETE
    @Path("{groupName}")
    public Response removeUserFromGroup(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader,
        @PathParam("customerId") String customerId,
        @PathParam("username") String username,
        @PathParam("groupName") String groupName) {

        logger.info("Removing user {} from group {}", username, groupName);

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

        // get user to update
        User user = this.userService.getUser(customerId, username);

        if (user == null) {
            String errorMsg = String.format(
                "Remove User From Group Failed - User not found: %s", username);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        ClientGroup group = this.clientService.getClientGroup(
            getRackspaceCustomerId(), getIdmClientId(), groupName);

        if (group == null) {
            String errorMsg = String.format(
                "Remove User From Group Failed - Group not found: {}",
                groupName);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        this.clientService.removeUserFromClientGroup(username, group);

        logger.info("User {} removed from group {}", user, group);

        return Response.noContent().build();
    }

    private User checkAndGetUser(String customerId, String username) {
        User user = this.userService.getUser(customerId, username);
        if (user == null) {
            handleUserNotFoundError(customerId, username);
        }
        return user;
    }

    private String getIdmClientId() {
        return config.getString("idm.clientId");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }

    private void handleUserNotFoundError(String customerId, String username) {
        String errorMsg = String.format("User not found: %s - %s", customerId,
            username);
        logger.error(errorMsg);
        throw new NotFoundException(errorMsg);
    }
}
