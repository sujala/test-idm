package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region;
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
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 10/25/12
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRegion implements MessageBodyReader<Region> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Region.class;
    }

    @Override
    public Region readFrom(Class<Region> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);
        Region object = getRegionFromJSONString(jsonBody);
        return object;
    }

    public static Region getRegionFromJSONString(String jsonBody) {
        Region region = new Region();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.RAX_AUTH_REGION)) {
                JSONObject jsonRegion = (JSONObject) parser.parse(outer.get(JSONConstants.RAX_AUTH_REGION).toString());
                Object name = jsonRegion.get(JSONConstants.NAME);
                Object enabled = jsonRegion.get(JSONConstants.ENABLED);
                Object isDefault = jsonRegion.get(JSONConstants.IS_DEFAULT);

                if (name != null) {
                    region.setName(name.toString());
                }

                if (enabled != null) {
                    region.setEnabled(Boolean.valueOf(enabled.toString()));
                }

                if (isDefault != null) {
                    region.setIsDefault(Boolean.valueOf(isDefault.toString()));
                }
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid json request body", e);
        }

        return region;
    }
}
