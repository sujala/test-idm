package com.rackspace.idm.modules.endpointassignment.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EndpointAssignmentRules;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDevices;
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
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthEndpointAssignmentRules extends JSONReaderForEntity<EndpointAssignmentRules> {

    @Override
    public EndpointAssignmentRules readFrom(Class<EndpointAssignmentRules> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(Constants.RAX_AUTH_ENDPOINT_ASSIGNMENT_RULES, Constants.ENDPOINT_ASSIGNMENT_RULES);
        prefixValues.put(Constants.TENANT_TYPE_ENDPOINT_RULE, Constants.TENANT_TYPE_ENDPOINT_RULES);
        return read(entityStream, Constants.RAX_AUTH_ENDPOINT_ASSIGNMENT_RULES, prefixValues, true);
    }
}