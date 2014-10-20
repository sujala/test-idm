package com.rackspace.idm.domain.entity;

import org.apache.commons.lang.StringUtils;

public enum AuthenticatedByMethodEnum {
    NULL(null)
    ,APIKEY("APIKEY")
    ,PASSWORD("PASSWORD")
    ,FEDERATION("FEDERATED")
    ,IMPERSONATION("IMPERSONATION")
    ,PASSCODE("PASSCODE")
    ,RSAKEY("RSAKEY")
    ;

    String value;

    private AuthenticatedByMethodEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AuthenticatedByMethodEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return NULL;
        }
        for (AuthenticatedByMethodEnum authenticatedByMethodEnum : values()) {
            if (value.equalsIgnoreCase(authenticatedByMethodEnum.value)) {
                return authenticatedByMethodEnum;
            }
        }
        return null;
    }
}
