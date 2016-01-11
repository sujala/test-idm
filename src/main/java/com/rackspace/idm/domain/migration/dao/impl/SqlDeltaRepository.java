package com.rackspace.idm.domain.migration.dao.impl;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.dao.DeltaDao;
import com.rackspace.idm.domain.migration.event.MigrationChangeEvent;
import com.rackspace.idm.domain.migration.sql.dao.SqlToLdapRepository;
import com.rackspace.idm.domain.migration.sql.entity.SqlToLdapEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@SQLRepository
public class SqlDeltaRepository implements DeltaDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapDeltaRepository.class);

    private static final String PERSIST_DELTA_ERROR_LOG_FORMAT = "Error saving migration change event. UUID: %s, CHANGE TYPE: %s, ENTITY ID: %s, PERSISTENCE: %s";

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private SqlToLdapRepository sqlToLdapRepository;

    @Override
    @Transactional
    public void save(MigrationChangeEvent changeEvent) {
        saveChangeEvent(changeEvent);
    }

    @Override
    @Transactional
    @Async("deltaMigrationExecutor")
    public void saveAsync(MigrationChangeEvent changeEvent) {
        saveChangeEvent(changeEvent);
    }

    public void saveChangeEvent(MigrationChangeEvent changeEvent) {
        try {
            final SqlToLdapEntity entity = new SqlToLdapEntity();
            entity.setId(changeEvent.getId());
            entity.setType(changeEvent.getEntityUniqueIdentifier());
            entity.setEvent(changeEvent.getChangeType());
            entity.setHost(identityConfig.getReloadableConfig().getAENodeNameForSignoff());
            entity.setData(changeEvent.getLdif());
            sqlToLdapRepository.save(entity);
        } catch (Exception e) {
            LOGGER.error(String.format(PERSIST_DELTA_ERROR_LOG_FORMAT, changeEvent.getId(), changeEvent.getChangeType().name(),
                    changeEvent.getEntityUniqueIdentifier(), changeEvent.getPersistenceTarget().name()), e);
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
