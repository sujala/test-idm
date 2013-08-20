package com.rackspace.idm.api.resource.cloud.JSONReaders;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JSONReaderForArrayEntity;
import com.rackspace.idm.api.resource.cloud.JSONReaderForEntity;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;

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

import static com.rackspace.idm.JSONConstants.EXTENSIONS;

/**
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 10/25/12
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForExtension extends JSONReaderForEntity<Extension> implements MessageBodyReader<Extension> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Extension.class;
    }

    @Override
    public Extension readFrom(Class<Extension> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return read(entityStream, JSONConstants.EXTENSION);
    }
}
