package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.BypassDeviceDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.BypassDevice;
import com.rackspace.idm.domain.sql.dao.BypassDeviceRepository;
import com.rackspace.idm.domain.sql.entity.SqlBypassDevice;
import com.rackspace.idm.domain.sql.mapper.impl.BypassDeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@SQLComponent
public class SqlBypassDeviceRepository implements BypassDeviceDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlBypassDeviceRepository.class);

    @Autowired
    private BypassDeviceMapper mapper;

    @Autowired
    private BypassDeviceRepository repository;

    @Override
    @Transactional
    public void addBypassDevice(UniqueId parent, BypassDevice bypassDevice) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            final SqlBypassDevice device = mapper.toSQL(bypassDevice);
            device.setUserId(userId);
            repository.save(device);

            // Update original entity (TODO: improve the way this method is used to avoid this)
            bypassDevice.setUniqueId(mapper.fromSqlBypassDeviceToUniqueId(device));
        }
    }

    @Override
    @Transactional
    public void deleteAllBypassDevices(UniqueId parent) {
        try {
            if (parent instanceof BaseUser) {
                final String userId = ((BaseUser) parent).getId();
                repository.deleteByUserId(userId);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot remove bypass devices for uniqueId: " + parent.getUniqueId(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteBypassDevice(BypassDevice bypassDevice) {
        try {
            repository.delete(bypassDevice.getId());
            return true;
        } catch (Exception e) {
            LOGGER.error("Cannot remove bypass device: " + bypassDevice.getId(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public void updateBypassDevice(BypassDevice bypassDevice) {
        try {
            repository.save(mapper.toSQL(bypassDevice, repository.findOne(bypassDevice.getId())));
        } catch (Exception e) {
            LOGGER.error("Cannot update bypass device: " + bypassDevice.getId(), e);
        }
    }

    @Override
    public Iterable<BypassDevice> getAllBypassDevices(UniqueId parent) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            return mapper.fromSQL(repository.findByUserId(userId));
        }
        return Collections.EMPTY_LIST;
    }

}
