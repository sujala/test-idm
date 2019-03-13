package com.rackspace.idm.api.error;

import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.GatewayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class GatewayExceptionMapper implements ExceptionMapper<GatewayException> {
    @Autowired
    private ExceptionHandler exceptionHandler;

    @Override
    public Response toResponse(GatewayException exception) {
        return exceptionHandler.exceptionResponse(exception).build();
    }
}
