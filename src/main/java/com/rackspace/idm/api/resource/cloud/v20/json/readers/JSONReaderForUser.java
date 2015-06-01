package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import org.openstack.docs.identity.api.v2.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForUser extends JSONReaderForEntity<User> {

    @Override
    public User readFrom(Class<User> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(USER_OS_KSADM_PASSWORD_PATH, PASSWORD);
        prefixValues.put(USER_RAX_AUTH_DEFAULT_REGION_PATH, DEFAULT_REGION);
        prefixValues.put(USER_RAX_AUTH_DOMAIN_ID_PATH, DOMAIN_ID);
        prefixValues.put(USER_RAX_AUTH_MULTI_FACTOR_ENABLED_PATH, MULTI_FACTOR_ENABLED);
        prefixValues.put(USER_RAX_AUTH_MULTI_FACTOR_STATE_PATH, MULTI_FACTOR_STATE);
        prefixValues.put(USER_RAX_AUTH_TOKEN_FORMAT_PATH, TOKEN_FORMAT);
        prefixValues.put(USER_RAX_AUTH_MULTI_FACTOR_TYPE_PATH, MULTI_FACTOR_TYPE);

        return read(inputStream, USER, prefixValues);
    }
}
