package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ValidatePasswordRequest;
import com.rackspace.idm.JSONConstants;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthValidatePasswordRequest extends JSONWriterForEntity<ValidatePasswordRequest> {

    @Override
    public void writeTo(ValidatePasswordRequest entity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<>();
        prefixValues.put(JSONConstants.VALIDATE_PWD_REQUEST, JSONConstants.RAX_AUTH_VALIDATE_PWD_REQUEST);
        write(entity, entityStream, prefixValues);
    }
}