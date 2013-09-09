package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

/*
   Used for testing
 */

//@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForGroups extends JSONReaderForEntity<GroupsList> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        return type == GroupsList.class;
    }

    @Override
    public GroupsList readFrom(Class<GroupsList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put("groups.values", JSONConstants.GROUP);

        return read(inputStream, JSONConstants.GROUPS, prefixValues);
    }
    
}
