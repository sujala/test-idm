package com.rackspace.idm.api.resource.cloud.JSONWriters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions;
import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;

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
public class JSONWriterForOsKsCatalogEndpointTemplates extends JSONWriterForArrayEntity<EndpointTemplateList> implements MessageBodyWriter<EndpointTemplateList> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == EndpointTemplateList.class;
    }

    @Override
    public long getSize(EndpointTemplateList endpointTemplateList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(EndpointTemplateList endpointTemplateList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(endpointTemplateList, JSONConstants.ENDPOINT_TEMPLATES, JSONConstants.OS_KSCATALOG_ENDPOINT_TEMPLATES, entityStream);
    }
}
