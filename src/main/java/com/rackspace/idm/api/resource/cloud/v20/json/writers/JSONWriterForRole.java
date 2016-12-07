package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import org.openstack.docs.identity.api.v2.Role;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRole extends JSONWriterForEntity<Role> {

    @Override
    public void writeTo(Role role, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(ROLE_ADMINISTRATOR_ROLE_PATH, RAX_AUTH_ADMINISTRATOR_ROLE);
        prefixValues.put(ROLE_WEIGHT_PATH, RAX_AUTH_WEIGHT);
        prefixValues.put(ROLE_PROPAGATE_PATH, RAX_AUTH_PROPAGATE);
        prefixValues.put(ROLE_ASSIGNMENT_PATH, RAX_AUTH_ASSIGNMENT);

        write(role, entityStream, prefixValues);
    }
}
