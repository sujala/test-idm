package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.Group;

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

    /**
     * Get the groups for that user.
     *
      * @param userId
     * @return
     */
    Iterable<Group> getGroupsForFederatedUser(String userId);

    /**
     * Get federated users by group.
     *
     * @param groupId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersByGroupId(String groupId);

}
