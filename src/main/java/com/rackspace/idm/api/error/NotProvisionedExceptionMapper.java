package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.NotProvisionedFault;
import com.rackspace.idm.exception.NotProvisionedException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class NotProvisionedExceptionMapper implements ExceptionMapper<NotProvisionedException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();

    @Override
    public Response toResponse(NotProvisionedException exception) {
        NotProvisionedFault fault = new NotProvisionedFault();
        fault.setCode(Response.Status.FORBIDDEN.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createNotProvisioned(fault)).status(Response.Status.FORBIDDEN).build();
    }
}
