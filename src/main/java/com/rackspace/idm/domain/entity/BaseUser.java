package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;

/**
 * Marker interface that represents all classes of "users" within Identity.
 */
public interface BaseUser extends UniqueId, Auditable {
    //TODO: Not sure why this would be required for Rackers. Need to investigate and refactor if not necessary
    String getDomainId();

    boolean isDisabled();

    String getUsername();

    /**
     * The unique identifier (e.g. - rsId) for the user. This is different than the "uniqueId" as that has been used to refer
     * to the DN of ldap entries which is NOT what we want.
     * @return
     */
    String getId();
}
