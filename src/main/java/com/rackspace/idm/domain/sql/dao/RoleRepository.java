package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

@SQLRepository
public interface RoleRepository extends JpaSpecificationExecutor<SqlRole>, JpaRepository<SqlRole, String> {

    @EntityGraph(value = "SqlRole.rax", type = EntityGraph.EntityGraphType.FETCH)
    SqlRole findByName(String cloud);

    @EntityGraph(value = "SqlRole.rax", type = EntityGraph.EntityGraphType.FETCH)
    SqlRole findByRaxClientIdAndName(String clientId, String name);

    @EntityGraph(value = "SqlRole.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlRole> findByRaxClientId(String clientId);

    List<SqlRole> findByIdIn(Collection<String> baseUrlIds);

    List<SqlRole> findByNameIn(Collection<String> names);

    @EntityGraph(value = "SqlRole.rax", type = EntityGraph.EntityGraphType.FETCH)
    Page<SqlRole> findByRaxRsWeightGreaterThan(Integer rsWeight, Pageable pageable);

    @EntityGraph(value = "SqlRole.rax", type = EntityGraph.EntityGraphType.FETCH)
    Page<SqlRole> findByRaxClientIdAndRaxRsWeightGreaterThan(String clientId, Integer rsWeight, Pageable pageable);

    @Override
    @EntityGraph(value = "SqlRole.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlRole> findAll();

}
