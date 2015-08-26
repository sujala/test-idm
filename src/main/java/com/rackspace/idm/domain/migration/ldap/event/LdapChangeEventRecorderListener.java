package com.rackspace.idm.domain.migration.ldap.event;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.migration.dao.DeltaDao;
import com.rackspace.idm.domain.migration.event.MigrationChangeEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Records LDAP delta events
 */
@LDAPComponent
public class LdapChangeEventRecorderListener extends MigrationChangeEventListener<LdapMigrationChangeApplicationEvent> {

    public static final String LISTENER_NAME = "ldap_recorder";

    @Qualifier("ldapDeltaRepository")
    @Autowired(required = false)
    private DeltaDao changeEventDao;

    @Override
    public void handleEvent(LdapMigrationChangeApplicationEvent event) {
        if (changeEventDao != null) {
            changeEventDao.save(event.getChangeType(), event.getEntityUniqueIdentifier(), event.getLdif());
        }
    }

    @Override
    public String getListenerName() {
        return LISTENER_NAME;
    }
}
