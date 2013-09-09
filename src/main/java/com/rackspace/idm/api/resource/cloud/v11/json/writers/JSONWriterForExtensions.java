package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import com.rackspace.idm.JSONConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;

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

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.*;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForExtensions implements MessageBodyWriter<Extensions> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Extensions.class;
    }

    @Override
    public long getSize(Extensions extensions, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Extensions extensions, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        String jsonText = JSONValue.toJSONString(getExtensionList(extensions));
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }

    JSONObject getExtensionList(Extensions extensions) {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();
        outer.put(JSONConstants.EXTENSIONS, list);
        for (Extension extension : extensions.getExtension()) {
            list.add(getExtensionWithoutWrapper(extension));
        }
        return outer;
    }
}
