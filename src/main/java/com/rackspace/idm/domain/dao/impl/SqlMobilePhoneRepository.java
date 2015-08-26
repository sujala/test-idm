package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.MobilePhoneDao;
import com.rackspace.idm.domain.entity.MobilePhone;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.sql.event.SqlMigrationChangeApplicationEvent;
import com.rackspace.idm.domain.sql.dao.MobilePhoneRepository;
import com.rackspace.idm.domain.sql.entity.SqlMobilePhone;
import com.rackspace.idm.domain.sql.mapper.impl.MobilePhoneMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@SQLComponent
public class SqlMobilePhoneRepository implements MobilePhoneDao {

    @Autowired
    private MobilePhoneMapper mapper;

    @Autowired
    private MobilePhoneRepository repository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void addMobilePhone(MobilePhone mobilePhone) {
        final SqlMobilePhone sqlMobilePhone = repository.save(mapper.toSQL(mobilePhone));

        final MobilePhone newMobilePhone = mapper.fromSQL(sqlMobilePhone, mobilePhone);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.ADD, newMobilePhone.getUniqueId(), mapper.toLDIF(newMobilePhone)));
    }

    @Override
    @Transactional
    public void updateMobilePhone(MobilePhone mobilePhone) {
        final SqlMobilePhone sqlMobilePhone = repository.save(mapper.toSQL(mobilePhone, repository.findOne(mobilePhone.getId())));

        final MobilePhone newMobilePhone = mapper.fromSQL(sqlMobilePhone, mobilePhone);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.MODIFY, newMobilePhone.getUniqueId(), mapper.toLDIF(newMobilePhone)));
    }

    @Override
    @Transactional
    public void deleteMobilePhone(MobilePhone mobilePhone) {
        repository.delete(mobilePhone.getId());
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.MODIFY, mobilePhone.getUniqueId(), null));
    }

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
    public String getNextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
