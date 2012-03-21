package com.rackspace.idm.api.resource;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
public class JSONReaderForGroup implements MessageBodyReader<Group> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == Group.class;
    }

    @Override
    public Group readFrom(Class<Group> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        Group object = getGroupFromJSONString(jsonBody);

        return object;
    }

    public static Group getGroupFromJSONString(String jsonBody) {
        Group ip = new Group();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.GROUP)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                        JSONConstants.GROUP).toString());

                Object name = obj3.get(JSONConstants.NAME);
                Object desc = obj3.get(JSONConstants.DESCRIPTION);
                Object id = obj3.get(JSONConstants.ID);

                if (name != null) {
                    ip.setName(name.toString());
                }
                if (desc != null) {
                    ip.setDescription(desc.toString());
                }
                if (id != null){
                    ip.setId(desc.toString());
                }
            }
        } catch (ParseException e) {
            throw new BadRequestException("Unable to parse data in request body.");
        }

        return ip;
    }

}
