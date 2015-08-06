package com.rackspace.idm.util.migration.ldap;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.util.migration.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Records LDAP events to LDAP
 */
@LDAPComponent
public class LdapChangeEventRecorderListener extends MigrationChangeEventListener<LDAPMigrationChangeApplicationEvent> {

    public static final String LISTENER_NAME = "ldap_recorder";

    @Qualifier("ldapChangeEventRepository")
    @Autowired
    private ChangeEventDao ldapChangeEventDao;

    @Override
    public void handleEvent(LDAPMigrationChangeApplicationEvent event) {
        ldapChangeEventDao.recordChangeEvent(event);
    }

    @Override
    public String getListenerName() {
        return LISTENER_NAME;
    }
}
