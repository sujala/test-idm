package com.rackspace.idm.modules.endpointassignment.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForEntity;
import com.rackspace.idm.modules.endpointassignment.Constants;

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
public class JSONWriterForRaxAuthTenantTypeEndpointRule extends JSONWriterForEntity<TenantTypeEndpointRule> {

    @Override
    public void writeTo(TenantTypeEndpointRule tenantTypeEndpointRule, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        final Map<String, String> prefixValues = new LinkedHashMap<String, String>();

        /* Order here is critical. Prefixes are added in order so changing the top level object's name requires subsequent
         prefixes to use the modified name */
        prefixValues.put(Constants.TENANT_TYPE_ENDPOINT_RULE, Constants.RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE);
        prefixValues.put(Constants.RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE_ENDPOINT_TEMPLATES_PATH, JSONConstants.OS_KSCATALOG_ENDPOINT_TEMPLATES);
        prefixValues.put(Constants.RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE_ENDPOINT_TEMPLATE_PATH, JSONConstants.OS_KSCATALOG_ENDPOINT_TEMPLATE);
        write(tenantTypeEndpointRule, entityStream, prefixValues, ALWAYS_PLURALIZE_HANDLER);
    }
}
