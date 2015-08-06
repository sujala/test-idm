package com.rackspace.idm.util.migration;

public interface ChangeEventDao<S extends MigrationChangeEvent, T extends MigrationChangeApplicationEvent> {
    S recordChangeEvent(T migrationChangeApplicationEvent);
}
