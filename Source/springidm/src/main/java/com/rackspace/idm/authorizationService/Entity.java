package com.rackspace.idm.authorizationService;

import java.util.List;

public class Entity {

    String entityType;

    AttributeTable attributeTable;

    public Entity(String entityType) {
        attributeTable = new AttributeTable();
        this.entityType = entityType;
    }

    public void addAttribute(String attrId, String attrType, String attrValue) {
        AuthorizationAttribute attributeToAdd = new AuthorizationAttribute(attrId, attrType, attrValue);
        attributeTable.put(attrId, attributeToAdd);
    }

    public void addAttribute(String attrId, String attrType,
        List<String> attributeValues) {
        AuthorizationAttribute attributeToAdd = new AuthorizationAttribute(attrId, attrType,
            attributeValues);
        attributeTable.put(attrId, attributeToAdd);
    }

    public AttributeTable getAttributes() {
        return attributeTable;
    }

    public String getEntityType() {
        return entityType;
    }
}