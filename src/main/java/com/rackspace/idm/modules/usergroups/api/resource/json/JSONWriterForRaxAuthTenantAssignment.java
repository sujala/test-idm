package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
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

import static com.rackspace.idm.modules.usergroups.Constants.RAX_AUTH_TENANT_ASSIGNMENT;
import static com.rackspace.idm.modules.usergroups.Constants.TENANT_ASSIGNMENT;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthTenantAssignment extends JSONWriterForEntity<TenantAssignment> {

    private static final String TENANT_IDS = "forTenants";

    @Override
    public void writeTo(TenantAssignment tenantAssignment, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(TENANT_ASSIGNMENT, RAX_AUTH_TENANT_ASSIGNMENT);
        write(tenantAssignment, entityStream, prefixValues, NEVER_PLURALIZE_HANDLER, new EntryTransformer());
    }

    private class EntryTransformer implements JsonArrayEntryTransformer {
        @Override
        public void transform(JSONObject entry) {
            if (entry != null) {
                JSONObject tenantAssignment = (JSONObject) entry.get(RAX_AUTH_TENANT_ASSIGNMENT);

                final JSONArray tenantArray = (JSONArray) tenantAssignment.get(TENANT_IDS);
                if (tenantArray != null) {
                    // Workaround to fix the "tenantIds" XML attribute JSON representation
                    final String codes = (String) tenantArray.get(0);
                    final JSONArray newCodes = new JSONArray();
                    tenantAssignment.put(TENANT_IDS, newCodes);
                    for (String code : codes.split(" ")) {
                        if (StringUtils.isNotBlank(code)) {
                            newCodes.add(code);
                        }
                    }
                }
            }
        }
    }
}
