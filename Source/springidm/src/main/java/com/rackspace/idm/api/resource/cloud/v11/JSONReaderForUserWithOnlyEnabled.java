package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
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
public class JSONReaderForUserWithOnlyEnabled implements MessageBodyReader<UserWithOnlyEnabled> {

    private static Logger logger = LoggerFactory.getLogger(JSONReaderForUserWithOnlyEnabled.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == UserWithOnlyEnabled.class;
    }

    @Override
    public UserWithOnlyEnabled readFrom(Class<UserWithOnlyEnabled> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        UserWithOnlyEnabled object = getUserWithOnlyEnabledFromJSONString(jsonBody);

        return object;
    }

    public static UserWithOnlyEnabled getUserWithOnlyEnabledFromJSONString(String jsonBody) {
        UserWithOnlyEnabled user = new UserWithOnlyEnabled();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.USER)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(JSONConstants.USER)
                    .toString());

                Object id = obj3.get(JSONConstants.ID);
                Object enabled = obj3.get(JSONConstants.ENABLED);

                if (id != null) {
                    user.setId(id.toString());
                }
                if (enabled != null) {
                    user.setEnabled(Boolean.valueOf(enabled.toString()));
                }
            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Invalid JSON", e);
        }

        return user;
    }
}
