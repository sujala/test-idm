package com.rackspace.idm.api.error;

import com.rackspace.api.common.fault.v1.ServiceFault;
import com.rackspace.idm.exception.DuplicateException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class DuplicateExceptionMapper implements ExceptionMapper<DuplicateException> {
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();
    @Override
    public Response toResponse(DuplicateException exception) {
        ServiceFault fault = new ServiceFault();
        fault.setCode(Response.Status.CONFLICT.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.status(Response.Status.CONFLICT).entity(objectFactory.createServiceFault(fault)).build();
    }
}
