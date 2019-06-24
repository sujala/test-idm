package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region;
import com.rackspace.idm.JSONConstants;
import org.apache.commons.io.IOUtils;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;

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
public class JSONReaderForRaxAuthImpersonationResponse extends JSONReaderForEntity<ImpersonationResponse> {

    @Override
    public ImpersonationResponse readFrom(Class<ImpersonationResponse> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        ImpersonationResponse response = new ImpersonationResponse();
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);

        Token token = JSONReaderForCloudAuthenticationResponseToken.getAuthenticationResponseTokenFromJSONString(jsonBody);
        response.setToken(token);
        return response;
    }
}
