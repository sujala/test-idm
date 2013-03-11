package com.rackspace.idm.api.error;

import com.rackspace.idm.exception.CloudAdminAuthorizationException;
import com.rackspacecloud.docs.auth.api.v1.AuthFault;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Component
@Provider
public class CloudAdminAuthorizationExceptionMapper implements ExceptionMapper<CloudAdminAuthorizationException> {
    private final com.rackspacecloud.docs.auth.api.v1.ObjectFactory objectFactory = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();
    @Override
    public Response toResponse(CloudAdminAuthorizationException exception) {
        AuthFault afault = new AuthFault();
        afault.setCode(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        afault.setMessage(exception.getMessage());
        return Response.ok(objectFactory.createAuthFault(afault)).status(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build();
    }
}
