package com.rackspace.idm.api.resource.user;

import java.net.URI;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UsersResource extends ParentResource {

    private final UserResource userResource;
    private final UserService userService;
    private final UserConverter userConverter;
    private final AuthorizationService authorizationService;


    @Autowired(required = true)
    public UsersResource(
    	UserResource userResource,UserService userService,
        InputValidator inputValidator, UserConverter userConverter,
        AuthorizationService authorizationService) {

    	super(inputValidator);

    	this.userResource = userResource;
        this.userService = userService;
        this.userConverter = userConverter;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets users
     *
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param username 
     */
    @GET
    public Response getUsers(
        @QueryParam("username") String username,
        @QueryParam("offset") Integer offset,
        @QueryParam("limit") Integer limit,
        @HeaderParam("X-Auth-Token") String authHeader) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        FilterParam[] filters = null;
    	if (!StringUtils.isBlank(username)) {
    		filters = new FilterParam[] { new FilterParam(FilterParamName.USERNAME, username)};
    	}
    	
    	Users users = this.userService.getAllUsers(filters, (offset == null ? -1 : offset), (limit == null ? -1 : limit));
    	
    	return Response.ok(userConverter.toUserListJaxb(users)).build();
    }

    /**
     * Adds a user. 
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param user New User
     */
    @POST
    public Response addUser(@Context Request reqRuest,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        com.rackspace.api.idm.v1.User holder) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        com.rackspace.api.idm.v1.User jaxbUser = holder;

        User user = userConverter.toUserDO(jaxbUser);
        user.setDefaults();
        validateDomainObject(user);

        this.userService.addUser(user);

        String locationUri = String.format("%s", user.getId());

        return Response.ok(userConverter.toUserJaxb(user)).location(URI.create(locationUri)).status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{userId}")
    public UserResource getUserResource() {
        return userResource;
    }
}
