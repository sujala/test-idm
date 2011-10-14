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
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForService implements MessageBodyReader<Service> {
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

        String jsonBody = IOUtils.toString(inputStream, "UTF-8");

        Service service = new Service();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey("OS-KSADM:service")) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get("OS-KSADM:service")
                    .toString());

                Object desc = obj3.get("description");
                Object id = obj3.get("id");
                Object serviceType = obj3.get("type");

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return service;
    }
}
