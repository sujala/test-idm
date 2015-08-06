package com.rackspace.idm.util.migration.ldap;

import com.rackspace.idm.util.migration.ChangeType;
import com.rackspace.idm.util.migration.MigrationChangeApplicationEvent;
import com.rackspace.idm.util.migration.PersistenceTarget;

import java.util.Date;

public class LDAPMigrationChangeApplicationEvent extends MigrationChangeApplicationEvent {

    public LDAPMigrationChangeApplicationEvent(Object source, Date changeOccurredDate, ChangeType changeType, String entityDn) {
        super(source, changeOccurredDate, changeType, entityDn);
    }

    @Override
    public PersistenceTarget getPersistenceTarget() {
        return PersistenceTarget.LDAP;
    }
}
