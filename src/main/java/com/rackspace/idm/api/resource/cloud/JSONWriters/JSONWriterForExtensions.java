package com.rackspace.idm.api.resource.cloud.JSONWriters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions;
import com.rackspace.idm.api.resource.cloud.JSONWriterForArrayEntity;
import org.openstack.docs.common.api.v1.Extensions;

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

import static com.rackspace.idm.JSONConstants.EXTENSIONS;
import static com.rackspace.idm.JSONConstants.RAX_AUTH_REGIONS;
import static com.rackspace.idm.JSONConstants.REGIONS;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForExtensions extends JSONWriterForArrayEntity<Extensions> implements MessageBodyWriter<Extensions> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Extensions.class;
    }

    @Override
    public long getSize(Extensions extensions, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(Extensions extensions, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(extensions, EXTENSIONS, EXTENSIONS, entityStream);
    }
}
