package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.JSONConstants;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.User;

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

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForUser implements MessageBodyReader<User> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == User.class;
    }

    @Override
    public User readFrom(Class<User> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        User object = getUserFromJSONString(jsonBody);

        return object;
    }
    
    public static User getUserFromJSONString(String jsonBody) {
        User user = new User();
        
        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.USER)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.USER).toString());
                
                Object id = obj3.get(JSONConstants.ID);
                Object username = obj3.get(JSONConstants.USERNAME);
                Object email = obj3.get(JSONConstants.EMAIL);
                Object displayname = obj3.get(JSONConstants.DISPLAY_NAME_CLOUD);
                Object enabled = obj3.get(JSONConstants.ENABLED);
                
                
                if (id != null) {
                    user.setId(id.toString());
                }
                if (enabled != null) {
                    user.setEnabled(Boolean.valueOf(enabled.toString()));
                }
                if (username != null) {
                    user.setUsername(username.toString());
                }
                if (email != null) {
                    user.setEmail(email.toString());
                }
                if (displayname != null) {
                    user.setDisplayName(displayname.toString());
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return user;
    }
}
