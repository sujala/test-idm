package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.service.IdentityUserTypeEnum;

import java.util.List;

/**
 * Represents a list of identity roles associated with a calling user against which authorization decisions should be made.
 *
 * TODO: Switch this to use {@link com.rackspace.idm.domain.entity.SourcedRoleAssignments} rather than a lists of roles
 */
public class AuthorizationContext {

    private List<ImmutableTenantRole> explicitRoles;
    private List<ImmutableClientRole> implicitRoles;

    public AuthorizationContext(List<ImmutableTenantRole> explicitRoles, List<ImmutableClientRole> implicitRoles) {
        this.explicitRoles = explicitRoles;
        this.implicitRoles = implicitRoles;
    }

    public boolean hasRoleWithId(String id) {
        if (id == null) {
            return false;
        }

        for (ImmutableTenantRole tenantRole : explicitRoles) {
            if (id.equals(tenantRole.getRoleRsId())) {
                return true;
            }
        }

        for (ImmutableClientRole role : implicitRoles) {
            if (id.equals(role.getId())) {
                return true;
            }
        }

        return false;
    }

    public boolean hasRoleWithName(String name) {
        if (name == null) {
            return false;
        }

        for (ImmutableTenantRole tenantRole : explicitRoles) {
            if (name.equals(tenantRole.getName())) {
                return true;
            }
        }

        for (ImmutableClientRole role : implicitRoles) {
            if (name.equals(role.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the lowest weight user type role included in the list of roles or null if no roles match an identity
     * user type.
     *
     * @return
     */
    public IdentityUserTypeEnum getIdentityUserType() {
        IdentityUserTypeEnum userType = null;
        for (ImmutableTenantRole explicitRole : explicitRoles) {
            IdentityUserTypeEnum type = IdentityUserTypeEnum.fromRoleName(explicitRole.getName());
            if (type != null && (userType == null || type.getLevel().isLowerWeightThan(userType.getLevel()))) {
                userType = type;
            }
        }

        //check implicit roles
        for (ImmutableClientRole implicit : implicitRoles) {
            IdentityUserTypeEnum type = IdentityUserTypeEnum.fromRoleName(implicit.getName());
            if (type != null && (userType == null || type.getLevel().isLowerWeightThan(userType.getLevel()))) {
                userType = type;
            }
        }

        return userType;
    }
}
