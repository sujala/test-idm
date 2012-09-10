package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
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
public class JSONReaderForPolicy implements MessageBodyReader<Policy> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Policy.class;
    }

    @Override
    public Policy readFrom(Class<Policy> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);
        Policy object = getPolicyFromJSONString(jsonBody);
        return object;
    }

    public static Policy getPolicyFromJSONString(String jsonBody) {
        Policy policy = new Policy();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.POLICY)) {
                JSONObject jsonPolicy = (JSONObject) parser.parse(outer.get(JSONConstants.POLICY).toString());
                Object id = jsonPolicy.get(JSONConstants.ID);
                Object enabled = jsonPolicy.get(JSONConstants.ENABLED);
                Object description = jsonPolicy.get(JSONConstants.DESCRIPTION);
                Object name = jsonPolicy.get(JSONConstants.NAME);
                Object blob = jsonPolicy.get(JSONConstants.BLOB);
                Object global = jsonPolicy.get(JSONConstants.GLOBAL);
                Object type = jsonPolicy.get(JSONConstants.TYPE);

                if (id != null) {
                    policy.setId(id.toString());
                }
                if (blob != null) {
                    policy.setBlob(blob.toString());
                }
                if (enabled != null) {
                    policy.setEnabled(Boolean.valueOf(enabled.toString()));
                }
                if (description != null) {
                    policy.setDescription(description.toString());
                }
                if (name != null) {
                    policy.setName(name.toString());
                }
                if (global != null) {
                    policy.setGlobal(Boolean.valueOf(global.toString()));
                }
                if (type != null) {
                    policy.setType(type.toString());
                }
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid json request body", e);
        }

        return policy;
    }
}
