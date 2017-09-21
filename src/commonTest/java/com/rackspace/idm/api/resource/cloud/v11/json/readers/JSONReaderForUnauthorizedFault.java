package com.rackspace.idm.api.resource.cloud.v11.json.readers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForEntity;
import com.rackspacecloud.docs.auth.api.v1.ForbiddenFault;
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForUnauthorizedFault extends JSONReaderForEntity<UnauthorizedFault> {
    @Override
    public UnauthorizedFault readFrom(Class<UnauthorizedFault> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return read(entityStream, JSONConstants.OPENSTACK_UNAUTHORIZED_FAULT);
    }
}
