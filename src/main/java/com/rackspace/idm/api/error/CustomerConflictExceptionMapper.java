package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.CustomerIdConflictFault;
import com.rackspace.idm.exception.CustomerConflictException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class CustomerConflictExceptionMapper implements ExceptionMapper<CustomerConflictException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();
    @Override
    public Response toResponse(CustomerConflictException exception) {
        CustomerIdConflictFault fault = new CustomerIdConflictFault();
        fault.setCode(Response.Status.CONFLICT.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createCustomerIdConflict(fault)).status(Response.Status.CONFLICT).build();
    }
}
