package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.BypassDeviceDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BypassDevice;
import com.unboundid.ldap.sdk.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@LDAPComponent
public class LdapBypassDeviceRepository extends LdapGenericRepository<BypassDevice> implements BypassDeviceDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapBypassDeviceRepository.class);

    @Override
    public String getBaseDn() {
        return USERS_BASE_DN;
    }

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_BYPASS_DEVICE;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public void addBypassDevice(UniqueId parent, BypassDevice bypassDevice) {
        addObject(addLdapContainer(parent.getUniqueId(), LdapRepository.CONTAINER_BYPASS_CODES), bypassDevice);
    }

    @Override
    public void deleteAllBypassDevices(UniqueId parent) {
        for (BypassDevice bypassDevice : getAllBypassDevices(parent)) {
            deleteBypassDevice(bypassDevice);
        }
    }

    @Override
    public boolean deleteBypassDevice(BypassDevice bypassDevice) {
        try {
            deleteObject(bypassDevice);
            return true;
        } catch (Exception e) {
            LOGGER.error("Cannot remove bypass device: " + bypassDevice.getId(), e);
            return false;
        }
    }

    @Override
    public void updateBypassDevice(BypassDevice bypassDevice) {
        try {
            updateObject(bypassDevice);
        } catch (Exception e) {
            LOGGER.error("Cannot update bypass device: " + bypassDevice.getId(), e);
        }
    }

    @Override
    public Iterable<BypassDevice> getAllBypassDevices(UniqueId parent) {
        try {
            final Filter filter = new LdapSearchBuilder()
                    .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BYPASS_DEVICE).build();
            return getObjects(filter, getContainerDN(parent));
        } catch (Exception e) {
            return Collections.EMPTY_LIST;
        }
    }

    private String getContainerDN(UniqueId parent) {
        return new LdapDnBuilder(parent.getUniqueId())
                .addAttribute(ATTR_NAME, LdapRepository.CONTAINER_BYPASS_CODES).build();
    }

}
