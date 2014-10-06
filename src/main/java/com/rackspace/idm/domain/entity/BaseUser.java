package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import lombok.Data;

/**
 * Marker interface that represents all classes of "users" within Identity.
 */
public interface BaseUser extends UniqueId, Auditable {
    //TODO: Not sure why this would be required for Rackers. Need to investigate and refactor if not necessary
    String getDomainId();

    boolean isDisabled();

    String getUsername();
}
