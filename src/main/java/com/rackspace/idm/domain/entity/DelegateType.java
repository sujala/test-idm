package com.rackspace.idm.domain.entity;

public enum DelegateType {
    USER_GROUP,
    USER;

    public String value() {
        return name();
    }

    public static DelegateType fromValue(String v) {
        return valueOf(v);
    }
}
