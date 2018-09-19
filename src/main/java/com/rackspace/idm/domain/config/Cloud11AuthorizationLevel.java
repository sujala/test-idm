package com.rackspace.idm.domain.config;

import lombok.Getter;

public enum Cloud11AuthorizationLevel {
    LEGACY("legacy"), ROLE("role"), FORBIDDEN("forbidden");

    @Getter
    private String representativeValue;

    Cloud11AuthorizationLevel(String representativeValue) {
        this.representativeValue = representativeValue;
    }

    public static Cloud11AuthorizationLevel fromValue(String val) {
        Cloud11AuthorizationLevel result = LEGACY;
        for (Cloud11AuthorizationLevel level : values()) {
            if (level.representativeValue.equalsIgnoreCase(val)) {
                result = level;
            }
        }
        return result;
    }
}
