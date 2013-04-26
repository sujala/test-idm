package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.cloud.devops.DevOpsService;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspace.idm.exception.NotAuthorizedException;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DevOpsResource {
    @Autowired
    DevOpsService devOpsService;

    private static final String X_AUTH_TOKEN = "X-AUTH-TOKEN";

    @PUT
    @Path("cloud/users/encrypt")
    public Response encryptUsers(@HeaderParam(X_AUTH_TOKEN) String authToken) {
        devOpsService.encryptUsers(authToken);
        return Response.status(Response.Status.ACCEPTED).build();
    }


}
