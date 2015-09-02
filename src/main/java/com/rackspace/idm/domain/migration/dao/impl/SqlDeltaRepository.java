package com.rackspace.idm.domain.migration.dao.impl;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.dao.DeltaDao;
import com.rackspace.idm.domain.migration.sql.dao.SqlToLdapRepository;
import com.rackspace.idm.domain.migration.sql.entity.SqlToLdapEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.UUID;

@SQLRepository
public class SqlDeltaRepository implements DeltaDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapDeltaRepository.class);

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private SqlToLdapRepository sqlToLdapRepository;

    @Override
    @Async("deltaMigrationExecutor")
    public void save(ChangeType event, String type, String ldif) {
        try {
            final SqlToLdapEntity entity = new SqlToLdapEntity();
            entity.setId(UUID.randomUUID().toString().replaceAll("-", ""));
            entity.setType(type);
            entity.setEvent(event);
            entity.setHost(identityConfig.getReloadableConfig().getAENodeNameForSignoff());
            if (event != ChangeType.DELETE) {
                entity.setData(ldif);
            }
            sqlToLdapRepository.save(entity);
        } catch (Exception e) {
            LOGGER.error("Cannot save delta (SQL->LDAP)!", e);
        }
    }

    @Override
    public List<SqlToLdapEntity> findByType(String type) {
        return sqlToLdapRepository.findByType(type);
    }

    @Override
    public void deleteAll() {
        sqlToLdapRepository.deleteAll();
    }

}
