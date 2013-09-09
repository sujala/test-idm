package com.rackspace.idm.api.resource.cloud.v20.json.writers;

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

import static com.rackspace.idm.JSONConstants.*;
import static com.rackspace.idm.JSONConstants.RAX_AUTH_DOMAIN_ID;
import static com.rackspace.idm.JSONConstants.USER_DOMAIN_ID_PATH;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForUserForCreate extends JSONWriterForEntity<UserForCreate> implements MessageBodyWriter<UserForCreate> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == UserForCreate.class;
    }

    @Override
    public long getSize(UserForCreate user, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(UserForCreate user, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(USER_PASSWORD_PATH, OS_KSADM_PASSWORD);
        prefixValues.put(USER_DEFAULT_REGION_PATH, RAX_AUTH_DEFAULT_REGION);
        prefixValues.put(USER_DOMAIN_ID_PATH, RAX_AUTH_DOMAIN_ID);

        write(user, entityStream, prefixValues);
    }
}
