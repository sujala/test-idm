package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.JSONConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getApiKeyCredentials;
import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getPasswordCredentials;
import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getRole;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRoles implements MessageBodyWriter<RoleList> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == RoleList.class;
    }

    @Override
    public long getSize(RoleList roleList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(RoleList roleList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();
        for (Role role : roleList.getRole()) {
            list.add(getRole(role));
        }
        outer.put(JSONConstants.ROLES, list);
        String jsonText = JSONValue.toJSONString(outer);
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
