package com.rackspace.idm.api.resource.cloud.JSONReaders;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JSONReaderForEntity;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForService extends JSONReaderForEntity<Service> implements MessageBodyReader<Service> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == Service.class;
    }

    @Override
    public Service readFrom(Class<Service> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.OS_KSADM_SERVICE ,JSONConstants.SERVICE);

        return read(inputStream, JSONConstants.OS_KSADM_SERVICE, prefixValues);
    }
    
}
