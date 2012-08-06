package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.UserSecret;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
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
public class JSONReaderForUserSecret implements MessageBodyReader<UserSecret> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSecret.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == UserSecret.class;
    }

    @Override
    public UserSecret readFrom(Class<UserSecret> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        UserSecret object = getUserSecretFromJSONString(jsonBody);

        return object;
    }
    
    public static UserSecret getUserSecretFromJSONString(String jsonBody) {
        UserSecret userSecret = new UserSecret();
        
        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.SECRET)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.SECRET).toString());
                
                Object question = obj3.get(JSONConstants.SECRET_QUESTION);
                Object answer = obj3.get(JSONConstants.SECRET_ANSWER);
                
                if (question != null) {
                    userSecret.setSecretQuestion(question.toString());
                }
                if (answer != null) {
                    userSecret.setSecretAnswer(answer.toString());
                }

            }
        } catch (ParseException e) {
            LOGGER.info(e.toString());
            throw new BadRequestException("Bad JSON request", e);
        }
        
        return userSecret;
    }
}
