package com.rackspace.idm.authorizationService;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class AttributeTable {

    Map<String, AuthorizationAttribute> attributes; // Key=<attrId, Attribute object>

    public AttributeTable() {
        attributes = new Hashtable<String, AuthorizationAttribute>();
    }

    public void put(String attributeId, AuthorizationAttribute attribute) {
        attributes.put(attributeId, attribute);
    }

    public AuthorizationAttribute get(String attributeId) {
        return (AuthorizationAttribute) attributes.get(attributeId);
    }

    public Set<String> keys() {
        return attributes.keySet();
    }
}