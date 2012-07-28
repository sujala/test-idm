package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.User;
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
public class JSONReaderForUser implements MessageBodyReader<User> {

    private static final Logger logger = LoggerFactory.getLogger(JSONReaderForUser.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == User.class;
    }

    @Override
    public User readFrom(Class<User> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        User user = getUserFromJSONString(jsonBody);

        return user;
    }

    public static User getUserFromJSONString(String jsonBody) {
        User user = new User();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.USER)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(JSONConstants.USER)
                    .toString());

                Object id = obj3.get(JSONConstants.ID);
                Object username = obj3.get(JSONConstants.USERNAME);
                Object customerId = obj3.get(JSONConstants.CUSTOMER_ID);
                Object email = obj3.get(JSONConstants.EMAIL);
                Object personId = obj3.get(JSONConstants.PERSON_ID);
                Object firstname = obj3.get(JSONConstants.FIRST_NAME);
                Object middlename = obj3.get(JSONConstants.MIDDLE_NAME);
                Object lastname = obj3.get(JSONConstants.LAST_NAME);
                Object displayname = obj3.get(JSONConstants.DISPLAY_NAME);
                Object preflang = obj3.get(JSONConstants.PREF_LANGUAGE);
                Object country = obj3.get(JSONConstants.COUNTRY);
                Object timeZone = obj3.get(JSONConstants.TIME_ZONE);
                Object passwordCredentials = obj3.get(JSONConstants.PASSWORD_CREDENTIALS);
                Object secret = obj3.get(JSONConstants.SECRET);
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
                if (customerId != null) {
                    user.setCustomerId(customerId.toString());
                }
                if (personId != null) {
                    user.setPersonId(personId.toString());
                }
                if (email != null) {
                    user.setEmail(email.toString());
                }
                if (firstname != null) {
                    user.setFirstName(firstname.toString());
                }
                if (middlename != null) {
                    user.setMiddleName(middlename.toString());
                }
                if (lastname != null) {
                    user.setLastName(lastname.toString());
                }
                if (displayname != null) {
                    user.setDisplayName(displayname.toString());
                }
                if (preflang != null) {
                    user.setPrefLanguage(preflang.toString());
                }
                if (country != null) {
                    user.setCountry(country.toString());
                }
                if (timeZone != null) {
                    user.setTimeZone(timeZone.toString());
                }
                if (passwordCredentials != null) {
                    user.setPasswordCredentials(JSONReaderForPasswordCredentials
                        .getUserPasswordCredentialsFromJSONString(obj3
                            .toString()));
                }
                if (secret != null) {
                    user.setSecret(JSONReaderForUserSecret
                        .getUserSecretFromJSONString(obj3.toString()));
                }

            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Bad JSON request");
        }

        return user;
    }
}
