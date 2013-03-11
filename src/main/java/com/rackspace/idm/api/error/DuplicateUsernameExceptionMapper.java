package com.rackspace.idm.api.error;

import com.rackspace.idm.exception.DuplicateUsernameException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.rackspace.api.idm.v1.*;
import org.springframework.stereotype.Component;

@Component
@Provider
public class DuplicateUsernameExceptionMapper implements ExceptionMapper<DuplicateUsernameException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();
    @Override
    public Response toResponse(DuplicateUsernameException exception) {
        UsernameConflictFault fault = new UsernameConflictFault();
        fault.setCode(Response.Status.CONFLICT.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createUsernameConflict(fault)).status(Response.Status.CONFLICT).build();
    }
}
