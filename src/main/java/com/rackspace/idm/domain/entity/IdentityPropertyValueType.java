package com.rackspace.idm.domain.entity;

public enum IdentityPropertyValueType {
    STRING("string"),
    BOOLEAN("boolean"),
    INT("int"),
    JSON("json"),
    YAML("yaml");

    String typeName;

    IdentityPropertyValueType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return this.typeName;
    }

    public static IdentityPropertyValueType getValueTypeByName(String name) {
        if (name == null) return null;

        for (IdentityPropertyValueType valueType : values()) {
            if (valueType.typeName.equalsIgnoreCase(name)) {
                return valueType;
            }
        }

        return null;
    }

}
