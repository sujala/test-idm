package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlFederatedUserRax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@SQLRepository
public interface FederatedUserRepository extends JpaSpecificationExecutor<SqlFederatedUserRax>, JpaRepository<SqlFederatedUserRax, String> {

    Iterable<SqlFederatedUserRax> findByDomainId(String domainId);

    Iterable<SqlFederatedUserRax> findByDomainIdAndFederatedIdpUri(String domainId, String federatedIdpUri);

    int countByDomainIdAndFederatedIdpUri(String domainId, String federatedIdpUri);

    SqlFederatedUserRax findOneByUsernameAndFederatedIdpUri(String username, String federatedIdpUri);

}
