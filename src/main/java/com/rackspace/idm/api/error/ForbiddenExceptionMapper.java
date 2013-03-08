package com.rackspace.idm.api.error;

import com.rackspace.idm.exception.ForbiddenException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.rackspace.api.common.fault.v1.*;
import org.springframework.stereotype.Component;

@Component
@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();
    @Override
    public Response toResponse(ForbiddenException exception) {
        ForbiddenFault fault = new ForbiddenFault();
        fault.setCode(Response.Status.FORBIDDEN.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createForbidden(fault)).status(Response.Status.FORBIDDEN).build();
    }
}
