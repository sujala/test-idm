package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;

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
public class JSONReaderForPasswordCredentials extends JSONReaderForEntity<PasswordCredentialsBase> implements
        MessageBodyReader<PasswordCredentialsBase> {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return type == PasswordCredentialsBase.class || (PasswordCredentialsBase.class).isAssignableFrom((Class) type);
    }

    @Override
    public PasswordCredentialsBase readFrom(Class<PasswordCredentialsBase> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return read(entityStream, JSONConstants.PASSWORD_CREDENTIALS);
    }
}
