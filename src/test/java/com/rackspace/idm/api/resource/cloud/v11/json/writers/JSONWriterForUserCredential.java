package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForEntity;
import com.rackspacecloud.docs.auth.api.v1.UserCredentials;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;


/**
 * Adding this "test" writer to make JSON Auth 1.1 authenticate call via integration tests. Not required for production.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForUserCredential extends JSONWriterForEntity<UserCredentials> {
    @Override
    public void writeTo(UserCredentials userCredentials, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        write(userCredentials, entityStream, prefixValues);
    }
}

