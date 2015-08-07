package com.rackspace.idm.util.migration.sql;

import com.rackspace.idm.util.migration.ChangeType;
import com.rackspace.idm.util.migration.MigrationChangeApplicationEvent;
import com.rackspace.idm.util.migration.PersistenceTarget;

import java.util.Date;

public class SQLMigrationChangeApplicationEvent extends MigrationChangeApplicationEvent {

    public SQLMigrationChangeApplicationEvent(Object source, Date changeOccurredDate, ChangeType changeType, String entityIdentifier) {
        super(source, changeOccurredDate, changeType, entityIdentifier);
    }

    @Override
    public PersistenceTarget getPersistenceTarget() {
        return PersistenceTarget.SQL;
    }
}
