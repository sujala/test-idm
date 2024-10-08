package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs;
import com.rackspace.idm.JSONConstants;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthSecretQAs extends JSONWriterForArrayEntity<SecretQAs> {

    @Override
    public void writeTo(SecretQAs secretQAs, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(secretQAs, JSONConstants.SECRETQAS, JSONConstants.RAX_AUTH_SECRETQAS, entityStream);
    }
}
