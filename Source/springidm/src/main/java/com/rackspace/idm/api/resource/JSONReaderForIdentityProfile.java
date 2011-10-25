package com.rackspace.idm.api.resource;

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

import com.rackspace.api.idm.v1.IdentityProfile;
import com.rackspace.idm.JSONConstants;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForIdentityProfile implements MessageBodyReader<IdentityProfile> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == IdentityProfile.class;
    }

    @Override
    public IdentityProfile readFrom(Class<IdentityProfile> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        IdentityProfile object = getIdentityProfileFromJSONString(jsonBody);

        return object;
    }

    public static IdentityProfile getIdentityProfileFromJSONString(String jsonBody) {
        IdentityProfile ip = new IdentityProfile();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.CUSTOMER_IDENTITY_PROFILE)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.CUSTOMER_IDENTITY_PROFILE).toString());

                Object id = obj3.get(JSONConstants.ID);
                Object customerId = obj3.get(JSONConstants.CUSTOMER_ID);
                Object enabled = obj3.get(JSONConstants.ENABLED);

                if (id != null) {
                    ip.setId(id.toString());
                }
                if (customerId != null) {
                    ip.setCustomerId(customerId.toString());
                }
                if (enabled != null) {
                    ip.setEnabled(Boolean.valueOf(enabled.toString()));
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ip;
    }
}
