package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlIdentityProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@SQLRepository
public interface IdentityProviderRepository extends JpaSpecificationExecutor<SqlIdentityProvider>, JpaRepository<SqlIdentityProvider, String> {

    SqlIdentityProvider findByUri(String uri);

}
