package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.BypassDevice;

public interface BypassDeviceDao extends GenericDao<BypassDevice> {
    void deleteAllBypassDevices(UniqueId parent);
    void addBypassDevice(UniqueId parent, BypassDevice bypassDevice);
    boolean useBypassCode(UniqueId parent, String bypassCode);
}
