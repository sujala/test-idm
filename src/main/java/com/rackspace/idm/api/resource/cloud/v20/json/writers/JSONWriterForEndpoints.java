package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.EndpointList;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.JSONConstants.*;
import static com.rackspace.idm.JSONConstants.VERSION;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForEndpoints implements MessageBodyWriter<EndpointList> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == EndpointList.class;
    }

    @Override
    public long getSize(EndpointList endpointList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(EndpointList endpointList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(endpointList, JSONConstants.ENDPOINTS, JSONConstants.ENDPOINTS, entityStream);
    }

    protected void write(EndpointList entity, String oldName, String newName, OutputStream entityStream) {
        OutputStream outputStream = new ByteArrayOutputStream();
        try {
            getMarshaller().marshallToJSON(entity, outputStream);
            String jsonString = outputStream.toString();

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonString);
            JSONObject middle = (JSONObject) outer.get(oldName);

            JSONObject newOuter = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            if (middle != null) {
                Object[] keys = middle.keySet().toArray();
                String key = (String)keys[0];

                for (Object jsonObject : (JSONArray)middle.get(key)) {
                    JSONObject version  = (JSONObject) ((JSONObject)jsonObject).get(VERSION);

                    if(version != null){
                        Object id = version.get(ID);
                        if(id != null){
                            ((JSONObject)jsonObject).put(VERSION_ID, id);
                        }
                        Object info = version.get(INFO);
                        if(info != null){
                            ((JSONObject)jsonObject).put(VERSION_INFO, info);
                        }
                        Object list = version.get(LIST);
                        if(list != null){
                            ((JSONObject)jsonObject).put(VERSION_LIST, list);
                        }
                        ((JSONObject)jsonObject).remove(VERSION);
                    }
                    jsonArray.add(jsonObject);
                }
            }
            newOuter.put(newName, jsonArray);

            String newJsonString = newOuter.toString();
            entityStream.write(newJsonString.getBytes(JSONConstants.UTF_8));

        } catch (JAXBException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        } catch (ParseException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        } catch (UnsupportedEncodingException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        } catch (IOException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        }
    }

    JSONMarshaller getMarshaller() throws JAXBException {
        return ((JSONJAXBContext) JAXBContextResolver.get()).createJSONMarshaller();
    }
}
