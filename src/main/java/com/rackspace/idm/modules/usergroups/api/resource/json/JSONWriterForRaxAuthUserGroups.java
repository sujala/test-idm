package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroups;
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForArrayEntity;
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

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthUserGroups extends JSONWriterForArrayEntity<UserGroups> {

    @Override
    public void writeTo(UserGroups userGroups, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(userGroups, Constants.USER_GROUPS, Constants.RAX_AUTH_USER_GROUPS, entityStream);
    }
}
