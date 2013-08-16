package com.rackspace.idm.api.resource.cloud.v20.JSONReaders;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RsaCredentials;
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

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 10/31/12
 * Time: 2:47 PM
 * To change this template use File | Settings | File Templates.
 */

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRSACredentials extends JSONReaderForEntity<RsaCredentials> implements MessageBodyReader<RsaCredentials> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == RsaCredentials.class;
    }

    @Override
    public RsaCredentials readFrom(Class<RsaCredentials> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return read(entityStream, JSONConstants.RAX_AUTH_RSA_CREDENTIALS);
    }
}
