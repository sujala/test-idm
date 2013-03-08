package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.ApplicationNameConflictFault;
import com.rackspace.idm.exception.ClientConflictException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class ClientConflictExceptionMapper implements ExceptionMapper<ClientConflictException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();
    @Override
    public Response toResponse(ClientConflictException exception) {
        ApplicationNameConflictFault fault = new ApplicationNameConflictFault();
        fault.setCode(Response.Status.CONFLICT.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createApplicationNameConflict(fault)).status(Response.Status.CONFLICT).build();
    }
}
