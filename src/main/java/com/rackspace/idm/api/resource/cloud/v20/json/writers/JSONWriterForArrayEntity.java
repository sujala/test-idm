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

import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public abstract class JSONWriterForArrayEntity<T> implements MessageBodyWriter<T> {

    protected void write(T entity, String oldName, String newName, OutputStream entityStream) {
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
