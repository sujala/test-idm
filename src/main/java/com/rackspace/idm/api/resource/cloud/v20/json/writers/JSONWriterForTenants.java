package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.v2.Tenants;

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
public class JSONWriterForTenants extends JSONWriterForArrayEntity<Tenants> implements MessageBodyWriter<Tenants> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Tenants.class;
    }

    @Override
    public long getSize(Tenants tenants, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Tenants tenants, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(tenants, JSONConstants.TENANTS, JSONConstants.TENANTS, entityStream);
    }
}
