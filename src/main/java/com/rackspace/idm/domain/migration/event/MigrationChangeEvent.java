package com.rackspace.idm.domain.migration.event;

import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.PersistenceTarget;

import java.util.Date;

public interface MigrationChangeEvent {

    /**
     * Get the UUID of the event
     *
     * @return
     */
    String getId();

    /**
     * Get the Type of change
     * @return
     */
    ChangeType getChangeType();

    /**
     * Get an identifier that will uniquely identify the particular instance of the entity type.
     * @return
     */
    String getEntityUniqueIdentifier();

    /**
     * The ldif
     * @return
     */
    String getLdif();

    /**
     * What backing store was changed
     * @return
     */
    PersistenceTarget getPersistenceTarget();
}
