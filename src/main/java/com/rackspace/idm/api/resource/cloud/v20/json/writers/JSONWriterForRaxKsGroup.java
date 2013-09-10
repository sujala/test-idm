package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;

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
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.GROUP;
import static com.rackspace.idm.JSONConstants.RAX_KSGRP_GROUP;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxKsGroup extends JSONWriterForEntity<Group> {

    @Override
    public void writeTo(Group group, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(GROUP, RAX_KSGRP_GROUP);

        write(group, entityStream, prefixValues);
    }
}
