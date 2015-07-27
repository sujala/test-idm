package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.sql.entity.SqlTokenRevocationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

@SQLComponent
public interface TokenRevocationRecordRepository extends JpaRepository<SqlTokenRevocationRecord, String>, TokenRevocationRecordRepositoryCustom {
}
