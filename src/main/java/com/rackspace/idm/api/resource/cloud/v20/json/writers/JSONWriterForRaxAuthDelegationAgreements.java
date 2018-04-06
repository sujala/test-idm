package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreements;
import com.rackspace.idm.JSONConstants;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthDelegationAgreements extends JSONWriterForArrayEntity<DelegationAgreements> {

    @Override
    public void writeTo(DelegationAgreements delegationAgreements, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(delegationAgreements, JSONConstants.DELEGATION_AGREEMENTS, JSONConstants.RAX_AUTH_DELEGATION_AGREEMENTS, entityStream);
    }
}