package com.rackspace.idm.api.security;

import java.util.List;

/**
 * Represents a list of identity roles associated with a calling user against which authorization decisions should be made.
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

}
