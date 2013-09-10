package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspacecloud.docs.auth.api.v1.AuthData;
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

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getServiceCatalog11;
import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getToken11;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForAuthData implements MessageBodyWriter<AuthData> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == AuthData.class;
    }

    @Override
    public long getSize(AuthData authData, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(AuthData authData, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();

        if (authData.getServiceCatalog() != null) {
            inner.put(JSONConstants.SERVICECATALOG, getServiceCatalog11(authData.getServiceCatalog()));
        }
        inner.put(JSONConstants.TOKEN, getToken11(authData.getToken()));
        outer.put(JSONConstants.AUTH, inner);
        String jsonText = JSONValue.toJSONString(outer);
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
