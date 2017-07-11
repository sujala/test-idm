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
    ,SYSTEM("SYSTEM") // Auto created by system. Should not be exposed externally
    ,OTPPASSCODE("OTPPASSCODE")
    ,TOKEN("TOKEN") // When a NEW token is issued based on passing a previous token (v3 concept of scoping an unscoped token)
    ,EMAIL("EMAIL") // User could only receive token via email. So by fact they know token, they must have access to user email
    ,OTHER("OTHER") // Used in Fed when IDP sends unrecognized (or no) AuthContextClassRef value
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
