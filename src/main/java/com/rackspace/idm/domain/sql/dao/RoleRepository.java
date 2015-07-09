package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


@SQLRepository
public interface RoleRepository extends JpaSpecificationExecutor<SqlRole>, JpaRepository<SqlRole, String> {

    public SqlRole findByName(String cloud);

    public SqlRole findByRaxClientIdAndName(String clientId, String name);

    public List<SqlRole> findByRaxClientId(String clientId);

    public List<SqlRole> findByIdIn(Collection<String> baseUrlIds);

    public List<SqlRole> findByNameIn(Collection<String> names);

    public Page<SqlRole> findByRaxRsWeightGreaterThan(Integer rsWeight, Pageable pageable);

    public Page<SqlRole> findByRaxClientIdAndRaxRsWeightGreaterThan(String clientId, Integer rsWeight, Pageable pageable);
}