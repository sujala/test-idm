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
     * Returns the user associated with the specified id. The user identified by the userId must represent an EndUser.
     *
     * @param userId
     * @return
     *
     * @throws com.rackspace.idm.exception.NotFoundException if no enduser matches the specified userId
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
     * Search for a federated user with the specified username within the specified idp. Returns null if no user was found.
     *
     * @param username
     * @param idpName
     * @return
     */
    FederatedUser getFederatedUserByUsernameAndIdentityProviderName(String username, String idpName);

    /**
     * Search for a federated user with the specified username within the specified idp. Throws NotFoundException if the user is not found.
     * This does not distinguish between an invalid idp name and an invalid username. The message in the exception will simply specify that the
     * user was not found.
     *
     * @param username
     * @param idpName
     * @return
     */
    FederatedUser checkAndGetFederatedUserByUsernameAndIdentityProviderName(String username, String idpName);

    /**
     * Search for a federated user with the specified username within the specified idp. Throws NotFoundException if the user is not found.
     * This does not distinguish between an invalid idp URI and an invalid username. The message in the exception will simply specify that the
     * user was not found.
     *
     * @param username
     * @param idpUri
     * @return
     */
    FederatedUser checkAndGetFederatedUserByUsernameAndIdentityProviderUri(String username, String idpUri);

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
     * Returns all federated and provisioned users associated with the specified domain based on their
     * enabled attribute. Note: Federated users do not have an enabled attribute. If the federated user
     * exists in the directory, then the user is enabled.
     *
     * @param domainId
     * @param enabled
     * @return
     */
    Iterable<EndUser> getEndUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled);

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
     * Return all
     * @param domainId
     * @return
     */
    Iterable<User> getProvisionedUsersByDomainId(String domainId);

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

    /**
     * Delete a user from a group. endUserId must resolve to EndUser. No-Op if no user is found for the given id
     *
     * @param groupId
     * @param endUserId
     */
    void addGroupToEndUser(String groupId, String endUserId);

    /**
     * Remove a user from a group. endUserId must resolve to EndUser. No-Op if no user is found for the given id
     *
     * @param groupId
     * @param endUserId
     */
    void removeGroupFromEndUser(String groupId, String endUserId);

    /**
     * Retrieve all the enabled end users that are associated with the specified group
     *
     * @param groupId
     * @return
     */
    Iterable<EndUser> getEnabledEndUsersByGroupId(String groupId);
}
