package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;
import static com.rackspace.idm.JSONConstants.DOMAIN_ID;
import static com.rackspace.idm.JSONConstants.USER_RAX_AUTH_DOMAIN_ID_PATH;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForUserForCreate extends JSONReaderForEntity<UserForCreate> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                              MediaType mediaType) {
        return type == UserForCreate.class;
    }

    @Override
    public UserForCreate readFrom(Class<UserForCreate> type,
                                  Type genericType, Annotation[] annotations, MediaType mediaType,
                                  MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
            throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(USER_OS_KSADM_PASSWORD_PATH, PASSWORD);
        prefixValues.put(USER_RAX_AUTH_DEFAULT_REGION_PATH, DEFAULT_REGION);
        prefixValues.put(USER_RAX_AUTH_DOMAIN_ID_PATH, DOMAIN_ID);

        return read(inputStream, JSONConstants.USER, prefixValues);
    }
}

