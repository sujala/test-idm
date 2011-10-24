package com.rackspace.idm.api.resource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.rackspace.api.idm.v1.Role;
import com.rackspace.idm.JSONConstants;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRole implements MessageBodyReader<Role> {
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == Role.class;
    }

    @Override
    public Role readFrom(Class<Role> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        Role object = getRoleFromJSONString(jsonBody);

        return object;
    }

    public static Role getRoleFromJSONString(String jsonBody) {
        Role ip = new Role();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.PASSWORD_ROTATION_POLICY)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.PASSWORD_ROTATION_POLICY).toString());

                Object id = obj3.get(JSONConstants.ID);
                Object name = obj3.get(JSONConstants.NAME);
                Object tenantId = obj3.get(JSONConstants.TENANT_ID);
                Object desc = obj3.get(JSONConstants.DESCRIPTION);
                Object appId = obj3.get(JSONConstants.APPLICATION_ID);

                if (id != null) {
                    ip.setId(id.toString());
                }
                if (name != null) {
                    ip.setName(name.toString());
                }
                if (tenantId != null) {
                    ip.setTenantId(tenantId.toString());
                }
                if (desc != null) {
                    ip.setDescription(desc.toString());
                }
                if (appId != null) {
                    ip.setApplicationId(appId.toString());
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ip;
    }

}
