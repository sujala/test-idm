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
     * @param identityProviderId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderId(String domainId, String identityProviderId);


    /**
     * Count federated users that belong to the specified domain and identity provider
     * @param domainId
     * @param identityProviderId
     * @return
     *
     * @deprecated - use Unexpired version
     */
    int getFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String identityProviderId);

    /**
     * Count unexpired federated users that belong to the specified domain and identity provider
     *
     * @param domainId
     * @param identityProviderId
     * @return
     */
    int getUnexpiredFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String identityProviderId);

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

    /**
     * Delete federated user
     */
    void deleteUser(FederatedUser federatedUser);

    /**
     * Get one expired federated user
     */
    FederatedUser getSingleExpiredFederatedUser();

}
