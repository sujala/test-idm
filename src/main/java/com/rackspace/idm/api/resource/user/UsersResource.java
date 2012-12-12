package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.impl.DefaultTenantService;
import com.rackspace.idm.validation.InputValidator;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UsersResource extends ParentResource {

    private final UserResource userResource;
    private final UserService userService;
    private final UserConverter userConverter;
    private final AuthorizationService authorizationService;
    private final ScopeAccessService scopeAccessService;

    @Autowired
    private UserValidatorFoundation userValidator;

    @Autowired
    private Configuration config;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired(required = true)
    public UsersResource(
    	UserResource userResource,UserService userService,
        InputValidator inputValidator, UserConverter userConverter,
        AuthorizationService authorizationService, ScopeAccessService scopeAccessService) {

    	super(inputValidator);

    	this.userResource = userResource;
        this.userService = userService;
        this.userConverter = userConverter;
        this.authorizationService = authorizationService;
        this.scopeAccessService = scopeAccessService;
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

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);

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
    public Response addUser(
        @HeaderParam("X-Auth-Token") String authHeader, com.rackspace.api.idm.v1.User user) throws IOException {
        userValidator.isUsernameEmpty(user.getUsername());
        userValidator.validateUsername(user.getUsername());
        //TODO enable if we want to check cloud auth for username conflicts
        //By adding this we can no longer create service accounts that already exist in Cloud Auth
//        if(config.getBoolean("useCloudAuth")){
//            userValidator.checkCloudAuthForUsername(user.getUsername());
//        }

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(scopeAccess);

        com.rackspace.api.idm.v1.User jaxbUser = user;

        User userDO = userConverter.toUserDO(jaxbUser);
        userDO.setDefaults();
        validateDomainObject(userDO);

        this.userService.addUser(userDO);

        String locationUri = String.format("%s", userDO.getId());

        return Response.ok(userConverter.toUserJaxb(userDO)).location(URI.create(locationUri)).status(HttpServletResponse.SC_CREATED).build();
    }

    @Path("{userId}")
    public UserResource getUserResource() {
        return userResource;
    }

    public void setUserValidator(UserValidatorFoundation userValidator) {
        this.userValidator = userValidator;
    }

    public Configuration getConfig() {
        return config;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }
}
