package com.rackspace.idm.domain.migration.event;

import com.rackspace.idm.domain.migration.ChangeType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Date;

/**
 * An change event compatible with Spring's Application Event framework.
 */
public abstract class MigrationChangeApplicationEvent extends ApplicationEvent implements MigrationChangeEvent {
    @Getter
    private ChangeType changeType;

    @Getter
    private String ldif;

    /**
     * This id must uniquely identify a given entity across the entire system.
     */
    @Getter
    private String entityUniqueIdentifier;

    public MigrationChangeApplicationEvent(Object source, ChangeType changeType, String entityUniqueIdentifier, String ldif) {
        super(source);
        this.changeType = changeType;
        this.entityUniqueIdentifier = entityUniqueIdentifier;
        this.ldif = ldif;
    }

}
