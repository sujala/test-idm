package com.rackspace.idm.util.migration.ldap;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.LdapGenericRepository;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.util.migration.ChangeEventDao;
import org.springframework.beans.factory.annotation.Autowired;

@LDAPComponent
public class LdapChangeEventRepository extends LdapGenericRepository<LdapMigrationChangeEvent> implements ChangeEventDao<LdapMigrationChangeEvent, LDAPMigrationChangeApplicationEvent> {

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public String getBaseDn() {
        return CHANGE_EVENT_BASE_DN;
    }

    @Override
    public String getNextId() {
        return super.getUuid();
    }

    @Override
    public String getSortAttribute() {
        return LdapRepository.ATTR_CHANGE_OCCURRED_DATE;
    }

    @Override
    public LdapMigrationChangeEvent recordChangeEvent(LDAPMigrationChangeApplicationEvent changeEvent) {
        LdapMigrationChangeEvent entity = new LdapMigrationChangeEvent(changeEvent, getNextId(), identityConfig.getReloadableConfig().getAENodeNameForSignoff());
        addObject(entity);
        return entity;
    }
}
