package com.rackspace.idm.api.resource.cloud.v20;

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
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/10/12
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForDomain implements MessageBodyReader<Domain> {
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

            if (outer.containsKey(JSONConstants.DOMAIN)) {
                JSONObject jsonDomain = (JSONObject) parser.parse(outer.get(JSONConstants.DOMAIN).toString());
                Object id = jsonDomain.get(JSONConstants.ID);
                Object enabled = jsonDomain.get(JSONConstants.ENABLED);
                Object description = jsonDomain.get(JSONConstants.DESCRIPTION);
                Object name = jsonDomain.get(JSONConstants.NAME);

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
                if (name != null) {
                    domain.setName(name.toString());
                }
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid json request body", e);
        }

        return domain;
    }
}
