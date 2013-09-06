package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonWriterHelper;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.common.api.v1.Extension;

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

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForExtension implements MessageBodyWriter<Extension> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Extension.class;
    }

    @Override
    public long getSize(Extension extension, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Extension extension, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        String jsonText = JSONValue.toJSONString(getExtension(extension));
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }

    private JSONObject getExtension(Extension extension) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.EXTENSION, getExtensionWithoutWrapper(extension));
        return outer;
    }


}
