package com.rackspace.idm.api.resource.cloud;

/**
 * Handler that will not wrap/unwrap any json arrays.
 */
public class NeverPluralizeJsonArrayTransformerHandler implements JsonArrayTransformerHandler {

    @Override
    public boolean pluralizeJSONArrayWithName(String elementName) {
        return false;
    }

    @Override
    public String getPluralizedNamed(String elementName) {
        return elementName + "s";
    }

}
