package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.util.RoleUtil;

import java.util.Collections;
import java.util.Set;

/**
 * A wrapper around a tenant role to make it immutable.
 */
public class ImmutableTenantRole {

    private TenantRole innerClone;

    public ImmutableTenantRole(TenantRole innerRole) {
        innerClone = RoleUtil.cloneTenantRole(innerRole);
    }

    public String getUniqueId() {
        return innerClone.getUniqueId();
    }

    public Boolean getPropagate() {
        return innerClone.getPropagate();
    }

    public String getName() {
        return innerClone.getName();
    }

    public String getClientId() {
        return innerClone.getClientId();
    }

    public String getDescription() {
        return innerClone.getDescription();
    }

    public Set<String> getTenantIds() {
        return Collections.unmodifiableSet(innerClone.getTenantIds());
    }

    public String getUserId() {
        return innerClone.getUserId();
    }

    public String getRoleRsId() {
        return innerClone.getRoleRsId();
    }

    /**
     * Clone this as a client role for interoperability purposes.
     *
     * @return
     */
    public TenantRole asTenantRole() {
        return RoleUtil.cloneTenantRole(innerClone);
    }
}
