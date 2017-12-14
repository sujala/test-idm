package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.rackspace.idm.api.resource.cloud.JsonArrayTransformerHandler;

/**
 * Skip forTenants because that array isn't wrapped
 */
public class TenantAssignmentArrayTranformerHandler implements JsonArrayTransformerHandler {
    public static final String FOR_TENANTS = "forTenants";

    @Override
    public boolean pluralizeJSONArrayWithName(String elementName) {
        return !elementName.equals(FOR_TENANTS);
    }

    @Override
    public String getPluralizedNamed(String elementName) {
        return elementName + "s";
    }
}
