package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

@SQLRepository
public interface UserRepository extends JpaSpecificationExecutor<SqlUser>, JpaRepository<SqlUser, String> {

    void deleteByUsername(String username);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlUser> findByUsername(String username);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    SqlUser findOneByUsername(String username);

    int countByUsername(String username);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlUser> findByDomainId(String domainId);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    Page<SqlUser> findByDomainId(String domainId, Pageable pageable);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    Page<SqlUser> findAll(Pageable pageable);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    Page<SqlUser> findByEnabledTrue(Pageable pageable);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlUser> findByDomainIdAndEnabled(String domainId, Boolean enabled);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlUser> findByEnabledAndRsGroupIdIn(Boolean enabled, Collection<String> rsGroupId);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    Page<SqlUser> findByEnabledAndRsGroupIdIn(Boolean enabled, Collection<String> rsGroupId, Pageable pageable);

    @EntityGraph(value = "SqlUser.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlUser> findByExtraContains(String email);

}