package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForEntity;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;

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

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForPasswordCredentials extends JSONWriterForEntity<PasswordCredentials> {

    @Override
    public void writeTo(PasswordCredentials passwordCredentials, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new HashMap<>();
        write(passwordCredentials, entityStream, prefixValues);
    }

}
