package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.EndpointList;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForEndpoints extends JSONReaderForArrayEntity<EndpointList> {

    JSONReaderForEndpoint jsonReaderForEndpoint = new JSONReaderForEndpoint();

    @Override
    public EndpointList readFrom(Class<EndpointList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String outerObject = JSONConstants.ENDPOINTS;
        EndpointList endpointList = new EndpointList();

        try {
            String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);
            JSONArray inner = (JSONArray) outer.get(outerObject);

            if (inner == null) {
                throw new BadRequestException("Invalid json");
            } else if (inner.size() <= 0) {
                return endpointList;
            } else {
                for (Object object : inner) {
                    if (object instanceof JSONObject) {
                        JSONObject obj = new JSONObject();
                        obj.put(JSONConstants.ENDPOINT, object);
                        Endpoint endpoint = jsonReaderForEndpoint.readFrom(Endpoint.class, null, null, null, null,  IOUtils.toInputStream(obj.toJSONString()));
                        endpointList.getEndpoint().add(endpoint);
                    }
                }
                return endpointList;
            }
        } catch (ParseException | IOException e) {
            throw new BadRequestException("Invalid json");
        }
    }
}
