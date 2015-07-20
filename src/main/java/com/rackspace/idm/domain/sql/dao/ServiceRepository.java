package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlService;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

@SQLRepository
public interface ServiceRepository extends JpaSpecificationExecutor<SqlService>, JpaRepository<SqlService, String> {

    @Override
    @EntityGraph(value = "SqlService.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlService> findAll();

    @EntityGraph(value = "SqlService.rax", type = EntityGraph.EntityGraphType.FETCH)
    public List<SqlService> findByOpenStackType(String type);
}
