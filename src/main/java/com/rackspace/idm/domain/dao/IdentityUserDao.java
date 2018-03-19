package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.unboundid.ldap.sdk.DN;

import java.util.List;

public interface IdentityUserDao {

    /**
     * Search for an end user with the specified id. The user identified by the userId must represent an EndUser (e.g. - provisioned or federated).
     *
     * @param userId
     * @return
     */
    EndUser getEndUserById(String userId);

    /**
     * Search for a provisioned user with the specified userId
     *
     * @param userId
     * @return
     */
    User getProvisionedUserById(String userId);


    /**
     * Search for a provisioned user with the specified userId with password history
     *
     * @param userId
     * @return
     */
    User getProvisionedUserByIdWithPwdHis(String userId);


    /**
     * Search for a federated user with the specified userId
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
     * Search for a federated user with the specified username and idpId
     *
     * @param username
     * @param idpId
     * @return
     */
    FederatedUser getFederatedUserByUsernameAndIdpId(String username, String idpId);

    /**
     * Search for federated users with the specified domain id and identity provider id
     *
     * @param domainId
     * @param idpId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderId(String domainId, String idpId);

    /**
     * Search for federated users within the specified identity provider id
     *
     * @param idpId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersByIdentityProviderId(String idpId);

    /**
     * Search for federated users not in approvedDomainIds for identity provider
     *
     * @param approvedDomainIds
     * @param idpId
     * @return
     */
    Iterable<FederatedUser> getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(List<String> approvedDomainIds, String idpId);

    /**
     * Search for the number of federated users with the specified domain id and identity provider id
     *
     * @param domainId
     * @param idpId
     * @return
     *
     * @deprecated Use unexpired version
     */
    int getFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String idpId);

    /**
     * Count unexpired federated users that belong to the specified domain and identity provider
     *
     * @param domainId
     * @param identityProviderId
     * @return
     */
    int getUnexpiredFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String identityProviderId);

    /**
     * Search for end users with the specified domain id.
     *
     * @param domainId
     * @return
     */
    public Iterable<EndUser> getEndUsersByDomainId(String domainId);

    /**
     * Search for end users with the specified domain id and enabled attribute
     *
     * @param domainId
     * @return
     */
    Iterable<EndUser> getEndUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled);

    /**
     * Search for end users with the specified domain id.
     *
     * @param domainId
     * @param offset
     * @param limit
     * @return
     */
    PaginatorContext<EndUser> getEndUsersByDomainIdPaged(String domainId, int offset, int limit);

    /**
     * Search for end users with the specified domain id.
     *
     * @param offset
     * @param limit
     * @return
     */
    PaginatorContext<EndUser> getEnabledEndUsersPaged(int offset, int limit);

    /**
     * Get groups for an end user
     *
     * @param userId
     * @return
     */
    Iterable<Group> getGroupsForEndUser(String userId);

    /**
     * Get all enabled end users that are associated with the specified group
     *
     * @param groupId
     * @return
     */
    Iterable<EndUser> getEnabledEndUsersByGroupId(String groupId);

    /**
     * Updates the user
     *
     * @param baseUser
     */
    void updateIdentityUser(BaseUser baseUser);

    /**
     * Delete the user
     */
    void deleteIdentityUser(BaseUser baseUser);

    /**
     * Gets the count of federated and provisioned users within a given region (by name)
     */
    int getUsersWithinRegionCount(String regionName);

    /**
     * Search for end users with the specified user group and userSearchCriteria.
     *
     * @param group
     * @param userSearchCriteria
     * @return
     */
    PaginatorContext<EndUser> getEndUsersInUserGroupPaged(UserGroup group, UserSearchCriteria userSearchCriteria);

    /**
     * Search for end users with the specified user group.
     *
     * @param group
     * @return
     */
    Iterable<EndUser> getEndUsersInUserGroup(UserGroup group);
}
