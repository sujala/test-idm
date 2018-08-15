package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;

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
public class JSONReaderForUserForCreate extends JSONReaderForEntity<UserForCreate> {

    @Override
    public UserForCreate readFrom(Class<UserForCreate> type,
                                  Type genericType, Annotation[] annotations, MediaType mediaType,
                                  MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
            throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(USER_OS_KSADM_PASSWORD_PATH, PASSWORD);
        prefixValues.put(USER_RAX_AUTH_DEFAULT_REGION_PATH, DEFAULT_REGION);
        prefixValues.put(USER_RAX_AUTH_DOMAIN_ID_PATH, DOMAIN_ID);
        prefixValues.put(USER_RAX_KSQA_SECRET_QA_PATH, SECRET_QA);
        prefixValues.put(USER_RAX_KSGRP_GROUPS_PATH, GROUPS);
        prefixValues.put(USER_RAX_AUTH_TOKEN_FORMAT_PATH, TOKEN_FORMAT);
        prefixValues.put(USER_RAX_AUTH_CONTACT_ID_PATH, CONTACT_ID);
        prefixValues.put(USER_RAX_AUTH_REGISTRATION_CODE_PATH, REGISTRATION_CODE);
        prefixValues.put(USER_RAX_AUTH_MULTI_FACTOR_ENABLED_PATH, MULTI_FACTOR_ENABLED);
        prefixValues.put(USER_RAX_AUTH_MULTI_FACTOR_STATE_PATH, MULTI_FACTOR_STATE);
        prefixValues.put(USER_RAX_AUTH_MULTI_FACTOR_TYPE_PATH, MULTI_FACTOR_TYPE);
        prefixValues.put(USER_RAX_AUTH_MULTI_FACTOR_ENFORCEMENT_LEVEL_PATH, RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL);

        return read(inputStream, JSONConstants.USER, prefixValues);
    }
}

