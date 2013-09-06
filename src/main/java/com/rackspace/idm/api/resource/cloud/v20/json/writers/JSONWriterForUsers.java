package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import org.openstack.docs.identity.api.v2.User;
import org.openstack.docs.identity.api.v2.UserList;

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

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getRole;
import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getUser;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForUsers implements MessageBodyWriter<UserList> {
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
            list.add(getUser(user));
        }
        outer.put(JSONConstants.USERS, list);
        String jsonText = JSONValue.toJSONString(outer);
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
