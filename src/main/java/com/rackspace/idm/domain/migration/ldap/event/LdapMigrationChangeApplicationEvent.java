package com.rackspace.idm.domain.migration.ldap.event;

import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.PersistenceTarget;
import com.rackspace.idm.domain.migration.event.MigrationChangeApplicationEvent;

public class LdapMigrationChangeApplicationEvent extends MigrationChangeApplicationEvent {

    public LdapMigrationChangeApplicationEvent(Object source, ChangeType changeType, String entityDn, String ldif) {
        super(source, changeType, entityDn, ldif);
    }

    @Override
    public PersistenceTarget getPersistenceTarget() {
        return PersistenceTarget.LDAP;
    }
}
