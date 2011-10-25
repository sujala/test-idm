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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.rackspace.idm.JSONConstants;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForBaseURLRefList implements
    MessageBodyReader<BaseURLRefList> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == BaseURLRefList.class;
    }

    @Override
    public BaseURLRefList readFrom(Class<BaseURLRefList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        BaseURLRefList object = getBaseURLRefFromJSONString(jsonBody);

        return object;
    }

    public static BaseURLRefList getBaseURLRefFromJSONString(String jsonBody) {
        BaseURLRefList refs = new BaseURLRefList();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.BASE_URL_REFS)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.BASE_URL_REFS).toString());

                if (obj3.containsKey(JSONConstants.BASE_URL_REF)) {
                    JSONArray array = (JSONArray) obj3
                        .get(JSONConstants.BASE_URL_REF);

                    for (Object object : array) {
                        if (object != null) {
                            BaseURLRef ref = JSONReaderForBaseUrlRef
                                .getBaseURLRefFromJSONStringWithoutWrapper(object
                                    .toString());
                            refs.getBaseURLRef().add(ref);
                        }
                    }
                }

            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return refs;
    }
}
