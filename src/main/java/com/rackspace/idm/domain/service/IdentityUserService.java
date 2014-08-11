package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;

public interface IdentityUserService {
    /**
     * Returns the user associated with the specified id. The user identified by the userId must represent an EndUser. Returns null if no
     * enduser matches the specified userId
     *
     * @param userId
     * @return
     */
    EndUser getEndUserById(String userId);

    /**
     * Returns the user associated with the specified id. The user identified by the userId must represent an EndUser. Throws
     * NotFoundException if no enduser matches the specified userId
     *
     * @param userId
     * @return
     */
    EndUser checkAndGetEndUserById(String userId);

    /**
     * Search for a provisioned user with the specified userId. Returns null if no provisioned user matches the userId.
     *
     * @param userId
     * @return
     */
    User getProvisionedUserById(String userId);

    /**
     * Search for a federated user with the specified userId. Returns null if no federated user matches the userId.
     *
     * @param userId
     * @return
     */
    FederatedUser getFederatedUserById(String userId);

    /**
     * Returns all federated users that are contained within the given identity provider and have the given domain ID.
     *
     * @param domainId
     * @param idpName
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderName(String domainId, String idpName);

    /**
     * Returns the number of federated users that are contained within the given identity provider and have the given domain ID.
     *
     * @param domainId
     * @param idpName
     * @return
     */
    int getFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String idpName);

    /**
     * Returns all federated and provisioned users associated with the specified domain.
     *
     * @param domainId
     * @return
     */
    Iterable<EndUser> getEndUsersByDomainId(String domainId);

    /**
     * Returns a page of federated and provisioned users associated with the specified domain.
     *
     * @param domainId
     * @param offset
     * @param limit
     * @return
     */
    PaginatorContext<EndUser> getEndUsersByDomainIdPaged(String domainId, int offset, int limit);

    /**
     * Returns a page of federated and provisioned users associated with the specified domain.
     * Provisioned users are only returned if they are enabled.
     *
     * NOTE: all federated users are currently returned. This included federated users that
     * are within a domain that is considered disabled (domain is disabled or all user-admins
     * for domain are disabled). This will be fixed in a future story.
     *
     * @param offset
     * @param limit
     * @return
     */
    PaginatorContext<EndUser> getEnabledEndUsersPaged(int offset, int limit);

    /**
     * Returns all groups for a federated or provisioned user.
     *
     * @param userId
     * @return
     */
    Iterable<Group> getGroupsForEndUser(String userId);

    /**
     * Returns the user associated with the specified id. The user identified by the userId must represent an EndUser.
     * Throws NotFoundException if no enduser matches the specified userId
     *
     * @param userId
     * @return
     */
    EndUser checkAndGetUserById(String userId);
}
