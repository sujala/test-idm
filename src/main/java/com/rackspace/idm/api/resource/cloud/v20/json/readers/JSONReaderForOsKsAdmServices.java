package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForOsKsAdmServices extends JSONReaderForArrayEntity<ServiceList> {

    @Override
    public ServiceList readFrom(Class<ServiceList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        return read(OS_KSADM_SERVICES, SERVICE, inputStream);
    }
}
