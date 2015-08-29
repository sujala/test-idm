package com.rackspace.idm.domain.migration.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.LdapConnectionPools;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.dao.DeltaDao;
import com.rackspace.idm.domain.migration.ldap.dao.LdapToSqlRepository;
import com.rackspace.idm.domain.migration.ldap.entity.LdapToSqlEntity;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@LDAPComponent
@Repository
public class LdapDeltaRepository implements DeltaDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapDeltaRepository.class);

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private LdapConnectionPools connPools;

    @Autowired
    private LdapToSqlRepository ldapToSqlRepository;

    private LDAPInterface getAppInterface() {
        return connPools.getAppConnPoolInterface();
    }

    @Override
    public void save(ChangeType event, String type, String ldif) {
        try {
            final LdapToSqlEntity entity = new LdapToSqlEntity();
            entity.setId(UUID.randomUUID().toString().replaceAll("-", ""));
            entity.setType(type);
            entity.setEvent(event);
            entity.setHost(identityConfig.getReloadableConfig().getAENodeNameForSignoff());
            if (event != ChangeType.DELETE) {
                entity.setData(getAppInterface().getEntry(type).toLDIFString());
            }
            ldapToSqlRepository.save(entity);
        } catch (Exception e) {
            LOGGER.error("Cannot save delta (LDAP->SQL)!", e);
        }
    }

    @Override
    public List<LdapToSqlEntity> findByType(String type) {
        return ldapToSqlRepository.findByType(type);
    }

    @Override
    public void deleteAll() {
        ldapToSqlRepository.deleteAll();
    }

}
