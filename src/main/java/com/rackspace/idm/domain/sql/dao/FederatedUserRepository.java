package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlFederatedUserRax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

@SQLRepository
public interface FederatedUserRepository extends JpaSpecificationExecutor<SqlFederatedUserRax>, JpaRepository<SqlFederatedUserRax, String> {

    Iterable<SqlFederatedUserRax> findByDomainId(String domainId);

    @Query("select u from SqlFederatedUserRax u where u.domainId = :domainId and u.federatedIdpUri = (select idp.uri from SqlIdentityProvider idp where idp.name = :idpName)")
    Iterable<SqlFederatedUserRax> findByDomainIdAndFederatedIdpName(@Param("domainId") String domainId, @Param("idpName") String federatedIdpName);

    @Query("select count(u) from SqlFederatedUserRax u where u.domainId = :domainId and u.federatedIdpUri = (select idp.uri from SqlIdentityProvider idp where idp.name = :idpName)")
    int countByDomainIdAndFederatedIdpName(@Param("domainId") String domainId, @Param("idpName") String federatedIdpName);

    @Query("select u from SqlFederatedUserRax u where u.username = :username and u.federatedIdpUri = (select idp.uri from SqlIdentityProvider idp where idp.name = :idpName)")
    SqlFederatedUserRax findOneByUsernameAndFederatedIdpName(@Param("username") String username, @Param("idpName") String federatedIdpName);

    Iterable<SqlFederatedUserRax> findByRsGroupIdIn(Collection<String> rsGroupId);

}
