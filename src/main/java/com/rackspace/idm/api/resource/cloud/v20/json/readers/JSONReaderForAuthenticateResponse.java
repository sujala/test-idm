package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import org.apache.commons.io.IOUtils;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;

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
public class JSONReaderForAuthenticateResponse extends JSONReaderForEntity<AuthenticateResponse> {

    @Override
    public AuthenticateResponse readFrom(Class<AuthenticateResponse> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);
        return JSONReaderForCloudAuthenticationResponse.getAuthenticationResponseFromJSONString(jsonBody);
    }
}
