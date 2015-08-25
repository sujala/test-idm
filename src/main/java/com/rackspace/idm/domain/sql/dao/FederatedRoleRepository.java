package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlFederatedRoleRax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@SQLRepository
public interface FederatedRoleRepository extends JpaSpecificationExecutor<SqlFederatedRoleRax>, JpaRepository<SqlFederatedRoleRax, String> {

    SqlFederatedRoleRax findOneByRoleRsIdAndUserId(String roleId, String userId);

    List<SqlFederatedRoleRax> findByRoleRsId(String roleId);

    List<SqlFederatedRoleRax> findByUserIdAndClientId(String userId, String clientId);

    List<SqlFederatedRoleRax> findByUserId(String userId);

    @Query("select tr from SqlFederatedRoleRax tr where tr.roleRsId in :roleIds")
    List<SqlFederatedRoleRax> findByRoleRsIds(@Param("roleIds") List<String> roleIds);

    @Query("select tr from SqlFederatedRoleRax tr where :tenantId member of tr.tenantIds")
    List<SqlFederatedRoleRax> findByTenantId(@Param("tenantId") String tenantId);

    @Query("select tr from SqlFederatedRoleRax tr where :tenantId member of tr.tenantIds and tr.roleRsId = :roleRsId")
    List<SqlFederatedRoleRax> findByTenantIdAndRoleRsId(@Param("tenantId") String tenantId, @Param("roleRsId") String roleRsId);

    Long deleteByUserIdAndRoleRsId(String userId, String roleId);

}
