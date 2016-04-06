package com.rackspace.idm.api.resource.cloud;

/**
 * Handler that will cause all json arrays to be wrapped/unwrapped.
 */
public class AlwaysPluralizeJsonArrayTransformerHandler implements JsonArrayTransformerHandler {

    @Override
    public boolean pluralizeJSONArrayWithName(String elementName) {
        return true;
    }

    @Override
    public String getPluralizedNamed(String elementName) {
        return elementName + "s";
    }

}
