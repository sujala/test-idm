package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Capability;

public interface CapabilityDao {
    String getNextCapabilityId();

    void addCapability(Capability capability);

    Capability getCapability(String id, String type, String version);

    Iterable<Capability> getCapabilities(String type, String version);

    void deleteCapability(String id, String type, String version);
}
