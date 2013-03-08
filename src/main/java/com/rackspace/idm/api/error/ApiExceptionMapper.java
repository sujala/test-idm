package com.rackspace.idm.api.error;

import com.rackspace.api.common.fault.v1.*;
import org.omg.CORBA.portable.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {
    private final Logger logger = LoggerFactory.getLogger(ApiExceptionMapper.class);
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();

    public ApiExceptionMapper() {}

    @Override
    public Response toResponse(Throwable thrown) {
        Throwable e = thrown;

        if (thrown instanceof ApplicationException) {
            e = thrown.getCause();
        }

        logger.error(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        logger.error(e.getMessage());
        logger.error("Exception is :::",e);
        ServiceFault sfault = new ServiceFault();
        sfault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        sfault.setMessage("Server Error");
        return Response.ok(objectFactory.createServiceFault(sfault)).status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
    }
}
