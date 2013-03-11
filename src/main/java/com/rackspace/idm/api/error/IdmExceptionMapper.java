package com.rackspace.idm.api.error;

import com.rackspace.api.common.fault.v1.ServiceFault;
import com.rackspace.idm.exception.IdmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class IdmExceptionMapper implements ExceptionMapper<IdmException> {
    private final Logger logger = LoggerFactory.getLogger(IdmExceptionMapper.class);
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();
    @Override
    public Response toResponse(IdmException exception) {
        ServiceFault fault = new ServiceFault();
        fault.setCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        logger.info(exception.getMessage());
        return Response.ok(objectFactory.createServiceFault(fault)).status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
}
