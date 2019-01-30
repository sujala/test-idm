package com.rackspace.idm.api.resource.cloud.atomHopper;

public enum FeedsUserStatusEnum {

    CREATE,
    DELETED,
    DISABLED,
    ENABLED,
    GROUP,
    MIGRATED,
    MULTI_FACTOR,
    ROLE,
    UPDATE,
    USER_GROUP;

    public String value() {
        return name();
    }

    public static FeedsUserStatusEnum fromValue(String v) {
        return valueOf(v);
    }

    public boolean isUpdateEvent() {
        return this == GROUP
                || this == MULTI_FACTOR
                || this == ROLE
                || this == UPDATE
                || this == USER_GROUP;
    }
}
