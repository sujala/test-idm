package com.rackspace.idm.api.resource.cloud.JSONWriters;

import com.rackspace.idm.api.resource.cloud.JSONWriterForEntity;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;

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
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForUserForCreate extends JSONWriterForEntity<UserForCreate> implements MessageBodyWriter<UserForCreate> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == UserForCreate.class;
    }

    @Override
    public long getSize(UserForCreate user, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(UserForCreate user, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put("user.password","OS-KSADM:password");
        prefixValues.put("user.defaultRegion", "RAX-AUTH:defaultRegion");
        prefixValues.put("user.domainId", "RAX-AUTH:domainId");

        write(user, entityStream, prefixValues);
    }
}
