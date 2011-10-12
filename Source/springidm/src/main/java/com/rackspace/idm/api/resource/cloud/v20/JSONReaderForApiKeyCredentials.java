package com.rackspace.idm.api.resource.cloud.v20;

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

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForApiKeyCredentials implements
    MessageBodyReader<ApiKeyCredentials> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == ApiKeyCredentials.class;
    }

    @Override
    public ApiKeyCredentials readFrom(Class<ApiKeyCredentials> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, "UTF-8");

        ApiKeyCredentials creds = new ApiKeyCredentials();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey("RAX-KSKEY:apiKeyCredentials")) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    "RAX-KSKEY:apiKeyCredentials").toString());
                Object username = obj3.get("username");
                Object apikey = obj3.get("apiKey");

                if (username != null) {
                    creds.setUsername(username.toString());
                }
                if (apikey != null) {
                    creds.setApiKey(apikey.toString());
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return creds;
    }
}
