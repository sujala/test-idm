package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.sql.entity.SqlIdentityProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@SQLRepository
public interface IdentityProviderRepository extends JpaSpecificationExecutor<SqlIdentityProvider>, JpaRepository<SqlIdentityProvider, String> {

    SqlIdentityProvider findByUri(String uri);

    @Query("select c.name from SqlIdentityProvider c where c.uri = :uri")
    String getIdpNameByURI(@Param("uri") String uri);

    List<SqlIdentityProvider> findByApprovedDomainIdsContains(String approvedDomainId);

    @Query("select idp from SqlIdentityProvider idp where :domainId member of idp.approvedDomainIds or idp.approvedDomainGroup = :domainGroup")
    List<SqlIdentityProvider> findByApprovedDomainIdsContainsOrApprovedDomainGroupEquals(@Param("domainId") String approvedDomainId, @Param("domainGroup") String approvedDomainGroup);

}
