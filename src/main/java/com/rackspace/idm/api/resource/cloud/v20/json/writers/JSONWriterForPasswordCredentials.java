package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;

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
public class JSONWriterForPasswordCredentials extends JSONWriterForEntity<PasswordCredentialsBase> {

    @Override
    public void writeTo(PasswordCredentialsBase passwordCredentialsBase, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(passwordCredentialsBase, entityStream);
    }
}
