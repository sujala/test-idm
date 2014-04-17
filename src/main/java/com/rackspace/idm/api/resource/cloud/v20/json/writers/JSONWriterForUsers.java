package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.identity.api.v2.User;
import org.openstack.docs.identity.api.v2.UserList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getUser;

@Component
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForUsers implements MessageBodyWriter<UserList> {

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == UserList.class;
    }

    @Override
    public long getSize(UserList userList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(UserList userList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();
        for (User user : userList.getUser()) {
            list.add(getUser(user, multiFactorCloud20Service.isMultiFactorGloballyEnabled()));
        }
        outer.put(JSONConstants.USERS, list);
        String jsonText = JSONValue.toJSONString(outer);
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
