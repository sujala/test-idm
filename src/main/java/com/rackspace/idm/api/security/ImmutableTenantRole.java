package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.TenantRole;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper around a tenant role to make it immutable.
 */
public class ImmutableTenantRole {

    private TenantRole innerClone;

    public ImmutableTenantRole(TenantRole innerRole) {
        innerClone = cloneTenantRole(innerRole);
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
        return cloneTenantRole(innerClone);
    }

    private TenantRole cloneTenantRole(TenantRole source) {
        TenantRole target = new TenantRole();
        target.setName(source.getName());
        target.setPropagate(source.getPropagate());
        target.setDescription(source.getDescription());
        target.setRoleRsId(source.getRoleRsId());
        target.setClientId(source.getClientId());
        target.setUniqueId(source.getUniqueId());
        target.setUserId(source.getUserId());
        target.setTenantIds(new HashSet<String>(source.getTenantIds()));

        return target;
    }
}
