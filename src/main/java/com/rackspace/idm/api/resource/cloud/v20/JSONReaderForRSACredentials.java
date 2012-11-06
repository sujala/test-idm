package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RsaCredentials;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 10/31/12
 * Time: 2:47 PM
 * To change this template use File | Settings | File Templates.
 */

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRSACredentials implements MessageBodyReader<RsaCredentials> {

    private static Logger logger = LoggerFactory.getLogger(JSONReaderForPasswordCredentials.class);


    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == RsaCredentials.class;
    }

    @Override
    public RsaCredentials readFrom(Class<RsaCredentials> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);
        RsaCredentials creds = getRSACredentialsFromJSONString(jsonBody);
        return creds;
    }

    public static RsaCredentials getRSACredentialsFromJSONString(String jsonBody) {
        RsaCredentials creds = new RsaCredentials();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);
            if (outer.containsKey(JSONConstants.RAX_AUTH_RSA)) {
                JSONObject jsonObject = (JSONObject) parser.parse(outer.get(JSONConstants.RAX_AUTH_RSA).toString());
                creds = getRSACredentialsFromInnerJSONObject(jsonObject);
            }
        } catch (Exception e) {
            logger.info(e.toString());
            throw new BadRequestException("JSON Parsing error", e);
        }
        return creds;
    }

    public static RsaCredentials getRSACredentialsFromInnerJSONObject(JSONObject jsonBody) {
        RsaCredentials creds = new RsaCredentials();

        try {
            Object username = jsonBody.get(JSONConstants.USERNAME);
            Object tokenkey = jsonBody.get(JSONConstants.TOKEN_KEY);

            if (username != null) {
                creds.setUsername(username.toString().trim());
            }

            if (tokenkey != null) {
                creds.setTokenKey(tokenkey.toString());
            }
        } catch (Exception e) {
            logger.info(e.toString());
            throw new BadRequestException("JSON Parsing error", e);
        }

        return creds;
    }
}
