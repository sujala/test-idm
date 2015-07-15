package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@SQLRepository
public interface PropertyRepository extends JpaSpecificationExecutor<SqlProperty>, JpaRepository<SqlProperty, String> {
}
