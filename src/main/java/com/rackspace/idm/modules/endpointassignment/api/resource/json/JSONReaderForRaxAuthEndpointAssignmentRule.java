package com.rackspace.idm.modules.endpointassignment.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EndpointAssignmentRule;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForEntity;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.modules.endpointassignment.Constants;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Unmarshalls various EndpointAssignmentRules from JSON format
 *
 * TODO: Make this more adaptable to additional rules without adding if/else statements
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthEndpointAssignmentRule extends JSONReaderForEntity<EndpointAssignmentRule> {

    JSONReaderForRaxAuthTenantTypeEndpointRule tenantTypeReader = new JSONReaderForRaxAuthTenantTypeEndpointRule();

    @Override
    public EndpointAssignmentRule readFrom(Class<EndpointAssignmentRule> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        EndpointAssignmentRule  endpointAssignmentRule = null;
        try {
            String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer == null || outer.keySet().size() < 1) {
                throw new BadRequestException("Invalid json request body");
            }

            String rootElement = outer.keySet().iterator().next().toString();

            if(rootElement.equals(Constants.RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE)){
                endpointAssignmentRule = tenantTypeReader.readFrom(TenantTypeEndpointRule.class, TenantTypeEndpointRule.class, annotations, mediaType, httpHeaders, new ByteArrayInputStream(jsonBody.getBytes("UTF8")));
            } else {
                throw new BadRequestException("Invalid json request body");
            }
        } catch (ParseException e) {
            throw new BadRequestException("Invalid json request body", e);
        }
        return endpointAssignmentRule;
    }
}
