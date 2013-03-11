package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.PasswordValidationFault;
import com.rackspace.idm.exception.PasswordValidationException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class PasswordValidationExceptionMapper implements ExceptionMapper<PasswordValidationException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();
    @Override
    public Response toResponse(PasswordValidationException exception) {
        PasswordValidationFault fault = new PasswordValidationFault();
        fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createPasswordValidationFault(fault)).status(Response.Status.BAD_REQUEST).build();
    }
}
