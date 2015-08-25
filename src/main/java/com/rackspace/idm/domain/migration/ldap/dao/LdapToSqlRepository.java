package com.rackspace.idm.domain.migration.ldap.dao;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.migration.ldap.entity.LdapToSqlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@LDAPComponent
@Repository
public interface LdapToSqlRepository extends JpaSpecificationExecutor<LdapToSqlEntity>, JpaRepository<LdapToSqlEntity, String> {
    @Query("select c from LdapToSqlEntity c where c.type = :type order by c.created asc")
    List<LdapToSqlEntity> findByType(@Param("type") String type);
}
