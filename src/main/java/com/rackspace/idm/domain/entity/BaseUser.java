package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import lombok.Data;

@Data
public class BaseUser implements UniqueId {
    public String uniqueId;

    public boolean isDisabled() {
        return false;
    }
}
