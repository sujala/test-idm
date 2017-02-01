package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.domain.config.IdmProperty;
import com.rackspace.idm.domain.config.IdmPropertyList;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
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
public class JSONReaderForIdmPropertyList implements MessageBodyReader<IdmPropertyList> {


    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(IdmPropertyList.class);
    }

    @Override
    public IdmPropertyList readFrom(Class<IdmPropertyList> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        ObjectMapper mapper = new ObjectMapper();
        IdmPropertyList propertyList = mapper.readValue(entityStream, IdmPropertyList.class);

        return propertyList;
    }

}
