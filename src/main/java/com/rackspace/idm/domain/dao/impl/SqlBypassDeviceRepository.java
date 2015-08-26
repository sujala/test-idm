package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.BypassDeviceDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.BypassDevice;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.sql.event.SqlMigrationChangeApplicationEvent;
import com.rackspace.idm.domain.sql.dao.BypassDeviceRepository;
import com.rackspace.idm.domain.sql.entity.SqlBypassDevice;
import com.rackspace.idm.domain.sql.mapper.impl.BypassDeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@SQLComponent
public class SqlBypassDeviceRepository implements BypassDeviceDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlBypassDeviceRepository.class);

    @Autowired
    private BypassDeviceMapper mapper;

    @Autowired
    private BypassDeviceRepository repository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void addBypassDevice(UniqueId parent, BypassDevice bypassDevice) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            SqlBypassDevice device = mapper.toSQL(bypassDevice);
            device.setUserId(userId);
            device = repository.save(device);

            final BypassDevice newBypassDevice = mapper.fromSQL(device, bypassDevice);
            applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.ADD, newBypassDevice.getUniqueId(), mapper.toLDIF(newBypassDevice)));
        }
    }

    @Override
    @Transactional
    public void updateBypassDevice(BypassDevice bypassDevice) {
        try {
            final SqlBypassDevice device = repository.save(mapper.toSQL(bypassDevice, repository.findOne(bypassDevice.getId())));

            final BypassDevice newBypassDevice = mapper.fromSQL(device, bypassDevice);
            applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.MODIFY, newBypassDevice.getUniqueId(), mapper.toLDIF(newBypassDevice)));
        } catch (Exception e) {
            LOGGER.error("Cannot update bypass device: " + bypassDevice.getId(), e);
        }
    }

    @Override
    @Transactional
    public void deleteAllBypassDevices(UniqueId parent) {
        try {
            if (parent instanceof BaseUser) {
                final String userId = ((BaseUser) parent).getId();
                final List<SqlBypassDevice> devices = repository.findByUserId(userId);
                repository.deleteByUserId(userId);

                if (devices != null) {
                    for (SqlBypassDevice device : devices) {
                        final BypassDevice bypassDevice = mapper.fromSQL(device);
                        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.DELETE, bypassDevice.getUniqueId(), null));
                    }
                }
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
            applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.DELETE, bypassDevice.getUniqueId(), null));
            return true;
        } catch (Exception e) {
            LOGGER.error("Cannot remove bypass device: " + bypassDevice.getId(), e);
            return false;
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
