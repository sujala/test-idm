package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
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
public class JSONReaderForUserPassword implements
    MessageBodyReader<UserPassword> {

    private static final Logger logger = LoggerFactory.getLogger(JSONReaderForUserPassword.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == UserPassword.class;
    }

    @Override
    public UserPassword readFrom(Class<UserPassword> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
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

            if (outer.containsKey(JSONConstants.USER_PASSWORD)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(JSONConstants.USER_PASSWORD).toString());

                Object password = obj3.get(JSONConstants.PASSWORD);

                if (password != null) {
                    userPassword.setPassword(obj3.get(JSONConstants.PASSWORD).toString());
                }

            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Bad JSON request");
        }

        return userPassword;
    }

    public static UserPassword getUserPasswordFromJSONStringWithoutWrapper(
        String jsonBody) {
        UserPassword userPassword = new UserPassword();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            Object password = outer.get(JSONConstants.PASSWORD);

            if (password != null) {
                userPassword.setPassword(password.toString());
            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Bad JSON request");
        }

        return userPassword;
    }
}
