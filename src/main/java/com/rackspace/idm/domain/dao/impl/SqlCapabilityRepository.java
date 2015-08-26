package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.CapabilityDao;
import com.rackspace.idm.domain.entity.Capability;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.sql.event.SqlMigrationChangeApplicationEvent;
import com.rackspace.idm.domain.sql.dao.CapabilityRepository;
import com.rackspace.idm.domain.sql.entity.SqlCapability;
import com.rackspace.idm.domain.sql.mapper.impl.CapabilityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@SQLComponent
public class SqlCapabilityRepository implements CapabilityDao {

    @Autowired
    private CapabilityMapper mapper;

    @Autowired
    private CapabilityRepository repository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void addCapability(Capability capability) {
        final SqlCapability sqlCapability = repository.save(mapper.toSQL(capability));

        final Capability newCapability = mapper.fromSQL(sqlCapability, capability);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.ADD, newCapability.getUniqueId(), mapper.toLDIF(newCapability)));
    }

    @Override
    @Transactional
    public void deleteCapability(String capabilityId, String type, String version) {
        final SqlCapability sqlCapability = repository.findByCapabilityIdAndTypeAndVersion(capabilityId, type, version);
        repository.delete(sqlCapability);

        final Capability capability = mapper.fromSQL(sqlCapability);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.DELETE, capability.getUniqueId(), mapper.toLDIF(capability)));
    }

    @Override
    public Capability getCapability(String id, String type, String version) {
        return mapper.fromSQL(repository.findByCapabilityIdAndTypeAndVersion(id, type, version));
    }

    @Override
    public Iterable<Capability> getCapabilities(String type, String version) {
        return mapper.fromSQL(repository.findByTypeAndVersion(type, version));
    }

    @Override
    public String getNextCapabilityId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
