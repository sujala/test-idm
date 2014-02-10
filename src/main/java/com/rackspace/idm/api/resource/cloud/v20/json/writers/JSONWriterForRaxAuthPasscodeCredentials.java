package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasscodeCredentials;
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
public class JSONWriterForRaxAuthPasscodeCredentials extends JSONWriterForEntity<PasscodeCredentials> {

    @Override
    public void writeTo(PasscodeCredentials credential, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.PASSCODE_CREDENTIALS, JSONConstants.RAX_AUTH_PASSCODE_CREDENTIALS);

        write(credential, entityStream, prefixValues);
    }
}
