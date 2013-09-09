package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
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

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForServiceCatalog implements MessageBodyWriter<ServiceCatalog> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ServiceCatalog.class;
    }

    @Override
    public long getSize(ServiceCatalog serviceCatalog, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(ServiceCatalog serviceCatalog, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.SERVICECATALOG, getServiceCatalog11(serviceCatalog));
        String jsonText = JSONValue.toJSONString(outer);
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
