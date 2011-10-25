package com.rackspace.idm.api.resource.cloud.v11;

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

import com.rackspace.idm.JSONConstants;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForUserWithOnlyKey implements MessageBodyReader<UserWithOnlyKey> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == UserWithOnlyKey.class;
    }

    @Override
    public UserWithOnlyKey readFrom(Class<UserWithOnlyKey> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        UserWithOnlyKey object = getUserWithOnlyKeyFromJSONString(jsonBody);

        return object;
    }

    public static UserWithOnlyKey getUserWithOnlyKeyFromJSONString(String jsonBody) {
        UserWithOnlyKey user = new UserWithOnlyKey();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.USER)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(JSONConstants.USER)
                    .toString());

                Object id = obj3.get(JSONConstants.ID);
                Object key = obj3.get(JSONConstants.KEY);

                if (id != null) {
                    user.setId(id.toString());
                }
                if (key != null) {
                    user.setKey(key.toString());
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return user;
    }
}
