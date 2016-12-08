package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlFederatedUserRax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;

@SQLRepository
public interface FederatedUserRepository extends JpaSpecificationExecutor<SqlFederatedUserRax>, JpaRepository<SqlFederatedUserRax, String> {

    Iterable<SqlFederatedUserRax> findByDomainId(String domainId);

    @Query("select u from SqlFederatedUserRax u where u.domainId = :domainId and u.federatedIdpUri = (select idp.uri from SqlIdentityProvider idp where idp.id = :idpId)")
    Iterable<SqlFederatedUserRax> findByDomainIdAndFederatedIdpId(@Param("domainId") String domainId, @Param("idpId") String federatedIdpId);

    @Query("select count(u) from SqlFederatedUserRax u where u.domainId = :domainId and u.federatedIdpUri = (select idp.uri from SqlIdentityProvider idp where idp.id = :idId)")
    int countByDomainIdAndFederatedIdpId(@Param("domainId") String domainId, @Param("idpId") String federatedIdpId);

    @Query("select u from SqlFederatedUserRax u where u.username = :username and u.federatedIdpUri = (select idp.uri from SqlIdentityProvider idp where idp.name = :idpId)")
    SqlFederatedUserRax findOneByUsernameAndFederatedIdpId(@Param("username") String username, @Param("idpId") String federatedIdpId);

    Iterable<SqlFederatedUserRax> findByRsGroupIdIn(Collection<String> rsGroupId);

    @Query("select i.name from SqlIdentityProvider i where i.uri = (select u.federatedIdpUri from SqlFederatedUserRax u where u.id = :userId)")
    String getIdpNameByUserId(@Param("userId") String userId);

    @Query("select u.username from SqlFederatedUserRax u where u.id = :userId")
    String getUsernameByUserId(@Param("userId") String userId);

    SqlFederatedUserRax findFirstByExpiredTimestampLessThanEqual(Date today);

}
