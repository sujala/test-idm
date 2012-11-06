package com.rackspace.idm.api.resource.cloud.v20;

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

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class JSONWriterForArrayEntity<T> {

    void write(T entity, String oldName, String newName, OutputStream entityStream) {
        OutputStream outputStream = new ByteArrayOutputStream();
        try {
            getMarshaller().marshallToJSON(entity, outputStream);
            String jsonString = outputStream.toString();

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonString);
            JSONObject middle = (JSONObject) outer.get(oldName);
            JSONArray jsonArray = new JSONArray();

            Object[] keys = middle.keySet().toArray();
            String key = (String)keys[0];

            for (Object jsonObject : (JSONArray)middle.get(key)) {
                jsonArray.add(jsonObject);
            }

            JSONObject newOuter = new JSONObject();
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
