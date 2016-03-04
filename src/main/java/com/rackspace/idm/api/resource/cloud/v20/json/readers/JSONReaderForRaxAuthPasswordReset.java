package com.rackspace.idm.api.resource.cloud.v20.json.readers;


import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordReset;
import com.rackspace.idm.JSONConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;


@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthPasswordReset extends JSONReaderForEntity<PasswordReset> {

    @Override
    public PasswordReset readFrom(Class<PasswordReset> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.RAX_AUTH_PASSWORD_RESET, JSONConstants.PASSWORD_RESET);
        return read(entityStream, JSONConstants.RAX_AUTH_PASSWORD_RESET, prefixValues);
    }

}
