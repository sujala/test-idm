package com.rackspace.idm.api.resource.cloud.v11;

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

import com.rackspace.idm.JSONConstants;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForBaseUrlRef implements MessageBodyReader<BaseURLRef> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == BaseURLRef.class;
    }

    @Override
    public BaseURLRef readFrom(Class<BaseURLRef> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        BaseURLRef object = getBaseURLRefFromJSONString(jsonBody);

        return object;
    }

    public static BaseURLRef getBaseURLRefFromJSONString(String jsonBody) {
        BaseURLRef baseUrlRef = new BaseURLRef();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.BASE_URL_REF)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.BASE_URL_REF).toString());

                Object id = obj3.get(JSONConstants.ID);
                Object v1Default = obj3.get(JSONConstants.V1_DEFAULT);

                if (id != null) {
                    baseUrlRef.setId(Integer.parseInt(id.toString()));
                }
                if (v1Default != null) {
                    baseUrlRef.setV1Default(Boolean.parseBoolean(v1Default
                        .toString()));
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return baseUrlRef;
    }

    public static BaseURLRef getBaseURLRefFromJSONStringWithoutWrapper(
        String jsonBody) {
        BaseURLRef baseUrlRef = new BaseURLRef();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            Object id = outer.get(JSONConstants.ID);
            Object v1Default = outer.get(JSONConstants.V1_DEFAULT);

            if (id != null) {
                baseUrlRef.setId(Integer.parseInt(id.toString()));
            }
            if (v1Default != null) {
                baseUrlRef.setV1Default(Boolean.parseBoolean(v1Default
                    .toString()));
            }

        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return baseUrlRef;
    }
}
