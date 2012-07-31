package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.UserPasswordCredentials;
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
public class JSONReaderForPasswordCredentials implements
        MessageBodyReader<UserPasswordCredentials> {

    private static Logger logger = LoggerFactory.getLogger(JSONReaderForPasswordCredentials.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mediaType) {
        return type == UserPasswordCredentials.class;
    }

    @Override
    public UserPasswordCredentials readFrom(
            Class<UserPasswordCredentials> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
            throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        UserPasswordCredentials creds = getUserPasswordCredentialsFromJSONString(jsonBody);

        return creds;
    }

    public static UserPasswordCredentials getUserPasswordCredentialsFromJSONString(
            String jsonBody) {
        UserPasswordCredentials creds = new UserPasswordCredentials();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.PASSWORD_CREDENTIALS)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                        JSONConstants.PASSWORD_CREDENTIALS).toString());

                Object newPassword = obj3.get(JSONConstants.NEW_PASSWORD);
                Object currentPassword = obj3.get(JSONConstants.CURRENT_PASSWORD);
                Object verifyCurrentPassword = obj3.get(JSONConstants.VERIFY_CURRENT_PASSWORD);


                if (newPassword != null) {
                    creds
                            .setNewPassword(JSONReaderForUserPassword
                                    .getUserPasswordFromJSONStringWithoutWrapper(newPassword
                                            .toString()));
                }
                if (currentPassword != null) {
                    creds
                            .setCurrentPassword(JSONReaderForUserPassword
                                    .getUserPasswordFromJSONStringWithoutWrapper(currentPassword
                                            .toString()));
                }
                if (verifyCurrentPassword != null) {
                    if (verifyCurrentPassword.equals("false"))
                        creds.setVerifyCurrentPassword(false);
                    else {
                        creds.setVerifyCurrentPassword(true);
                    }
                }
            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Invalid JSON", e);
        }

        return creds;
    }
}
