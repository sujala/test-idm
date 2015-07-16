package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlServiceApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

@SQLRepository
public interface ServiceApiRepository extends JpaSpecificationExecutor<SqlServiceApi>, JpaRepository<SqlServiceApi, String> {

    List<SqlServiceApi> findByVersionNotNullAndTypeNotNull();
}
