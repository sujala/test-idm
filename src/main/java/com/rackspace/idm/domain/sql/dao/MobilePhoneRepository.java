package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlMobilePhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@SQLRepository
public interface MobilePhoneRepository extends JpaSpecificationExecutor<SqlMobilePhone>, JpaRepository<SqlMobilePhone, String> {

    SqlMobilePhone getByExternalMultiFactorPhoneId(String externalMultiFactorPhoneId);

    SqlMobilePhone getByTelephoneNumber(String telephoneNumber);

}
