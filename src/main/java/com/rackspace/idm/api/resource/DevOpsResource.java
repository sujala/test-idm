package com.rackspace.idm.api.resource;

import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class DevOpsResource {

    @Autowired
    UserService userService;

    @PUT
    @Path("cloud/users/encrypt")
    public Response encryptUsers() {
        userService.reEncryptUsers();
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
