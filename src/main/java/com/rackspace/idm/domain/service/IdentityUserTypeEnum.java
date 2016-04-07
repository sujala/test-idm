package com.rackspace.idm.domain.service;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The defined 'user types' within identity. Every end user within identity is classified as one of these user types.
 */
public enum IdentityUserTypeEnum {
    SERVICE_ADMIN(RoleLevelEnum.LEVEL_0, "identity:service-admin")
    , IDENTITY_ADMIN(RoleLevelEnum.LEVEL_100, "identity:admin")
    , USER_ADMIN(RoleLevelEnum.LEVEL_750, "identity:user-admin")
    , USER_MANAGER(RoleLevelEnum.LEVEL_900, "identity:user-manage")
    , DEFAULT_USER(RoleLevelEnum.LEVEL_2000, "identity:default");

    private RoleLevelEnum level; //weight
    private String roleName;

    private IdentityUserTypeEnum(RoleLevelEnum level, String roleName) {
        this.level = level;
        this.roleName = roleName;
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

    /**
     * Returns false if provided argument is null.
     *
     * @param that
     * @return
     */
    public boolean hasLevelAccessOf(IdentityUserTypeEnum that) {
        if (that == null) {
            return false;
        }
        return this.level.getLevelAsInt() <= that.level.getLevelAsInt();
    }

    public String getRoleName() {
        return roleName;
    }

    public int getLevelAsInt() {
        return level.getLevelAsInt();
    }

    public RoleLevelEnum getLevel() {
        return level;
    }

    public static IdentityUserTypeEnum fromRoleName(String roleName) {
        if (StringUtils.isBlank(roleName)) {
            return null;
        }

        for (IdentityUserTypeEnum identityUserTypeEnum : values()) {
            if (identityUserTypeEnum.roleName.equalsIgnoreCase(roleName)) {
                return identityUserTypeEnum;
            }
        }
        return null;
    }

    public static boolean isIdentityUserTypeRoleName(String roleName) {
        return fromRoleName(roleName) != null;
    }

    public static final List<String> getUserTypeRoleNames() {
        List<String> roleNames = new ArrayList<String>();
        for (IdentityUserTypeEnum identityUserTypeEnum : IdentityUserTypeEnum.values()) {
            roleNames.add(identityUserTypeEnum.roleName);
        }
        return roleNames;
    }
}
