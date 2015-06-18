package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.UserScopeAccess;

/**
 * Operations for provisioned user based identity providers
 */
public interface FederatedUserDao extends FederatedBaseUserDao<FederatedUser> {
    /**
     * Retrieve all federated users that belong to the specified domain across all identity providers
     * @param domainId
     * @return
     */
    Iterable<FederatedUser> getUsersByDomainId(String domainId);
}
