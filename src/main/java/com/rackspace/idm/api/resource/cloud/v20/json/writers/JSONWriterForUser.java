package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.api.resource.cloud.UserJsonAttributeNamesTransformHandler;
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
        prefixValues.put(USER_MULTI_FACTOR_TYPE_PATH, RAX_AUTH_MULTI_FACTOR_TYPE);
        prefixValues.put(USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_PATH, RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL);
        prefixValues.put(USER_TOKEN_FORMAT_PATH, RAX_AUTH_TOKEN_FORMAT);
        prefixValues.put(USER_CONTACT_ID_PATH, RAX_AUTH_CONTACT_ID);
        prefixValues.put(USER_GROUPS_PATH, RAX_KSGRP_GROUPS);
        prefixValues.put(USER_SECRET_QA_PATH, RAX_KSQA_SECRET_QA);
        prefixValues.put(USER_PASSWORD_EXPIRATION_PATH, RAX_AUTH_PASSWORD_EXPIRATION);
        prefixValues.put(DELEGATION_AGREEMENT_ID_PATH, RAX_AUTH_DELEGATION_AGREEMENT_ID);
        prefixValues.put(UNVERIFIED_PATH, RAX_AUTH_UNVERIFIED_ID);

        write(user, entityStream, prefixValues, new UserJsonAttributeNamesTransformHandler());
    }
}
