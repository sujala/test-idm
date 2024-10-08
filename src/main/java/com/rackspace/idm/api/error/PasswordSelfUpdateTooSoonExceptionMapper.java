package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.PasswordSelfUpdateTooSoonFault;
import com.rackspace.idm.exception.PasswordSelfUpdateTooSoonException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class PasswordSelfUpdateTooSoonExceptionMapper implements ExceptionMapper<PasswordSelfUpdateTooSoonException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();
    @Override
    public Response toResponse(PasswordSelfUpdateTooSoonException exception) {
        PasswordSelfUpdateTooSoonFault fault = new PasswordSelfUpdateTooSoonFault();
        fault.setCode(Response.Status.CONFLICT.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createPasswordSelfUpdateTooSoonFault(fault)).status(Response.Status.CONFLICT).build();
    }
}
