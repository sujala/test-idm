package com.rackspace.idm.api.resource.cloud.JSONReaders;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;

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

import static com.rackspace.idm.JSONConstants.*;

/*
    Used only for testing - Not verified
 */
//@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxKsGroup extends JSONReaderForEntity<Group> implements MessageBodyReader<Group> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Group.class;
    }

    @Override
    public Group readFrom(Class<Group> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(RAX_KSGRP_GROUP, GROUP);
        return read(entityStream, RAX_KSGRP_GROUP, prefixValues);
    }
}
