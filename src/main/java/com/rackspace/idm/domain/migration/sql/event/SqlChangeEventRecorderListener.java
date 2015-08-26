package com.rackspace.idm.domain.migration.sql.event;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.migration.dao.DeltaDao;
import com.rackspace.idm.domain.migration.event.MigrationChangeEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Records SQL delta events
 */
@SQLComponent
public class SqlChangeEventRecorderListener extends MigrationChangeEventListener<SqlMigrationChangeApplicationEvent> {

    public static final String LISTENER_NAME = "sql_recorder";

    @Qualifier("sqlDeltaRepository")
    @Autowired(required = false)
    private DeltaDao changeEventDao;

    @Override
    public void handleEvent(SqlMigrationChangeApplicationEvent event) {
        if (changeEventDao != null) {
            changeEventDao.save(event.getChangeType(), event.getEntityUniqueIdentifier(), event.getLdif());
        }
    }

    @Override
    public String getListenerName() {
        return LISTENER_NAME;
    }
}
