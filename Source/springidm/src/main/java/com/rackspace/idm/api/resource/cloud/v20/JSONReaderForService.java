package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
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
public class JSONReaderForService implements MessageBodyReader<Service> {
    private static Logger logger = LoggerFactory.getLogger(JSONReaderForService.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == Service.class;
    }

    @Override
    public Service readFrom(Class<Service> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        Service service = getServiceFromJSONString(jsonBody);

        return service;
    }
    
    public static Service getServiceFromJSONString(String jsonBody) {
        Service service = new Service();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.SERVICE)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(JSONConstants.SERVICE)
                    .toString());

                Object desc = obj3.get(JSONConstants.DESCRIPTION);
                Object id = obj3.get(JSONConstants.ID);
                Object name = obj3.get(JSONConstants.NAME);
                Object serviceType = obj3.get(JSONConstants.TYPE);

                if (name != null) {
                    service.setName(name.toString());
                }
                if (desc != null) {
                    service.setDescription(desc.toString());
                }
                if (id != null) {
                    service.setId(id.toString());
                }
                if (serviceType != null) {
                    service.setType(serviceType.toString());
                }
            }
        } catch (ParseException e) {
            logger.info(e.toString());
            throw new BadRequestException("Invalid JSON", e);
        }

        return service;
    }
}
