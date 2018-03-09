package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.User;

import java.util.List;

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
     * Retrieve all federated users that do not belong to the approved domains in the identity provider
     * @param approvedDomainIds
     * @param identityProviderId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(List<String> approvedDomainIds, String identityProviderId);

    /**
     * Retrieve all federated users that belong to the specified domain and identity provider
     * @param domainId
     * @param identityProviderId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderId(String domainId, String identityProviderId);

    /**
     * Retrieve all federated users that belong to the specified identity provider
     * @param identityProviderId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersByIdentityProviderId(String identityProviderId);

    /**
     * Count federated users that belong to the specified domain and identity provider
     * @param domainId
     * @param identityProviderId
     * @return
     *
     * @deprecated - use unexpired version
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
     * Delete federated user
     */
    void deleteUser(FederatedUser federatedUser);

    /**
     * Get one expired federated user
     */
    FederatedUser getSingleExpiredFederatedUser();

    void doPreEncode(FederatedUser user);

    void doPostEncode(FederatedUser user);
}
