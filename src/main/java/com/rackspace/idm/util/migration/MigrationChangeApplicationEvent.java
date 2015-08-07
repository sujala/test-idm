package com.rackspace.idm.util.migration;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Date;

/**
 * An change event compatible with Spring's Application Event framework.
 */
public abstract class MigrationChangeApplicationEvent extends ApplicationEvent implements MigrationChangeEvent {
    @Getter
    private Date changeOccurredDate;

    @Getter
    private ChangeType changeType;

    /**
     * This id must uniquely identify a given entity across the entire system.
     */
    @Getter
    private String entityUniqueIdentifier;

    public MigrationChangeApplicationEvent(Object source, Date changeOccurredDate, ChangeType changeType, String entityUniqueIdentifier) {
        super(source);
        this.changeOccurredDate = changeOccurredDate;
        this.changeType = changeType;
        this.entityUniqueIdentifier = entityUniqueIdentifier;
    }

}
