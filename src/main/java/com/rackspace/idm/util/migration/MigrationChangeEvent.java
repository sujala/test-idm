package com.rackspace.idm.util.migration;

import java.util.Date;

public interface MigrationChangeEvent {
    /**
     * Get the Type of change
     * @return
     */
    ChangeType getChangeType();

    /**
     * Get the date the change was made
     *
     * @return
     */
    Date getChangeOccurredDate();

    /**
     * Get an identifier that will uniquely identify the particular instance of the entity type.
     * @return
     */
    String getEntityUniqueIdentifier();

    /**
     * What backing store was changed
     * @return
     */
    PersistenceTarget getPersistenceTarget();
}
