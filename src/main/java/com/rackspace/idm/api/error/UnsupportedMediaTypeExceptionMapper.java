package com.rackspace.idm.api.error;

import com.rackspace.api.common.fault.v1.UnsupportedMediaTypeFault;
import com.rackspace.idm.exception.UnsupportedMediaTypeException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class UnsupportedMediaTypeExceptionMapper implements ExceptionMapper<UnsupportedMediaTypeException> {
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();

    @Override
    public Response toResponse(UnsupportedMediaTypeException exception) {
        UnsupportedMediaTypeFault fault = new UnsupportedMediaTypeFault();
        fault.setCode(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createUnsupportedMediaType(fault)).status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
    }
}
