package com.rackspace.idm.domain.entity;

import org.apache.commons.lang.StringUtils;

public enum TokenScopeEnum {
    SETUP_MFA("SETUP-MFA"), PWD_RESET("PWD-RESET"), MFA_SESSION_ID("MFA-SESSION-ID");

    private String scope;

    TokenScopeEnum(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public static TokenScopeEnum fromScope(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        for (TokenScopeEnum tokenScopeEnum : values()) {
            if (value.equalsIgnoreCase(tokenScopeEnum.scope)) {
                return tokenScopeEnum;
            }
        }
        return null;
    }
}
