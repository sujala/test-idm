package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

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
        MessageBodyReader<PasswordCredentialsRequiredUsername> {

    private static Logger logger = LoggerFactory.getLogger(JSONReaderForPasswordCredentials.class);


    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return type == PasswordCredentialsRequiredUsername.class;
    }

    @Override
    public PasswordCredentialsRequiredUsername readFrom(Class<PasswordCredentialsRequiredUsername> passwordCredentialsRequiredUsernameClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> stringStringMultivaluedMap, InputStream inputStream) throws IOException, WebApplicationException {
        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        PasswordCredentialsRequiredUsername creds = getPasswordCredentialsFromJSONString(jsonBody);

        return creds;
    }

    public static PasswordCredentialsRequiredUsername getPasswordCredentialsFromJSONString(String jsonBody) {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.PASSWORD_CREDENTIALS)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.PASSWORD_CREDENTIALS).toString());
                Object username = obj3.get(JSONConstants.USERNAME);
                Object password = obj3.get(JSONConstants.PASSWORD);

                if (username != null) {
                    creds.setUsername(username.toString());
                }

                if (password != null) {
                    creds.setPassword(password.toString());
                }
            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("JSON Parsing error", e);
        }

        return creds;
    }
    
    public static PasswordCredentialsRequiredUsername checkAndGetPasswordCredentialsFromJSONString(String jsonBody) {
        PasswordCredentialsRequiredUsername creds = getPasswordCredentialsFromJSONString(jsonBody);
        
        if (StringUtils.isBlank(creds.getUsername())) {
            throw new BadRequestException("Expecting username");
        }
        if (StringUtils.isBlank(creds.getPassword())) {
            throw new BadRequestException("Expecting password");
        }
        
        return creds;
    }
}
