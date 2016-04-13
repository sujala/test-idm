package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
import org.json.simple.JSONObject;
import org.openstack.docs.identity.api.v2.RoleList;

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
public class JSONWriterForRoles extends JSONWriterForArrayEntity<RoleList> {

    @Override
    public void writeTo(RoleList roleList, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(roleList, JSONConstants.ROLES, JSONConstants.ROLES, entityStream, new RoleEntryTransformer());
    }

    private static class RoleEntryTransformer implements JsonArrayEntryTransformer {
        @Override
        public void transform(JSONObject arrayEntry) {
            if (arrayEntry.containsKey(JSONConstants.PROPAGATE)) {
                Object prop = arrayEntry.get(JSONConstants.PROPAGATE);
                arrayEntry.remove(JSONConstants.PROPAGATE);
                arrayEntry.put(JSONConstants.RAX_AUTH_PROPAGATE, prop);
            }

            if (arrayEntry.containsKey(JSONConstants.ADMINISTRATOR_ROLE)) {
                Object prop = arrayEntry.get(JSONConstants.ADMINISTRATOR_ROLE);
                arrayEntry.remove(JSONConstants.ADMINISTRATOR_ROLE);
                arrayEntry.put(JSONConstants.RAX_AUTH_ADMINISTRATOR_ROLE, prop);
            }
        }
    }
}
