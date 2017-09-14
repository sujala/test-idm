package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForEntity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
import static com.rackspace.idm.modules.usergroups.Constants.*;
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthRoleAssignments extends JSONWriterForEntity<RoleAssignments> {
    private static final String TENANT_IDS = "forTenants";

    @Override
    public void writeTo(RoleAssignments roleAssignments, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(ROLE_ASSIGNMENTS, RAX_AUTH_ROLE_ASSIGNMENTS);
        write(roleAssignments, entityStream, prefixValues, ALWAYS_PLURALIZE_HANDLER, new EntryTransformer());
    }

    private class EntryTransformer implements JsonArrayEntryTransformer {
        /*
         * This will get called with the top level object due to the prefix value getting mapped, but before the
         * inner array of tenantOnRoleAssignments is flattened
         *
         * For example, {"roleAssignments":{"tenantAssignments":{"tenantAssignment":[{"forTenants":["tenant1 tenant2"],"onRole":"12345"}]}}}
         */
        @Override
        public void transform(JSONObject entry) {
            if (entry != null) {
                JSONObject roleAssignments = (JSONObject) entry.get(RAX_AUTH_ROLE_ASSIGNMENTS);
                JSONObject outerTAssignments = (JSONObject) roleAssignments.get(TENANT_ASSIGNMENTS);

                if (outerTAssignments != null) {
                    JSONArray innerTAssignments = (JSONArray) outerTAssignments.get("tenantAssignment");
                    for (Object tAssignmentObj : innerTAssignments) {
                        JSONObject tAssignment = (JSONObject) tAssignmentObj;
                        final JSONArray tenantArray = (JSONArray) tAssignment.get(TENANT_IDS);
                        if (tenantArray != null) {
                            // Workaround to fix the "tenantIds" XML attribute JSON representation
                            final String codes = (String) tenantArray.get(0);
                            final JSONArray newCodes = new JSONArray();
                            tAssignment.put(TENANT_IDS, newCodes);
                            for (String code : codes.split(" ")) {
                                newCodes.add(code);
                            }
                        }
                    }
                }
            }
        }
    }
}