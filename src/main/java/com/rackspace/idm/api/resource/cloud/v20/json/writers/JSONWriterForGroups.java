package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspacecloud.docs.auth.api.v1.GroupsList;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;

import static com.rackspace.idm.JSONConstants.GROUPS_LIST_PATH;
import static com.rackspace.idm.JSONConstants.VALUES;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForGroups extends JSONWriterForEntity<GroupsList> {

    @Override
    public void writeTo(GroupsList groupsList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(groupsList, entityStream, Collections.singletonMap(GROUPS_LIST_PATH, VALUES), NEVER_PLURALIZE_HANDLER);
    }

}
