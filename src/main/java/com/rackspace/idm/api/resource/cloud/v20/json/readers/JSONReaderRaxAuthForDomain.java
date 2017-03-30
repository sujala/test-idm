package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderRaxAuthForDomain implements MessageBodyReader<Domain> {

    private static final String INVALID_JSON_ERROR_MESSAGE = "Invalid json request body";

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Domain.class;
    }

    @Override
    public Domain readFrom(Class<Domain> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);
        Domain object = getDomainFromJSONString(jsonBody);
        return object;
    }

    public static Domain getDomainFromJSONString(String jsonBody) {
        Domain domain = new Domain();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.RAX_AUTH_DOMAIN)) {
                JSONObject jsonDomain = (JSONObject) parser.parse(outer.get(JSONConstants.RAX_AUTH_DOMAIN).toString());
                domain = getDomainFromInnerJSONString(jsonDomain);
            }
        } catch (Exception e) {
            throw new BadRequestException(INVALID_JSON_ERROR_MESSAGE, e);
        }

        return domain;
    }

    public static Domain getDomainFromInnerJSONString(JSONObject jsonDomain) {
        Domain domain = new Domain();
        try {
            Object id = jsonDomain.get(JSONConstants.ID);
            Object enabled = jsonDomain.get(JSONConstants.ENABLED);
            Object description = jsonDomain.get(JSONConstants.DESCRIPTION);
            Object name = jsonDomain.get(JSONConstants.NAME);
            Object sessionInactivityTimeout = jsonDomain.get(JSONConstants.SESSION_INACTIVITY_TIMEOUT);

            if (id != null) {
                domain.setId(id.toString());
            }
            if (enabled != null) {
                domain.setEnabled(Boolean.valueOf(enabled.toString()));
            }
            if (name != null) {
                domain.setName(name.toString());
            }
            if (description != null) {
                domain.setDescription(description.toString());
            }
            if (sessionInactivityTimeout != null) {
                Duration duration;
                try {
                    duration = DatatypeFactory.newInstance().newDuration(sessionInactivityTimeout.toString());
                } catch (DatatypeConfigurationException e) {
                    throw new BadRequestException(INVALID_JSON_ERROR_MESSAGE, e);
                }
                domain.setSessionInactivityTimeout(duration);
            }
        } catch (Exception e) {
            throw new BadRequestException(INVALID_JSON_ERROR_MESSAGE, e);
        }
        return domain;
    }
}
