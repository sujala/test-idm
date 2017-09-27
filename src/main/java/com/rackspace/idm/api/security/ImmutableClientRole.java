package com.rackspace.idm.api.security;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.util.RoleUtil;

import java.util.Collections;
import java.util.Set;

/**
 * A wrapper around a client role to make it immutable. Did not simply create an interface w/ read only methods because
 * the underlying object is still mutable. Due to need to be interoperable w/ existing code (e.g. morph to client role)
 * that would allow the supposed "immutable" object to be mutable. As this object will be used in a cache, complete
 * immutability is critical to avoid poisoning caches.
 */
public class ImmutableClientRole {

    private ClientRole innerClone;

    public ImmutableClientRole(ClientRole innerRole) {
        innerClone = RoleUtil.cloneClientRole(innerRole);
    }

    public String getAssignmentType() {
        return innerClone.getAssignmentType();
    }

    public RoleTypeEnum getRoleType() {
        return innerClone.getRoleType();
    }

    public RoleAssignmentEnum getAssignmentTypeAsEnum() {
        return innerClone.getAssignmentTypeAsEnum();
    }

    public Set<String> getTenantTypes() {
        return Collections.unmodifiableSet(innerClone.getTenantTypes());
    }

    public String getUniqueId() {
        return innerClone.getUniqueId();
    }

    public Boolean getPropagate() {
        return innerClone.getPropagate();
    }

    public String getId() {
        return innerClone.getId();
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

    public int getRsWeight() {
        return innerClone.getRsWeight();
    }


    /**
     * Clone this as a client role for interoperability purposes with existing code that expects a ClientRole
     *
     * @return
     */
    public ClientRole asClientRole() {
        return RoleUtil.cloneClientRole(innerClone);
    }
}
