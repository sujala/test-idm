package com.rackspace.idm.api.error;

import com.rackspace.api.idm.v1.PermisionIdConflictFault;
import com.rackspace.idm.exception.PermissionConflictException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class PermissionConflictExceptionMapper implements ExceptionMapper<PermissionConflictException> {
    private final com.rackspace.api.idm.v1.ObjectFactory objectFactory = new com.rackspace.api.idm.v1.ObjectFactory();

    @Override
    public Response toResponse(PermissionConflictException exception) {
        PermisionIdConflictFault fault = new PermisionIdConflictFault();
        fault.setCode(Response.Status.CONFLICT.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createPermissionIdConflict(fault)).status(Response.Status.CONFLICT).build();
    }
}
