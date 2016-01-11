package com.rackspace.idm.domain.migration.dao;

import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.event.MigrationChangeEvent;

import java.util.List;

public interface DeltaDao {

    /**
     * Save a migration change event.
     *
     * @param changeEvent
     */
    void save(MigrationChangeEvent changeEvent);

    /**
     * Save a migration change event asynchronously.
     *
     * @param changeEvent
     */
    void saveAsync(MigrationChangeEvent changeEvent);

    List<?> findByType(String type);

    void deleteAll();

}
