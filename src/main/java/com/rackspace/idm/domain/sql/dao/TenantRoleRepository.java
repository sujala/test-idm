package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.domain.sql.entity.SqlAssignmentId;
import com.rackspace.idm.domain.sql.entity.SqlRole;
import com.rackspace.idm.domain.sql.entity.SqlTenantRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantRoleRepository extends JpaSpecificationExecutor<SqlTenantRole>, JpaRepository<SqlTenantRole, SqlAssignmentId> {

    @EntityGraph(value = "SqlTenantRole.sqlRole", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlTenantRole> findByActorId(String actorId);

    @EntityGraph(value = "SqlTenantRole.sqlRole", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlTenantRole> findByTargetId(String actorId);

    @EntityGraph(value = "SqlTenantRole.sqlRole", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlTenantRole> findByRoleId(String roleId);

    @EntityGraph(value = "SqlTenantRole.sqlRole", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlTenantRole> findByRoleId(String roleId, Pageable pageable);

    @EntityGraph(value = "SqlTenantRole.sqlRole", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlTenantRole> findByActorIdAndRoleId(String actorId, String roleId);

    @EntityGraph(value = "SqlTenantRole.sqlRole", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlTenantRole> findByTargetIdAndRoleId(String targetId, String roleId);

    @EntityGraph(value = "SqlTenantRole.sqlRole", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlTenantRole> findByActorIdAndSqlRoleRaxClientId(String actorId, String applicationId);

    void deleteBySqlRoleId(String id);

    /*
     * Following code fails in Hibernate, due to bug:
     * https://hibernate.atlassian.net/browse/HHH-9230
    */
    //@EntityGraph(value = "SqlTenantRole.sqlRole", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlTenantRole> findByActorIdAndRoleIdIn(String actorId, List<String> roleIds);
}
