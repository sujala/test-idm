package com.rackspace.idm.api.error;

import com.rackspace.idm.exception.NotAuthenticatedException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.rackspace.api.common.fault.v1.*;
import org.springframework.stereotype.Component;

@Component
@Provider
public class NotAuthenticatedExceptionMapper implements ExceptionMapper<NotAuthenticatedException> {
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();
    @Override
    public Response toResponse(NotAuthenticatedException exception) {
        UnauthorizedFault fault = new UnauthorizedFault();
        fault.setCode(Response.Status.UNAUTHORIZED.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createUnauthorized(fault)).status(Response.Status.UNAUTHORIZED).build();
    }
}
