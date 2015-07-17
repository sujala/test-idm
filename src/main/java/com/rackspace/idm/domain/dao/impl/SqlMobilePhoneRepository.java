package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.MobilePhoneDao;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.sql.dao.MobilePhoneRepository;
import com.rackspace.idm.domain.sql.mapper.impl.MobilePhoneMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@SQLComponent
public class SqlMobilePhoneRepository implements MobilePhoneDao {

    @Autowired
    MobilePhoneMapper mapper;

    @Autowired
    MobilePhoneRepository repository;

    @Override
    public MobilePhone getByExternalId(String externalMultiFactorPhoneId) {
        return mapper.fromSQL(repository.getByExternalMultiFactorPhoneId(externalMultiFactorPhoneId));
    }

    @Override
    public MobilePhone getById(String id) {
        return mapper.fromSQL(repository.findOne(id));
    }

    @Override
    public MobilePhone getByTelephoneNumber(String telephoneNumber) {
        return mapper.fromSQL(repository.getByTelephoneNumber(telephoneNumber));
    }

    @Override
    public void addMobilePhone(MobilePhone mobilePhone) {
        repository.save(mapper.toSQL(mobilePhone));
    }

    @Override
    public void updateMobilePhone(MobilePhone mobilePhone) {
        repository.save(mapper.toSQL(mobilePhone));
    }

    @Override
    public void deleteMobilePhone(MobilePhone mobilePhone) {
        repository.delete(mapper.toSQL(mobilePhone));
    }

    @Override
    public String getNextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
