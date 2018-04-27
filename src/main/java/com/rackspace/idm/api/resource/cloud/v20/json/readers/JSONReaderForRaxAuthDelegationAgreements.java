package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreements;
import com.rackspace.idm.JSONConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthDelegationAgreements extends JSONReaderForArrayEntity<DelegationAgreements> {

    @Override
    public DelegationAgreements readFrom(Class<DelegationAgreements> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return read(JSONConstants.RAX_AUTH_DELEGATION_AGREEMENTS, JSONConstants.DELEGATION_AGREEMENT, entityStream);
    }
}