package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonWriterHelper;
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

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthPolicies implements MessageBodyWriter<Policies> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Policies.class;
    }

    @Override
    public long getSize(Policies policies, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Policies policies, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.RAX_AUTH_POLICIES, JsonWriterHelper.getPoliciesWithoutWrapper(policies));
        String jsonText = JSONValue.toJSONString(outer);
        entityStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
