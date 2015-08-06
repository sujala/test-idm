package com.rackspace.idm.util.migration;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.util.migration.MigrationChangeApplicationEvent;
import com.rackspace.idm.util.migration.MigrationChangeEventListener;
import com.rackspace.idm.util.migration.PersistenceTarget;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Records events to log file
 */
@Component
public class ChangeEventLoggerListener extends MigrationChangeEventListener<MigrationChangeApplicationEvent> {

    private final Logger migrationChangeEventLogger = LoggerFactory.getLogger(GlobalConstants.MIGRATION_CHANGE_EVENT_LOG_NAME);

    public static final String LISTENER_NAME = "logger";

    private static final String logFormat = "TIMESTAMP: %s, CHANGE TYPE: %s, ENTITY ID: %s, PERSISTENCE: %s";
    DateTimeFormatter df = ISODateTimeFormat.basicDateTime();

    @Override
    public void handleEvent(MigrationChangeApplicationEvent event) {
        migrationChangeEventLogger.warn(formatChangeEventLog(event));
    }

    @Override
    public String getListenerName() {
        return LISTENER_NAME;
    }

    private String formatChangeEventLog(MigrationChangeApplicationEvent event) {
        return String.format(logFormat, df.print(event.getChangeOccurredDate().getTime()), event.getChangeType().name()
                , event.getEntityUniqueIdentifier(), event.getPersistenceTarget().name());
    }
}
