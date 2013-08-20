package com.rackspace.idm.api.resource.cloud.JSONWriters;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;

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

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForOsKsCatalogEndpointTemplate extends JSONWriterForEntity<EndpointTemplate> implements MessageBodyWriter<EndpointTemplate> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == EndpointTemplate.class;
    }

    @Override
    public long getSize(EndpointTemplate endpointTemplate, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(EndpointTemplate endpointTemplate, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.ENDPOINT_TEMPLATE, JSONConstants.OS_KSCATALOG_ENDPOINT_TEMPLATE);

        write(endpointTemplate, entityStream, prefixValues);
    }
}
