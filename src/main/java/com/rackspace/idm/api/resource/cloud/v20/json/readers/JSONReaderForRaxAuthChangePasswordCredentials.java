package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ChangePasswordCredentials;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ForgotPasswordCredentials;
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
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthChangePasswordCredentials extends JSONReaderForEntity<ChangePasswordCredentials> {

    @Override
    public ChangePasswordCredentials readFrom(Class<ChangePasswordCredentials> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.RAX_AUTH_CHANGE_PWD_CREDENTIALS, JSONConstants.CHANGE_PWD_CREDENTIALS);
        return read(entityStream, JSONConstants.RAX_AUTH_CHANGE_PWD_CREDENTIALS, prefixValues);
    }
}
