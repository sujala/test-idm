package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForOsKsAdmServices extends JSONReaderForArrayEntity<ServiceList> {

    JSONReaderForOsKsAdmService jsonReaderForOsKsAdmService = new JSONReaderForOsKsAdmService();

    @Override
    public ServiceList readFrom(Class<ServiceList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String outerObject = JSONConstants.OS_KSADM_SERVICES;
        final Class<ServiceList> entityType = ServiceList.class;

        try {
            String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);
            JSONArray inner = (JSONArray) outer.get(outerObject);

            if (inner == null) {
                throw new BadRequestException("Invalid json");
            } else if (inner.size() <= 0) {
                return entityType.newInstance();
            } else {
                ServiceList serviceList = new ServiceList();
                for (Object object : inner) {
                    if (object instanceof JSONObject) {
                        JSONObject obj = new JSONObject();
                        obj.put(JSONConstants.OS_KSADM_SERVICE, object);
                        Service service = jsonReaderForOsKsAdmService.readFrom(Service.class, null, null, null, null,  IOUtils.toInputStream(obj.toJSONString()));
                        serviceList.getService().add(service);
                    }
                }
                return serviceList;
            }
        } catch (ParseException | IOException | IllegalAccessException | InstantiationException e) {
            throw new BadRequestException("Invalid json");
        }
    }
}
