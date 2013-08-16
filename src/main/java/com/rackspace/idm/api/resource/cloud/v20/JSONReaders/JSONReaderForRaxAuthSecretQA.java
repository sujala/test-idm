package com.rackspace.idm.api.resource.cloud.v20.JSONReaders;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JSONReaderForEntity;

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
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthSecretQA extends JSONReaderForEntity<SecretQA> implements MessageBodyReader<SecretQA> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == SecretQA.class;
    }

    @Override
    public SecretQA readFrom(Class<SecretQA> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.RAX_AUTH_SECRETQA, JSONConstants.SECRETQA);

        return read(entityStream, JSONConstants.RAX_AUTH_SECRETQA, prefixValues);
    }
}
