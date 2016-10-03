package com.rackspace.idm.modules.endpointassignment.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForEntity;
import com.rackspace.idm.modules.endpointassignment.Constants;

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
public class JSONReaderForRaxAuthTenantTypeEndpointRule extends JSONReaderForEntity<TenantTypeEndpointRule> {
    @Override
    public TenantTypeEndpointRule readFrom(Class<TenantTypeEndpointRule> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        final Map<String, String> prefixValues = new LinkedHashMap<String, String>();

        /* Order here is critical. Prefixes are added in order so changing the top level object's name requires subsequent
         prefixes to use the modified name */
        prefixValues.put(Constants.RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE_OSK_ENDPOINT_TEMPLATES_PATH, JSONConstants.ENDPOINT_TEMPLATES);
        prefixValues.put(Constants.RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE, Constants.TENANT_TYPE_ENDPOINT_RULE);
        return read(entityStream, Constants.RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE, prefixValues, ALWAYS_PLURALIZE_HANDLER);
    }
}
