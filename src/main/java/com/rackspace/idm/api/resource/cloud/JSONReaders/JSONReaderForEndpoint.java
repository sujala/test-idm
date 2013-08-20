package com.rackspace.idm.api.resource.cloud.JSONReaders;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.Role;

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
public class JSONReaderForEndpoint extends JSONReaderForEntity<Endpoint> implements MessageBodyReader<Endpoint> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == Endpoint.class;
    }

    @Override
    public Endpoint readFrom(Class<Endpoint> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put("endpoint.endpoint_links", JSONConstants.LINK);

        return read(inputStream, JSONConstants.ENDPOINT, prefixValues);
    }
    
}
