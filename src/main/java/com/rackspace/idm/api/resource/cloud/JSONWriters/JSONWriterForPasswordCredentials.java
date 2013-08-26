package com.rackspace.idm.api.resource.cloud.JSONWriters;

import org.openstack.docs.identity.api.v2.Endpoint;
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
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.ENDPOINT_LINKS;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForPasswordCredentials extends JSONWriterForEntity<PasswordCredentialsBase> implements MessageBodyWriter<PasswordCredentialsBase> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == PasswordCredentialsBase.class;
    }

    @Override
    public long getSize(PasswordCredentialsBase passwordCredentialsBase, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(PasswordCredentialsBase passwordCredentialsBase, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(passwordCredentialsBase, entityStream);
    }
}
