package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.ClientGroupConflictFault;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class DuplicateClientGroupExceptionMapper implements ExceptionMapper<DuplicateClientGroupException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();
    @Override
    public Response toResponse(DuplicateClientGroupException exception) {
        ClientGroupConflictFault fault = new ClientGroupConflictFault();
        fault.setCode(Response.Status.CONFLICT.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createClientGroupConflict(fault)).status(Response.Status.CONFLICT).build();
    }
}
