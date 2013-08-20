package com.rackspace.idm.api.resource.cloud.JSONWriters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions;
import com.rackspace.idm.api.resource.cloud.JSONWriterForArrayEntity;

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

import static com.rackspace.idm.JSONConstants.*;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthRegions extends JSONWriterForArrayEntity<Regions> implements MessageBodyWriter<Regions> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Regions.class;
    }

    @Override
    public long getSize(Regions regions, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(Regions regions, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(regions, REGIONS, RAX_AUTH_REGIONS, entityStream);
    }
}
