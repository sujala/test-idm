package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxKsGroups extends JSONWriterForArrayEntity<Groups> implements MessageBodyWriter<Groups> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Groups.class;
    }

    @Override
    public long getSize(Groups groups, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Groups groups, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(groups, GROUPS, RAX_KSGRP_GROUPS, entityStream);
    }
}
