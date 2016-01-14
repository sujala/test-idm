package com.rackspace.idm.domain.migration.sql.event;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
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

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public void handleEvent(SqlMigrationChangeApplicationEvent event) {
        if (changeEventDao != null) {
            if(identityConfig.getReloadableConfig().migrationSaveAsyncEnabled()) {
                changeEventDao.saveAsync(event);
            } else {
                changeEventDao.save(event);
            }
        }
    }

    @Override
    public String getListenerName() {
        return LISTENER_NAME;
    }
}
