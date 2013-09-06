package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import org.openstack.docs.identity.api.v2.Tenant;

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

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForTenant extends JSONWriterForEntity<Tenant> implements MessageBodyWriter<Tenant> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Tenant.class;
    }

    @Override
    public long getSize(Tenant tenant, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Tenant tenant, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(tenant, entityStream);
    }
}
