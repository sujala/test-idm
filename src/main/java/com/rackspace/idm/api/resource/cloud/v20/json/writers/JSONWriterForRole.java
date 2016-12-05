package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
import com.rackspace.idm.api.resource.cloud.JsonArrayTransformerHandler;
import org.json.simple.JSONObject;
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
        prefixValues.put(ROLE_ROLE_TYPE_PATH, RAX_AUTH_ROLE_TYPE);
        prefixValues.put(ROLE_TYPE_PATH, RAX_AUTH_TYPES);

        write(role, entityStream, prefixValues, new RoleJsonArrayTransformerHandler(), new RoleJsonArrayEntryTransformer(role));
    }

    public static class RoleJsonArrayTransformerHandler implements JsonArrayTransformerHandler {

        @Override
        public boolean pluralizeJSONArrayWithName(String elementName) {
            return true;
        }

        @Override
        public String getPluralizedNamed(String elementName) {
            if (JSONConstants.TYPE.equals(elementName)) {
                return JSONConstants.RAX_AUTH_TYPES;
            }
            return elementName + "s";
        }
    }

    private static class  RoleJsonArrayEntryTransformer implements JsonArrayEntryTransformer {

        private Role role;

        public RoleJsonArrayEntryTransformer(Role role) {
            this.role = role;
        }

        @Override
        public void transform(JSONObject arrayEntry) {
            if (arrayEntry.containsKey(JSONConstants.RAX_AUTH_TYPES)) {
                if (role.getTypes() == null) {
                    arrayEntry.remove(JSONConstants.RAX_AUTH_TYPES);
                }
            }

            if (arrayEntry.containsKey(JSONConstants.TYPES)) {
                Object tenantTypes = arrayEntry.get(JSONConstants.TYPES);
                arrayEntry.remove(JSONConstants.TYPES);
                arrayEntry.put(JSONConstants.RAX_AUTH_TYPES, tenantTypes);
            }
        }
    }
}
