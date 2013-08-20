package com.rackspace.idm.api.resource.cloud.JSONWriters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question;
import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;

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
public class JSONWriterForOsKsAdmService extends JSONWriterForEntity<Service> implements MessageBodyWriter<Service> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Service.class;
    }

    @Override
    public long getSize(Service service, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Service service, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.SERVICE, JSONConstants.OS_KSADM_SERVICE);

        write(service, entityStream, prefixValues);
    }
}
