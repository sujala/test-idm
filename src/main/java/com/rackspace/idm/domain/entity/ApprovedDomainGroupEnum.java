package com.rackspace.idm.domain.entity;

public enum ApprovedDomainGroupEnum {
    GLOBAL("GLOBAL");

    /**
     * This is the value provided by the API service for this enum value
     */
    private String storedVal;

    ApprovedDomainGroupEnum(String storedVal) {
        this.storedVal = storedVal;
    }

    public static ApprovedDomainGroupEnum lookupByStoredValue(String value) {
        for (ApprovedDomainGroupEnum approvedDomainGroupEnum : values()) {
            if (approvedDomainGroupEnum.storedVal.equals(value)) {
                return approvedDomainGroupEnum;
            }
        }
        return null;
    }

    public String getStoredVal() {
        return storedVal;
    }
}
