package com.rackspace.idm.api.resource.customeridentityprofile;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component("identityProfileUsersResource")
public class UsersResource extends ParentResource {

    private final UserService userService;
    private final UserConverter userConverter;
    private final AuthorizationService authorizationService;

    @Autowired
    public UsersResource(
            InputValidator inputValidator, UserConverter userConverter,
            AuthorizationService authorizationService,
            UserService userService) {

        super(inputValidator);

        this.userConverter = userConverter;
        this.authorizationService = authorizationService;
        this.userService = userService;
    }

    /**
     * Gets users that are bound to a specific customer
     *
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param customerId customer Id
     */
    @GET
    public Response getUsers(
            @HeaderParam("X-Auth-Token") String authHeader,
            @PathParam("customerId") String customerId,
            @QueryParam("offset") Integer offset,
            @QueryParam("limit") Integer limit) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);

        FilterParam[] filters = new FilterParam[]{new FilterParam(FilterParamName.RCN, customerId)};

        //TODO: Implement Authorization rules
        Users users = userService.getAllUsers(filters, (offset == null ? -1 : offset), (limit == null ? -1 : limit));

        return Response.ok(userConverter.toUserListJaxb(users)).build();
    }
}
