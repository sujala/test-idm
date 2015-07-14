package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlPolicy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@SQLRepository
public interface PolicyRepository extends JpaSpecificationExecutor<SqlPolicy>, JpaRepository<SqlPolicy, String> {

    @EntityGraph(value = "SqlPolicy.rax", type = EntityGraph.EntityGraphType.FETCH)
    public SqlPolicy findByRaxName(String cloud);
}
