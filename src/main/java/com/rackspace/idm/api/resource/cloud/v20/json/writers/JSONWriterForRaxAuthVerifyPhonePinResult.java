package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerifyPhonePinResult;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForEntity;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthVerifyPhonePinResult extends JSONWriterForEntity<VerifyPhonePinResult> {

    @Override
    public void writeTo(VerifyPhonePinResult verifyPhonePinResult, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        final Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.VERIFY_PHONE_PIN_RESULT, JSONConstants.RAX_AUTH_VERIFY_PHONE_PIN_RESULT);
        write(verifyPhonePinResult, entityStream, prefixValues, ALWAYS_PLURALIZE_HANDLER);
    }
}
