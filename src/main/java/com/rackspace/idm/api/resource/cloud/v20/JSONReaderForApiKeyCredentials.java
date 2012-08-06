package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

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
public class JSONReaderForApiKeyCredentials implements
    MessageBodyReader<ApiKeyCredentials> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONReaderForApiKeyCredentials.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == ApiKeyCredentials.class;
    }

    @Override
    public ApiKeyCredentials readFrom(Class<ApiKeyCredentials> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        ApiKeyCredentials creds = getApiKeyCredentialsFromJSONString(jsonBody);

        return creds;
    }
    
    public static ApiKeyCredentials getApiKeyCredentialsFromJSONString(String jsonBody) {
        ApiKeyCredentials creds = new ApiKeyCredentials();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.APIKEY_CREDENTIALS)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.APIKEY_CREDENTIALS).toString());
                Object username = obj3.get(JSONConstants.USERNAME);
                Object apikey = obj3.get(JSONConstants.API_KEY);

                if (username != null) {
                    creds.setUsername(username.toString());
                }
                if (apikey != null) {
                    creds.setApiKey(apikey.toString());
                }
            }
        } catch (ParseException e) {
            LOGGER.info(e.toString());
            throw new BadRequestException("Invalid JSON", e);
        }

        return creds;
    }
    
    public static ApiKeyCredentials checkAndGetApiKeyCredentialsFromJSONString(String jsonBody) {
        ApiKeyCredentials creds = getApiKeyCredentialsFromJSONString(jsonBody);
        if (StringUtils.isBlank(creds.getApiKey())) {
            throw new BadRequestException("Expecting apiKey");
        }
        if (StringUtils.isBlank(creds.getUsername())) {
            throw new BadRequestException("Expecting username");
        }
        return creds;
    }
}
