package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlIdentityProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SQLRepository
public interface IdentityProviderRepository extends JpaSpecificationExecutor<SqlIdentityProvider>, JpaRepository<SqlIdentityProvider, String> {

    SqlIdentityProvider findByUri(String uri);

    @Query("select c.name from SqlIdentityProvider c where c.uri = :uri")
    String getIdpNameByURI(@Param("uri") String uri);

}
