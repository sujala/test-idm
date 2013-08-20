package com.rackspace.idm.api.resource.cloud.JSONReaders;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JSONReaderForEntity;
import org.openstack.docs.identity.api.v2.User;

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
public class JSONReaderForUser extends JSONReaderForEntity<User> implements MessageBodyReader<User> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == User.class;
    }

    @Override
    public User readFrom(Class<User> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        return read(inputStream, JSONConstants.USER);
    }
}
