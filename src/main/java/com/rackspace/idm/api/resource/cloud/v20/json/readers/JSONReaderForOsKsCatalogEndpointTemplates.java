package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
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
public class JSONReaderForOsKsCatalogEndpointTemplates extends JSONReaderForArrayEntity<EndpointTemplateList> {

    @Override
    public EndpointTemplateList readFrom(Class<EndpointTemplateList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        return read(OS_KSCATALOG_ENDPOINT_TEMPLATES, ENDPOINT_TEMPLATE, inputStream);
    }
}
