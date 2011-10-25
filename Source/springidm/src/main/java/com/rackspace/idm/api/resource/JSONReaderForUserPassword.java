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

import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.idm.JSONConstants;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForUserPassword implements MessageBodyReader<UserPassword> {
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == UserPassword.class;
    }

    @Override
    public UserPassword readFrom(Class<UserPassword> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        UserPassword userPassword = getUserPasswordFromJSONString(jsonBody);

        return userPassword;
    }
    
    public static UserPassword getUserPasswordFromJSONString(String jsonBody) {
        UserPassword userPassword = new UserPassword();
        
        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.PASSWORD)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.PASSWORD).toString());
                
                Object password = obj3.get(JSONConstants.PASSWORD);
                
                if (password != null) {
                    userPassword.setPassword(obj3.get(JSONConstants.PASSWORD).toString());
                }
                

            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return userPassword;
    }
}
