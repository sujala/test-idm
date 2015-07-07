package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.BypassDevice;

public interface BypassDeviceDao {
    void addBypassDevice(UniqueId parent, BypassDevice bypassDevice);
    boolean deleteBypassDevice(BypassDevice bypassDevice);
    void updateBypassDevice(BypassDevice bypassDevice);
    Iterable<BypassDevice> getAllBypassDevices(UniqueId parent);
    void deleteAllBypassDevices(UniqueId parent);
}
