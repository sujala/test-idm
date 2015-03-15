package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.ClientRole;

/**
 * A wrapper around a client role to make it immutable.
 */
public class ImmutableClientRole {

    private ClientRole innerClone;

    public ImmutableClientRole(ClientRole innerRole) {
        innerClone = cloneClientRole(innerRole);
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
     * Clone this as a client role for interoperability purposes.
     *
     * @return
     */
    public ClientRole asClientRole() {
        return cloneClientRole(innerClone);
    }

    private ClientRole cloneClientRole(ClientRole source) {
        ClientRole target = new ClientRole();
        target.setName(source.getName());
        target.setPropagate(source.getPropagate());
        target.setDescription(source.getDescription());
        target.setId(source.getId());
        target.setClientId(source.getClientId());
        target.setLdapEntry(source.getLDAPEntry());
        target.setRsWeight(source.getRsWeight());

        return target;
    }
}
