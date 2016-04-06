package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.v2.Role;

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

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRole extends JSONReaderForEntity<Role> {

    @Override
    public Role readFrom(Class<Role> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(ROLE_RAX_AUTH_ADMINISTRATOR_ROLE_PATH, ADMINISTRATOR_ROLE);
        prefixValues.put(ROLE_RAX_AUTH_WEIGHT_PATH, WEIGHT);
        prefixValues.put(ROLE_RAX_AUTH_PROPAGATE_PATH, PROPAGATE);

        return read(inputStream, ROLE, prefixValues);
    }
    
}
