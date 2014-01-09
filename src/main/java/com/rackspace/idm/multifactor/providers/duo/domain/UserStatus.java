package com.rackspace.idm.multifactor.providers.duo.domain;

/**
*/
public enum UserStatus {
    ACTIVE("active"), BYPASS("bypass"), DISABLED("disabled"), LOCKED_OUT("locked out"), ;

    private String duoSecurityCode;

    UserStatus(String duoSecurityCode) {
        this.duoSecurityCode = duoSecurityCode;
    }

    public String toDuoSecurityCode() {
        return duoSecurityCode;
    }

    public static UserStatus fromDuoSecurityCode(String duoSecurityCode) {
        for (UserStatus userStatus : values()) {
            if (userStatus.duoSecurityCode.equals(duoSecurityCode)) {
                return userStatus;
            }
        }
        return null;
    }

}
