package com.rackspace.idm.api.error;

import com.rackspace.api.common.fault.v1.ItemNotFoundFault;
import com.sun.jersey.api.NotFoundException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class JerseyNotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();
    @Override
    public Response toResponse(NotFoundException exception) {
        ItemNotFoundFault fault = new ItemNotFoundFault();
        fault.setCode(Response.Status.NOT_FOUND.getStatusCode());
        fault.setMessage("Resource Not Found");
        return Response.ok(objectFactory.createItemNotFound(fault)).status(Response.Status.NOT_FOUND).build();
    }
}
