package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
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
public class JSONReaderForDefaultRegionServices implements MessageBodyReader<DefaultRegionServices> {

    private static final Logger logger = LoggerFactory.getLogger(JSONReaderForRole.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == DefaultRegionServices.class;
    }

    @Override
    public DefaultRegionServices readFrom(Class<DefaultRegionServices> type, Type genericType, Annotation[] annotations,
                                          MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                          InputStream inputStream) throws IOException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        DefaultRegionServices object = getDefaultRegionServicesFromJSONString(jsonBody);

        return object;
    }

    public static DefaultRegionServices getDefaultRegionServicesFromJSONString(String jsonBody) {
        DefaultRegionServices defaultRegionServices = new DefaultRegionServices();
        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.RAX_AUTH_DEFAULT_REGION_SERVICES)) {
                JSONArray serviceArray;

                serviceArray = (JSONArray) parser.parse(outer.get(JSONConstants.RAX_AUTH_DEFAULT_REGION_SERVICES).toString());
                for(Object service : serviceArray){
                    defaultRegionServices.getServiceName().add(service.toString());
                }

            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Bad JSON request", e);
        }

        return defaultRegionServices;
    }
}
