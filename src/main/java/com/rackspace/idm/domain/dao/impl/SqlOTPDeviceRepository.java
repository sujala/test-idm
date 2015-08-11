package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.OTPDeviceDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.OTPDevice;
import com.rackspace.idm.domain.sql.dao.OTPDeviceRepository;
import com.rackspace.idm.domain.sql.entity.SqlOTPDevice;
import com.rackspace.idm.domain.sql.mapper.impl.OTPDeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@SQLComponent
public class SqlOTPDeviceRepository implements OTPDeviceDao {

    @Autowired
    OTPDeviceMapper mapper;

    @Autowired
    OTPDeviceRepository repository;

    @Override
    @Transactional
    public void addOTPDevice(UniqueId parent, OTPDevice otpDevice) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            final SqlOTPDevice device = mapper.toSQL(otpDevice);
            device.setUserId(userId);
            repository.save(device);

            // Update original entity (TODO: improve the way this method is used to avoid this)
            otpDevice.setUniqueId(mapper.fromSqlOTPDeviceToUniqueId(device));
        }
    }

    @Override
    public OTPDevice getOTPDeviceByParentAndId(UniqueId parent, String id) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            final SqlOTPDevice sqlOTPDevice = repository.findByIdAndUserId(id, userId);
            return mapper.fromSQL(sqlOTPDevice);
        }
        return null;
    }

    @Override
    public int countOTPDevicesByParent(UniqueId parent) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            return repository.countByUserId(userId);
        }
        return 0;
    }

    @Override
    public int countVerifiedOTPDevicesByParent(UniqueId parent) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            return repository.countByUserIdAndMultiFactorDeviceVerified(userId, true);
        }
        return 0;
    }

    @Override
    public Iterable<OTPDevice> getVerifiedOTPDevicesByParent(UniqueId parent) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            final List<SqlOTPDevice> list = repository.findByUserIdAndMultiFactorDeviceVerified(userId, true);
            return mapper.fromSQL(list);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public Iterable<OTPDevice> getOTPDevicesByParent(UniqueId parent) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            final List<SqlOTPDevice> list = repository.findByUserId(userId);
            return mapper.fromSQL(list);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    @Transactional
    public void deleteAllOTPDevicesFromParent(UniqueId parent) {
        if (parent instanceof BaseUser) {
            final String userId = ((BaseUser) parent).getId();
            repository.deleteByUserId(userId);
        }
    }

    @Override
    @Transactional
    public void deleteOTPDevice(OTPDevice object) {
        repository.delete(object.getId());
    }

    @Override
    @Transactional
    public void updateOTPDevice(OTPDevice object) {
        repository.save(mapper.toSQL(object, repository.findOne(object.getId())));
    }

}
