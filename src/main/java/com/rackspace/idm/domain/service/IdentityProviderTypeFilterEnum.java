package com.rackspace.idm.domain.service;

import org.apache.commons.lang.StringUtils;

public enum IdentityProviderTypeFilterEnum {
    EXPLICIT("EXPLICIT");

    private String idpType;

    IdentityProviderTypeFilterEnum(String idpType) {
        this.idpType = idpType;
    }

    public static IdentityProviderTypeFilterEnum parseIdpTypeFilter(String idpType) {
        if (StringUtils.isBlank(idpType)) {
            return null;
        }

        for(IdentityProviderTypeFilterEnum idpFilter : values()) {
            if (idpFilter.idpType.equals(idpType)) {
                return idpFilter;
            }
        }

        return null;
    }
}
