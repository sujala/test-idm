package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.PasswordRotationPolicy;
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
public class JSONReaderForPasswordRotationPolicy implements MessageBodyReader<PasswordRotationPolicy> {
    private static Logger logger = LoggerFactory.getLogger(JSONReaderForPasswordRotationPolicy.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == PasswordRotationPolicy.class;
    }

    @Override
    public PasswordRotationPolicy readFrom(Class<PasswordRotationPolicy> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        PasswordRotationPolicy object = getPasswordRotationPolicyFromJSONString(jsonBody);

        return object;
    }

    public static PasswordRotationPolicy getPasswordRotationPolicyFromJSONString(String jsonBody) {
        PasswordRotationPolicy ip = new PasswordRotationPolicy();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.PASSWORD_ROTATION_POLICY)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.PASSWORD_ROTATION_POLICY).toString());

                Object duration = obj3.get(JSONConstants.DURATION);
                Object enabled = obj3.get(JSONConstants.ENABLED);

                if (duration != null) {
                    ip.setDuration(Integer.parseInt(duration.toString()));
                }
                if (enabled != null) {
                    ip.setEnabled(Boolean.valueOf(enabled.toString()));
                }
            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Invalid JSON", e);
        }

        return ip;
    }
}

