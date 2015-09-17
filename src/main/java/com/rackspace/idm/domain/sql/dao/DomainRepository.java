package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlDomain;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@SQLRepository
public interface DomainRepository extends JpaSpecificationExecutor<SqlDomain>, JpaRepository<SqlDomain, String> {

    @Override
    @EntityGraph(value = "SqlDomain.rax", type = EntityGraph.EntityGraphType.FETCH)
    SqlDomain findOne(String domainId);

    @Override
    @EntityGraph(value = "SqlDomain.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlDomain> findAll();

    Long countByName(String name);

    @Query("select count(d) from SqlDomain d where d.name = :name and d.domainId != :id")
    Long countByNameAndNotDomainId(@Param("name") String name, @Param("id") String id);

}
