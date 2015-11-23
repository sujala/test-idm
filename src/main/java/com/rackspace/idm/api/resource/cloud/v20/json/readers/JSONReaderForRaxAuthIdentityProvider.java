package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonArrayTransformerHandler;

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
public class JSONReaderForRaxAuthIdentityProvider extends JSONReaderForEntity<IdentityProvider> {

    @Override
    public IdentityProvider readFrom(Class<IdentityProvider> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        final Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.RAX_AUTH_IDENTITY_PROVIDER, JSONConstants.IDENTITY_PROVIDER);
        return read(entityStream, JSONConstants.RAX_AUTH_IDENTITY_PROVIDER, prefixValues, ALWAYS_PLURALIZE_HANDLER);
    }

}
