package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class JSONReaderForUserWithOnlyKey implements MessageBodyReader<UserWithOnlyKey> {

    private static Logger logger = LoggerFactory.getLogger(JSONReaderForUserWithOnlyKey.class);

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
            logger.info(e.toString());
            throw new BadRequestException("Invalid JSON", e);
        }

        return user;
    }
}
