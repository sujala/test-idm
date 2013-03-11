package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.BaseUrlIdConflictFault;
import com.rackspace.idm.exception.BaseUrlConflictException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class BaseUrlConflictExceptionMapper implements ExceptionMapper<BaseUrlConflictException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();
    @Override
    public Response toResponse(BaseUrlConflictException exception) {
        BaseUrlIdConflictFault fault = new BaseUrlIdConflictFault();
        fault.setCode(Response.Status.CONFLICT.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createBaseUrlIdConflict(fault)).status(Response.Status.CONFLICT).build();
    }
}
