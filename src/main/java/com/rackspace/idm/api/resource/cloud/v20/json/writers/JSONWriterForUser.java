package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.v2.User;

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

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForUser extends JSONWriterForEntity<User> implements MessageBodyWriter<User> {

    @Override
    public void writeTo(User user, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(USER_PASSWORD_PATH, OS_KSADM_PASSWORD);
        prefixValues.put(USER_DEFAULT_REGION_PATH, RAX_AUTH_DEFAULT_REGION);
        prefixValues.put(USER_DOMAIN_ID_PATH, RAX_AUTH_DOMAIN_ID);
        prefixValues.put(USER_MULTI_FACTOR_ENABLED_PATH, RAX_AUTH_MULTI_FACTOR_ENABLED);
        prefixValues.put(USER_MULTI_FACTOR_STATE_PATH, RAX_AUTH_MULTI_FACTOR_STATE);
        prefixValues.put(USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_PATH, RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL);
        prefixValues.put(USER_TOKEN_FORMAT_PATH, RAX_AUTH_TOKEN_FORMAT);
        write(user, entityStream, prefixValues);
    }
}
