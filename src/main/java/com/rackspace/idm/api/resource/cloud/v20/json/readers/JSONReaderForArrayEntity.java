package com.rackspace.idm.api.resource.cloud.v20.json.readers;


import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONUnmarshaller;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class JSONReaderForArrayEntity<T> implements MessageBodyReader<T> {

    final private Class<T> entityType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == entityType;
    }

    protected T read(String outerObject, String innerObject, InputStream entityStream) {
        try {
            String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);
            JSONArray inner = (JSONArray) outer.get(outerObject);

            JSONObject newOuter = new JSONObject();
            JSONArray middle = new JSONArray();

            if (inner == null) {
                throw new BadRequestException("Invalid json request body");
            } else if (inner.size() <= 0) {
                return entityType.newInstance();

            } else {
                for (Object object : inner) {
                    middle.add(object);
                }
            }

            newOuter.put(innerObject,middle);

            String jsonString = newOuter.toString();
            InputStream inputStream = IOUtils.toInputStream(jsonString);
            return getMarshaller().unmarshalFromJSON(inputStream, entityType);

        } catch (ParseException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (JAXBException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (IOException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (InstantiationException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (IllegalAccessException e) {
            throw new BadRequestException("Invalid json request body");
        }
    }

    JSONUnmarshaller getMarshaller() throws JAXBException {
        //Todo: move to its own class
        JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(true).build();
        JSONJAXBContext context = new JSONJAXBContext(
                jsonConfiguration,
                "com.rackspacecloud.docs.auth.api.v1:" +
                "org.openstack.docs.common.api.v1:" +
                "org.openstack.docs.compute.api.v1:" +
                "org.openstack.docs.identity.api.v2:" +
                "com.rackspace.docs.identity.api.ext.rax_ksgrp.v1:" +
                "com.rackspace.docs.identity.api.ext.rax_kskey.v1:" +
                "org.openstack.docs.identity.api.ext.os_ksadm.v1:" +
                "org.openstack.docs.identity.api.ext.os_kscatalog.v1:" +
                "org.openstack.docs.identity.api.ext.os_ksec2.v1:" +
                "org.w3._2005.atom:" +
                "com.rackspace.docs.identity.api.ext.rax_ksqa.v1:" +
                "com.rackspace.api.common.fault.v1:" +
                "com.rackspace.docs.identity.api.ext.rax_auth.v1");

        return context.createJSONUnmarshaller();
    }
}
