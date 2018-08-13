package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.domain.entity.User.UserType;
import com.unboundid.ldap.sdk.DN;

import java.util.List;

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
     * Search for provisioned users with the specified domain and email address. Returns null if no provisioned user
     * matches the domain and email address.
     *
     * @param domainId
     * @param email
     * @return
     */
    Iterable<User> getProvisionedUsersByDomainIdAndEmail(String domainId, String email);

    /**
     * Search for a provisioned user with the specified userId with password history.
     * Returns null if no provisioned user matches the userId.
     *
     * @param userId
     * @return
     */
    User getProvisionedUserByIdWithPwdHis(String userId);


    /**
     * Search for a federated user with the specified username within the specified idp. Returns null if no user was found.
     *
     * @param username
     * @param idpName
     * @return
     */
    FederatedUser getFederatedUserByUsernameAndIdentityProviderId(String username, String idpName);

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
     * Retrieves a federated user by DN
     *
     * @param dn
     * @return
     */
    FederatedUser getFederatedUserByDn(DN dn);

    /**
     * Returns all federated users that are contained within the given identity provider.
     *
     * @param idpId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersByIdentityProviderId(String idpId);

    /**
     * Retrieve all federated users that do not belong to the approved domains in the identity provider
     *
     * @param approvedDomainIds
     * @param idpId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(List<String> approvedDomainIds, String idpId);

    /**
     * Returns the number of federated users that are contained within the given identity provider and have the given domain ID.
     *
     * @param domainId
     * @param idpName
     * @return
     *
     * @deprecated - Use unexpired version
     */
    int getFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String idpName);

    /**
     * Count unexpired federated users that belong to the specified domain and identity provider
     *
     * @param domainId
     * @param identityProviderId
     * @return
     */
    int getUnexpiredFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String identityProviderId);

    /**
     * Returns all federated and provisioned users associated with the specified domain.
     *
     * @param domainId
     * @param userType (Verified, Unverified, All from enum)
     * @return
     */
    Iterable<EndUser> getEndUsersByDomainId (String domainId, UserType userType);

    /**
     * Returns all federated and provisioned users associated with the specified domain based on their
     * enabled attribute. Note: Federated users do not have an enabled attribute. If the federated user
     * exists in the directory, then the user is enabled.
     *
     * @param domainId
     * @param enabled
     * @param userType
     * @return
     */
    Iterable<EndUser> getEndUsersByDomainIdAndEnabledFlag (String domainId, boolean enabled, UserType userType);

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

    /**
     * Delete the specified user
     *
     * @param baseUser
     */
    void deleteUser(BaseUser baseUser);

    /**
     * Add group to user
     *
     * @param group
     * @param baseUser
     */
    void addUserGroupToUser(UserGroup group, User baseUser);

     /**
     * Remove group from user
     *
      * @param group
      * @param baseUser
      */
    void removeUserGroupFromUser(UserGroup group, User baseUser);

    /**
     * Returns a page of federated and provisioned users associated with a user group. If no users
     * are found, a context with an empty list of results will be returned.
     *
     * @param group
     * @param userSearchCriteria
     * @return
     */
    PaginatorContext<EndUser> getEndUsersInUserGroupPaged(UserGroup group, UserSearchCriteria userSearchCriteria);

    /**
     * Returns users associated with a user group. If no users are found, a non-null iterable with 0 elements is returned.
     *
     * @param group
     * @return
     */
    Iterable<EndUser> getEndUsersInUserGroup(UserGroup group);

    /**
     * Gets the count of federated and provisioned users within a given region (by name)
     */
    int getUsersWithinRegionCount(String regionName);

    /**
     * Retrieve the service catalog info for a user. This is the more performant version of that found in the ScopeAccessService
     *
     * @param baseUser
     * @return
     */
    ServiceCatalogInfo getServiceCatalogInfo(BaseUser baseUser);

    /**
     * Similar to {@link #getServiceCatalogInfo(BaseUser)}, except the RCN Role logic is applied to the user's roles.
     * When applied all non-RCN global roles are granted on all tenants within the user's domain and RCN roles are applied
     * across domains within the RCN.
     *
     * @param baseUser
     * @return
     */
    ServiceCatalogInfo getServiceCatalogInfoApplyRcnRoles(BaseUser baseUser);

    /**
     * Update a federated user. Currently only contactId is allowed to be updated on a federated user.
     *
     * @param user
     */
    void updateFederatedUser(FederatedUser user);
}


