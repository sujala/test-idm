package com.rackspace.idm.domain.migration.sql.dao;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.migration.sql.entity.SqlToLdapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@SQLComponent
public interface SqlToLdapRepository extends JpaSpecificationExecutor<SqlToLdapEntity>, JpaRepository<SqlToLdapEntity, String> {
    @Query("select c from SqlToLdapEntity c where c.type = :type order by c.created asc")
    List<SqlToLdapEntity> findByType(@Param("type") String type);
}
