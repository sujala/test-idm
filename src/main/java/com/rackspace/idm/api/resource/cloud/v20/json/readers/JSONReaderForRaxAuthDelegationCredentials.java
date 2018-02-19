package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationCredentials;
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
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthDelegationCredentials extends JSONReaderForEntity<DelegationCredentials> {

    @Override
    public DelegationCredentials readFrom(Class<DelegationCredentials> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.RAX_AUTH_DELEGATION_CREDENTIALS, JSONConstants.DELEGATION_CREDENTIALS);
        return read(entityStream, JSONConstants.RAX_AUTH_DELEGATION_CREDENTIALS, prefixValues);
    }
}
