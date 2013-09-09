package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getBaseUrl;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForBaseURLList implements MessageBodyWriter<BaseURLList> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == BaseURLList.class;
    }

    @Override
    public long getSize(BaseURLList baseURLList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(BaseURLList baseURLList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();
        for (BaseURL url : baseURLList.getBaseURL()) {
            list.add(getBaseUrl(url));
        }
        outer.put(JSONConstants.BASE_URLS, list);
        String jsonText = JSONValue.toJSONString(outer);
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
