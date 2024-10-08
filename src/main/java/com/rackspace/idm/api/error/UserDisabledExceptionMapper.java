package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.UserDisabledFault;
import com.rackspace.idm.exception.UserDisabledException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class UserDisabledExceptionMapper implements ExceptionMapper<UserDisabledException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();
    @Override
    public Response toResponse(UserDisabledException exception) {
        UserDisabledFault fault = new UserDisabledFault();
        fault.setCode(Response.Status.FORBIDDEN.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createUserDisabled(fault)).status(Response.Status.FORBIDDEN).build();
    }
}
