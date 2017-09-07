package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForEntity;
import com.rackspace.idm.modules.usergroups.Constants;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthUserGroup extends JSONWriterForEntity<UserGroup> {

    @Override
    public void writeTo(UserGroup userGroup, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        final Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(Constants.USER_GROUP, Constants.RAX_AUTH_USER_GROUP);
        write(userGroup, entityStream, prefixValues, ALWAYS_PLURALIZE_HANDLER);
    }
}
