package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlBypassDevice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

@SQLRepository
public interface BypassDeviceRepository extends JpaSpecificationExecutor<SqlBypassDevice>, JpaRepository<SqlBypassDevice, String> {

    List<SqlBypassDevice> findByUserId(String userId);

    void deleteByUserId(String userId);

}
