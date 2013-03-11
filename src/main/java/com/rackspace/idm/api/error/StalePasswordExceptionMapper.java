package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.StalePasswordFault;
import com.rackspace.idm.exception.StalePasswordException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class StalePasswordExceptionMapper implements ExceptionMapper<StalePasswordException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();
    @Override
    public Response toResponse(StalePasswordException exception) {
        StalePasswordFault fault = new StalePasswordFault();
        fault.setCode(Response.Status.CONFLICT.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createStalePasswordFault(fault)).status(Response.Status.CONFLICT).build();
    }
}
