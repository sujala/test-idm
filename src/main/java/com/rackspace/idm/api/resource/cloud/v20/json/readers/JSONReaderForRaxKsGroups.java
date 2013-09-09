package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;

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

import static com.rackspace.idm.JSONConstants.GROUP;
import static com.rackspace.idm.JSONConstants.RAX_KSGRP_GROUPS;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxKsGroups extends JSONReaderForArrayEntity<Groups> {

    @Override
    public Groups readFrom(Class<Groups> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return read(RAX_KSGRP_GROUPS, GROUP , entityStream);
    }
}
