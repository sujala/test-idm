package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
import com.rackspace.idm.api.resource.cloud.JsonArrayTransformerHandler;
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForEntity;
import org.apache.commons.lang.StringUtils;
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
    private JsonArrayTransformerHandler arrayHandler = new TenantAssignmentArrayTranformerHandler();
    private TenantAssignmentArrayEntryTransformer tenantAssignmentArrayEntryTransformer = new TenantAssignmentArrayEntryTransformer();

    @Override
    public void writeTo(RoleAssignments roleAssignments, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(ROLE_ASSIGNMENTS, RAX_AUTH_ROLE_ASSIGNMENTS);
        write(roleAssignments, entityStream, prefixValues, arrayHandler, new EntryTransformer());
    }

    private class EntryTransformer implements JsonArrayEntryTransformer {
        /*
         * This will get called with the top level object due to the prefix value getting mapped, but before the
         * inner array of tenantOnRoleAssignments is flattened
         *
         * For example, {"RAX-AUTH:roleAssignments":{"tenantAssignments":{"tenantAssignment":[{"forTenants":["tenant1 tenant2"],"onRole":"12345"}]}}}
         */
        @Override
        public void transform(JSONObject entry) {
            if (entry != null) {
                JSONObject roleAssignments = (JSONObject) entry.get(RAX_AUTH_ROLE_ASSIGNMENTS);
                JSONObject outerTAssignments = (JSONObject) roleAssignments.get(TENANT_ASSIGNMENTS);

                if (outerTAssignments != null) {
                    JSONArray innerTAssignments = (JSONArray) outerTAssignments.get(TENANT_ASSIGNMENT);
                    for (Object tAssignmentObj : innerTAssignments) {
                        JSONObject tAssignment = (JSONObject) tAssignmentObj;
                        tenantAssignmentArrayEntryTransformer.transform(tAssignment);
                    }
                } else {
                    // Add the outer/inner objects to put in an empty array
                    outerTAssignments = new JSONObject();
                    JSONArray innerTAssignments =  new JSONArray();
                    outerTAssignments.put(TENANT_ASSIGNMENT, innerTAssignments);
                    roleAssignments.replace(TENANT_ASSIGNMENTS, null, outerTAssignments);
                }
            }
        }
    }
}