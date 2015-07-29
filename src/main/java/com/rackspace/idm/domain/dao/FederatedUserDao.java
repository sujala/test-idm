package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.FederatedUser;

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

    /**
     * Retrieve all federated users that belong to the specified domain and identity provider
     * @param domainId
     * @param identityProviderName
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderName(String domainId, String identityProviderName);


    /**
     * Count federated users that belong to the specified domain and identity provider
     * @param domainId
     * @param identityProviderName
     * @return
     */
    int getFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String identityProviderName);

}
