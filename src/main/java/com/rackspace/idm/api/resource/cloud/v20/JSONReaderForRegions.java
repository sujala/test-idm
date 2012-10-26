package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
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
public class JSONReaderForRegions implements MessageBodyReader<Regions> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Regions.class;
    }

    @Override
    public Regions readFrom(Class<Regions> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);
        Regions object = getRegionsFromJSONString(jsonBody);
        return object;
    }

    public static Regions getRegionsFromJSONString(String jsonBody) {
        Regions regions = new Regions();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.RAX_AUTH_REGIONS)) {
                JSONArray array = (JSONArray) parser.parse(outer.get(JSONConstants.RAX_AUTH_REGIONS).toString());

                for (Object object : array) {
                    if (object != null) {
                        Region region = new Region();

                        JSONObject jsonRegion = (JSONObject) object;
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

                        regions.getRegion().add(region);
                    }
                }
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid json request body", e);
        }

        return regions;
    }
}
