package com.rackspace.idm.domain.service;

/**
 * The defined 'user types' within identity. Every end user within identity is classified as one of these user types.
 */
public enum IdentityUserTypeEnum {
    SERVICE_ADMIN(0), IDENTITY_ADMIN(100), USER_ADMIN(750), USER_MANAGER(900), DEFAULT_USER(2000);

    private int level;

    private IdentityUserTypeEnum(int level) {
        this.level = level;
    }

    /**
     * Whether the role has at least user manager level access to the system.
     */
    public boolean hasAtLeastUserManagedAccessLevel() {
        return hasLevelAccessOf(USER_MANAGER);
    }

    public boolean isDomainBasedAccessLevel() {
        if (this == USER_ADMIN || this == USER_MANAGER || this == DEFAULT_USER) {
            return true;
        }
        return false;
    }

    /**
     * Whether the role has at least identity admin level access to the system.
     */
    public boolean hasAtLeastIdentityAdminAccessLevel() {
        return hasLevelAccessOf(IDENTITY_ADMIN);
    }

    public boolean hasLevelAccessOf(IdentityUserTypeEnum that) {
        return this.level <= that.level;
    }


}
