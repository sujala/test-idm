package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;



/**
 * Transforms the passed in object assuming it's the root of a tenantAssignment object
 */
public class TenantAssignmentArrayEntryTransformer implements JsonArrayEntryTransformer {
    @Override
    public void transform(JSONObject tenantAssignmentEntry) {
        if (tenantAssignmentEntry != null) {
            fixForTenantsArray(tenantAssignmentEntry);

            // Loop through sources to fix forTenants array in them
            JSONObject sources = (JSONObject) tenantAssignmentEntry.get("sources");
            if (sources != null) {
                JSONArray sourcesArray = (JSONArray) sources.get("source");
                if (sourcesArray != null) {
                    for (Object source : sourcesArray) {
                        fixForTenantsArray((JSONObject) source);
                    }
                }
            }
        }
    }

    private void fixForTenantsArray(JSONObject parent) {
        final JSONArray tenantArray = (JSONArray) parent.get(TenantAssignmentArrayTranformerHandler.FOR_TENANTS);
        if (tenantArray != null) {
            // Workaround to fix the "tenantIds" XML attribute JSON representation
            final String codes = (String) tenantArray.get(0);
            final JSONArray newCodes = new JSONArray();
            parent.put(TenantAssignmentArrayTranformerHandler.FOR_TENANTS, newCodes);
            for (String code : codes.split(" ")) {
                if (StringUtils.isNotBlank(code)) {
                    newCodes.add(code);
                }
            }
        }
    }
}
