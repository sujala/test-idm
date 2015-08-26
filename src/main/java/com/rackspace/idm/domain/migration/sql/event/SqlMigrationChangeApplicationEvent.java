package com.rackspace.idm.domain.migration.sql.event;

import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.PersistenceTarget;
import com.rackspace.idm.domain.migration.event.MigrationChangeApplicationEvent;

public class SqlMigrationChangeApplicationEvent extends MigrationChangeApplicationEvent {

    public SqlMigrationChangeApplicationEvent(Object source, ChangeType changeType, String entityIdentifier, String ldif) {
        super(source, changeType, entityIdentifier, ldif);
    }

    @Override
    public PersistenceTarget getPersistenceTarget() {
        return PersistenceTarget.SQL;
    }
}
