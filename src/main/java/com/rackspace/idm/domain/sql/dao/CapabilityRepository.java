package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

@SQLRepository
public interface CapabilityRepository extends JpaSpecificationExecutor<SqlCapability>, JpaRepository<SqlCapability, String> {

    SqlCapability findByCapabilityIdAndTypeAndVersion(String capabilityId, String type, String version);

    List<SqlCapability> findByTypeAndVersion(String type, String version);
}
