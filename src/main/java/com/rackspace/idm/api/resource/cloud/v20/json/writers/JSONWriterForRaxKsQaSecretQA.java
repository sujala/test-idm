package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;

public class JSONWriterForRaxKsQaSecretQA extends JSONWriterForEntity<SecretQA>  {
    @Override
    public void writeTo(SecretQA secretQA, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(SECRET_QA, RAX_KSQA_SECRET_QA);

        write(secretQA, entityStream, prefixValues);
    }
}
