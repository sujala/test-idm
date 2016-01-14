package com.rackspace.idm.domain.migration.ldap.event;

import com.rackspace.idm.annotation.MigrationComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.migration.dao.DeltaDao;
import com.rackspace.idm.domain.migration.event.MigrationChangeEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Records LDAP delta events
 */
@MigrationComponent
public class LdapChangeEventRecorderListener extends MigrationChangeEventListener<LdapMigrationChangeApplicationEvent> {

    public static final String LISTENER_NAME = "ldap_recorder";

    @Qualifier("ldapDeltaRepository")
    @Autowired(required = false)
    private DeltaDao changeEventDao;

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public void handleEvent(LdapMigrationChangeApplicationEvent event) {
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
