package com.rackspace.idm.util.migration;

public interface ChangeEventEntity {

    /**
     * Retrieve id of the entity
     */
    String getChangeEventEntityId();

    /**
     * Retrieve the uniquely identifying entity type of the changed entity.
     */
    String getChangeEventEntityType();
}
