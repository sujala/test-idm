package com.rackspace.idm.api.resource.cloud.JSONReaders;

import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.JSONConstants.ENDPOINT_TEMPLATE;
import static com.rackspace.idm.JSONConstants.OS_KSCATALOG_ENDPOINT_TEMPLATES;

/*
    Test use only
 */
//@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForOsKsCatalogEndpointTemplates extends JSONReaderForArrayEntity<EndpointTemplateList> implements MessageBodyReader<EndpointTemplateList> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == EndpointTemplateList.class;
    }

    @Override
    public EndpointTemplateList readFrom(Class<EndpointTemplateList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        return read(OS_KSCATALOG_ENDPOINT_TEMPLATES, ENDPOINT_TEMPLATE, inputStream);
    }
}
