package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.CapabilityDao;
import com.rackspace.idm.domain.entity.Capability;
import com.rackspace.idm.domain.sql.dao.CapabilityRepository;
import com.rackspace.idm.domain.sql.mapper.impl.CapabilityMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@SQLComponent
public class SqlCapabilityRepository implements CapabilityDao {

    @Autowired
    CapabilityMapper mapper;

    @Autowired
    CapabilityRepository repository;

    @Override
    public String getNextCapabilityId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void addCapability(Capability capability) {
        repository.save(mapper.toSQL(capability));
    }

    @Override
    public Capability getCapability(String id, String type, String version) {
        return mapper.fromSQL(repository.findByIdAndTypeAndVersion(id, type, version));
    }

    @Override
    public Iterable<Capability> getCapabilities(String type, String version) {
        return mapper.fromSQL(repository.findByTypeAndVersion(type, version));
    }

    @Override
    public void deleteCapability(String id, String type, String version) {
        repository.delete(id);
    }
}
