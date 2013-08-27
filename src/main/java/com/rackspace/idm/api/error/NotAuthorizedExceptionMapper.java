package com.rackspace.idm.api.error;

import com.rackspace.api.common.fault.v1.UnauthorizedFault;
import com.rackspace.idm.exception.NotAuthorizedException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class NotAuthorizedExceptionMapper implements ExceptionMapper<NotAuthorizedException> {
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();
    @Override
    public Response toResponse(NotAuthorizedException exception) {
        UnauthorizedFault fault = new UnauthorizedFault();
        fault.setCode(Response.Status.UNAUTHORIZED.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createUnauthorized(fault)).status(Response.Status.UNAUTHORIZED).build();
    }
}
