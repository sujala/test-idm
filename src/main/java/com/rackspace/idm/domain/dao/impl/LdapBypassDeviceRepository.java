package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.BypassDeviceDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BypassDevice;
import com.unboundid.ldap.sdk.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
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

    private Iterable<BypassDevice> getUnexpiredAndCleanExpiredBypassDevices(UniqueId parent) {
        final List<BypassDevice> result = new ArrayList<BypassDevice>();
        for (BypassDevice bypassDevice : getAllBypassDevices(parent)) {
            if (bypassDevice.getMultiFactorDevicePinExpiration() == null ||
                    bypassDevice.getMultiFactorDevicePinExpiration().compareTo(new Date()) >= 0) {
                result.add(bypassDevice);
            } else {
                LOGGER.info("Clean expired bypass code: " + bypassDevice.getId());
                deleteBypassDevice(bypassDevice);
            }
        }
        return result;
    }

    @Override
    public boolean useBypassCode(UniqueId parent, String passcode) {
        for (BypassDevice bypassDevice : getUnexpiredAndCleanExpiredBypassDevices(parent)) {
            for (String bypassCode : bypassDevice.getBypassCodes()) {
                if (bypassCode.equals(passcode)) {
                    removeBypassCodeFromBypassDevice(bypassDevice, bypassCode);
                    return true;
                }
            }
        }
        return false;
    }

    private void removeBypassCodeFromBypassDevice(BypassDevice bypassDevice, String bypassCode) {
        bypassDevice.getBypassCodes().remove(bypassCode);
        if (bypassDevice.getBypassCodes().size() == 0) {
            deleteBypassDevice(bypassDevice);
        } else {
            updateBypassDevice(bypassDevice);
        }
    }

    private void deleteBypassDevice(BypassDevice bypassDevice) {
        try {
            deleteObject(bypassDevice);
        } catch (Exception e) {
            LOGGER.error("Cannot remove bypass device: " + bypassDevice.getId(), e);
        }
    }

    private void updateBypassDevice(BypassDevice bypassDevice) {
        try {
            updateObject(bypassDevice);
        } catch (Exception e) {
            LOGGER.error("Cannot update bypass device: " + bypassDevice.getId(), e);
        }
    }

    private Iterable<BypassDevice> getAllBypassDevices(UniqueId parent) {
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
