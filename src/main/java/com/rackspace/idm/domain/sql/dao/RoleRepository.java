package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

@SQLRepository
public interface RoleRepository extends JpaSpecificationExecutor<SqlRole>, JpaRepository<SqlRole, String> {

    SqlRole findByName(String cloud);

    SqlRole findByRaxClientIdAndName(String clientId, String name);

    List<SqlRole> findByRaxClientId(String clientId);

    List<SqlRole> findByIdIn(Collection<String> baseUrlIds);

    List<SqlRole> findByNameIn(Collection<String> names);

    Page<SqlRole> findByRaxRsWeightGreaterThan(Integer rsWeight, Pageable pageable);

    Page<SqlRole> findByRaxClientIdAndRaxRsWeightGreaterThan(String clientId, Integer rsWeight, Pageable pageable);

}
