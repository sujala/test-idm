package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@SQLRepository
public interface GroupRepository extends JpaSpecificationExecutor<SqlGroup>, JpaRepository<SqlGroup, String> {

    public SqlGroup findByName(String name);

}
