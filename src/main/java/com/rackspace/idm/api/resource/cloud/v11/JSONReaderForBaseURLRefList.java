package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
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
public class JSONReaderForBaseURLRefList implements
    MessageBodyReader<BaseURLRefList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONReaderForBaseURLRefList.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == BaseURLRefList.class;
    }

    @Override
    public BaseURLRefList readFrom(Class<BaseURLRefList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

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
                JSONArray array = (JSONArray) parser.parse(outer.get(JSONConstants.BASE_URL_REFS).toString());

                for (Object object : array) {
                    if (object != null) {
                        BaseURLRef ref = JSONReaderForBaseUrlRef.getBaseURLRefFromJSONStringWithoutWrapper(object.toString());
                        refs.getBaseURLRef().add(ref);
                    }
                }
            }
        } catch (ParseException e) {
            LOGGER.info(e.toString());
            throw new BadRequestException("Bad JSON request", e);
        }

        return refs;
    }
}
