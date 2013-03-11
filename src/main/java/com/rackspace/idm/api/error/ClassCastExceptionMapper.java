package com.rackspace.idm.api.error;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.rackspace.api.common.fault.v1.*;
import org.springframework.stereotype.Component;

@Component
@Provider
public class ClassCastExceptionMapper implements ExceptionMapper<ClassCastException> {
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();
    @Override
    public Response toResponse(ClassCastException exception) {
        BadRequestFault fault = new BadRequestFault();
        fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
    }
}
