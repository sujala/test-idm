package com.rackspace.idm.domain.service;

/**
 * The defined 'user types' within identity. Every end user within identity is classified as one of these user types.
 */
public enum IdentityUserTypeEnum {
    SERVICE_ADMIN, IDENTITY_ADMIN, USER_ADMIN, USER_MANAGER, DEFAULT_USER;

    /**
     * Whether the role has at least user manager level access to the system.
     */
    public boolean hasAtLeastUserManagedAccessLevel() {
        if (this == SERVICE_ADMIN || this == IDENTITY_ADMIN || this == USER_ADMIN || this == USER_MANAGER) {
            return true;
        }
        return false;
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
        if (this == SERVICE_ADMIN || this == IDENTITY_ADMIN) {
            return true;
        }
        return false;
    }



}
