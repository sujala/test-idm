package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Role;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRole extends JSONReaderForEntity<Role> {

    @Override
    public Role readFrom(Class<Role> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(ROLE_RAX_AUTH_ADMINISTRATOR_ROLE_PATH, ADMINISTRATOR_ROLE);
        prefixValues.put(ROLE_RAX_AUTH_WEIGHT_PATH, WEIGHT);
        prefixValues.put(ROLE_RAX_AUTH_PROPAGATE_PATH, PROPAGATE);
        prefixValues.put(ROLE_RAX_AUTH_ASSIGNMENT_PATH, ASSIGNMENT);

        String json = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(json);
            JSONObject inner = (JSONObject) outer.get(JSONConstants.ROLE);

            if(inner.containsKey(JSONConstants.RAX_AUTH_ASSIGNMENT)) {
                String assignmentValue = (String)inner.get(JSONConstants.RAX_AUTH_ASSIGNMENT);

                if (StringUtils.isBlank(assignmentValue)) {
                    inner.remove(JSONConstants.RAX_AUTH_ASSIGNMENT);
                }
            }

            json = outer.toJSONString();

        } catch (ParseException e) {
            throw new BadRequestException("Invalid json request body");
        }

        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(json.getBytes());
        return read(arrayInputStream, JSONConstants.ROLE, prefixValues);
    }
    
}
