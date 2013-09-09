package com.rackspace.idm.api.resource.cloud.v11.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;
import com.rackspacecloud.docs.auth.api.v1.User;
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

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getBaseUrls;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForUser implements MessageBodyWriter<User> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == User.class;
    }

    @Override
    public long getSize(User user, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(User user, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        inner.put(JSONConstants.ID, user.getId());
        inner.put(JSONConstants.ENABLED, user.isEnabled());
        if (user.getKey() != null) {
            inner.put(JSONConstants.KEY, user.getKey());
        }
        if (user.getMossoId() != null) {
            inner.put(JSONConstants.MOSSO_ID, user.getMossoId());
        }
        if (user.getNastId() != null) {
            inner.put(JSONConstants.NAST_ID, user.getNastId());
        }
        if (user.getCreated() != null) {
            inner.put(JSONConstants.CREATED, String.valueOf(user.getCreated()));
        }
        if (user.getUpdated() != null) {
            inner.put(JSONConstants.UPDATED, String.valueOf(user.getUpdated()));
        }
        BaseURLRefList baseList = user.getBaseURLRefs();
        JSONArray baseUrls = getBaseUrls(baseList);
        inner.put(JSONConstants.BASE_URL_REFS, baseUrls);
        outer.put(JSONConstants.USER, inner);
        String jsonText = JSONValue.toJSONString(outer);
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
