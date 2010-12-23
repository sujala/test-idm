package com.rackspace.idm.authorizationService;

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class AuthorizationAttribute {

    String type;
    String attrId;
    Vector<String> attributeValues = new Vector<String>();

    public AuthorizationAttribute(String attrId) {
        this.attrId = attrId;
        this.type = AuthorizationConstants.TYPE_STRING;
    }

    public AuthorizationAttribute(String attrId, String type) {
        this.attrId = attrId;
        this.type = type;
    }

    public AuthorizationAttribute(String attrId, String type, String value) {
        this.attrId = attrId;
        this.type = type;
        this.attributeValues.add(value);
    }

    public AuthorizationAttribute(String attrId, String type, List<String> values) {
        this.attrId = attrId;
        this.type = type;
        this.attributeValues.addAll(values);
    }

    public String getType() {
        return type;
    }

    public void addValue(String value) {
        if (!this.attributeValues.contains(value)) {
            this.attributeValues.add(value);
        }
    }

    public Enumeration<String> values() {
        return this.attributeValues.elements();
    }

    public String getId() {
        return attrId;
    }

    public List<String> getValues() {
        return attributeValues;
    }
}