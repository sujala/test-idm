package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ServiceApis;
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
public class JSONWriterForServiceApis extends JSONWriterForArrayEntity<ServiceApis> implements MessageBodyWriter<ServiceApis> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ServiceApis.class;
    }

    @Override
    public long getSize(ServiceApis serviceApis, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(ServiceApis serviceApis, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(serviceApis, JSONConstants.SERVICE_APIS, JSONConstants.RAX_AUTH_SERVICE_APIS, entityStream);
    }
}
