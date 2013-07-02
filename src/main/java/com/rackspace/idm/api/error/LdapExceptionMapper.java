package com.rackspace.idm.api.error;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.unboundid.ldap.sdk.LDAPException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class LdapExceptionMapper implements ExceptionMapper<LDAPException> {
    private final com.rackspace.api.common.fault.v1.ObjectFactory objectFactory = new com.rackspace.api.common.fault.v1.ObjectFactory();

    @Override
    public Response toResponse(LDAPException exception) {
        switch (exception.getResultCode().intValue()){
            case 68:
            default:
        }
        BadRequestFault fault = new BadRequestFault();
        fault.setCode(Response.Status.BAD_REQUEST.getStatusCode());
        fault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createBadRequest(fault)).status(Response.Status.BAD_REQUEST).build();
    }
}
