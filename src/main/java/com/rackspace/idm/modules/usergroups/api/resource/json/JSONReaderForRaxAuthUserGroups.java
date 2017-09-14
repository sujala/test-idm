package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroups;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForEntity;
import com.rackspace.idm.modules.usergroups.Constants;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Implementation of this class is for testing purpose only.
 */

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthUserGroups extends JSONReaderForEntity<UserGroups> {

    @Override
    public UserGroups readFrom(Class<UserGroups> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(Constants.RAX_AUTH_USER_GROUPS, Constants.USER_GROUPS);
        return read(entityStream, Constants.RAX_AUTH_USER_GROUPS, prefixValues, true);
    }
}
