package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PolicyAlgorithm;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by IntelliJ IDEA.
 * User: jorge.munoz
 * Date: 9/08/12
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForPolicies implements MessageBodyReader<Policies> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Policies.class;
    }

    @Override
    public Policies readFrom(Class<Policies> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);
        Policies object = getPoliciesFromJSONString(jsonBody);
        return object;
    }

    public static Policies getPoliciesFromJSONString(String jsonBody) {
        Policies policies = new Policies();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);
            if (outer.containsKey(JSONConstants.POLICIES)) {
                JSONObject jsonPolicies = (JSONObject) parser.parse(outer.get(JSONConstants.POLICIES).toString());
                if (jsonPolicies.containsKey(JSONConstants.POLICY)) {
                    JSONArray policyArray = (JSONArray) parser.parse(jsonPolicies.get(JSONConstants.POLICY).toString());
                    for (int i = 0; i < policyArray.size(); i++) {
                        JSONObject id = (JSONObject)policyArray.get(i);
                        Policy policy = new Policy();
                        policy.setId(id.get(JSONConstants.ID).toString());
                        policies.getPolicy().add(policy);
                    }
                }

                if(jsonPolicies.containsKey(JSONConstants.POLICIES_ALGORITHM)){
                String algorithm = jsonPolicies.get(JSONConstants.POLICIES_ALGORITHM).toString();
                if(algorithm.equalsIgnoreCase(PolicyAlgorithm.IF_FALSE_DENY.value())){
                    policies.setAlgorithm(PolicyAlgorithm.IF_FALSE_DENY);
                }else if (algorithm.equalsIgnoreCase(PolicyAlgorithm.IF_TRUE_ALLOW.value())){
                    policies.setAlgorithm(PolicyAlgorithm.IF_TRUE_ALLOW);
                }

            }
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid json request body", e);
        }

        return policies;
    }
}
