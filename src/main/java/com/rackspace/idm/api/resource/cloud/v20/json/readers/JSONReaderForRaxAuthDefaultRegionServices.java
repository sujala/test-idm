package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.idm.JSONConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthDefaultRegionServices extends JSONReaderForArrayEntity<DefaultRegionServices> implements MessageBodyReader<DefaultRegionServices> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == DefaultRegionServices.class;
    }

    @Override
    public DefaultRegionServices readFrom(Class<DefaultRegionServices> type, Type genericType, Annotation[] annotations,
                                          MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                          InputStream inputStream) throws IOException {
        return read(JSONConstants.RAX_AUTH_DEFAULT_REGION_SERVICES, JSONConstants.SERVICE_NAME, inputStream);
    }
}
