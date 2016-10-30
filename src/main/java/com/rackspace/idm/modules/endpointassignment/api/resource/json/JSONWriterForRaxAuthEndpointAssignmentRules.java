package com.rackspace.idm.modules.endpointassignment.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EndpointAssignmentRules;
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
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthEndpointAssignmentRules extends JSONWriterForEntity<EndpointAssignmentRules> {

    @Override
    public void writeTo(EndpointAssignmentRules endpointAssignmentRules, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(Constants.ENDPOINT_ASSIGNMENT_RULES, Constants.RAX_AUTH_ENDPOINT_ASSIGNMENT_RULES);
        prefixValues.put(Constants.RAX_AUTH_EP_ASSIGNMENT_RULES_TT_RULE_ENDPOINT_TEMPLATES_PATH, JSONConstants.OS_KSCATALOG_ENDPOINT_TEMPLATES);
        prefixValues.put(Constants.RAX_AUTH_EP_ASSIGNMENT_RULES_TT_RULE_ENDPOINT_TEMPLATE_PATH, JSONConstants.OS_KSCATALOG_ENDPOINT_TEMPLATE);

        write(endpointAssignmentRules, entityStream, prefixValues, ALWAYS_PLURALIZE_HANDLER);
    }
}