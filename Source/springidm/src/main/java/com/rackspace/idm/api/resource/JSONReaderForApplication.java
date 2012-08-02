package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.Application;
import com.rackspace.api.idm.v1.ApplicationSecretCredentials;
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
public class JSONReaderForApplication implements MessageBodyReader<Application> {

    private static Logger logger = LoggerFactory.getLogger(JSONReaderForApplication.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == Application.class;
    }

    @Override
    public Application readFrom(Class<Application> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        Application object = getApplicationFromJSONString(jsonBody);

        return object;
    }

    public static Application getApplicationFromJSONString(String jsonBody) {
        Application app = new Application();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.APPLICATION)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.APPLICATION).toString());

                Object clientId = obj3.get(JSONConstants.CLIENT_ID);
                Object customerId = obj3.get(JSONConstants.CUSTOMER_ID);
                Object name = obj3.get(JSONConstants.NAME);
                Object enabled = obj3.get(JSONConstants.ENABLED);
                Object title = obj3.get(JSONConstants.TITLE);
                Object desc = obj3.get(JSONConstants.DESCRIPTION);
                Object callback = obj3.get(JSONConstants.CALL_BACK_URL);
                Object scope = obj3.get(JSONConstants.SCOPE);
                Object creds = obj3.get(JSONConstants.SECRET_CREDENTIALS);

                if (clientId != null) {
                    app.setClientId(clientId.toString());
                }
                if (customerId != null) {
                    app.setCustomerId(customerId.toString());
                }
                if (name != null) {
                    app.setName(name.toString());
                }
                if (enabled != null) {
                    app.setEnabled(Boolean.valueOf(enabled.toString()));
                }
                if (title != null) {
                    app.setTitle(title.toString());
                }
                if (desc != null) {
                    app.setDescription(desc.toString());
                }
                if (callback != null) {
                    app.setCallBackUrl(callback.toString());
                }
                if (scope != null) {
                    app.setScope(scope.toString());
                }
                if (creds != null) {
                    app.setSecretCredentials(getSecretCredentialsFromJSONString(obj3
                        .toString()));
                }

            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Invalid JSON", e);
        }

        return app;
    }

    public static ApplicationSecretCredentials getSecretCredentialsFromJSONString(
        String jsonBody) {
        ApplicationSecretCredentials creds = new ApplicationSecretCredentials();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.APPLICATION_SECRET_CREDENTIALS)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.APPLICATION_SECRET_CREDENTIALS).toString());

                Object secret = obj3.get(JSONConstants.CLIENT_SECRET);

                if (secret != null) {
                    creds.setClientSecret(secret.toString());
                }
            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Bad JSON request", e);
        }

        return creds;
    }
}
