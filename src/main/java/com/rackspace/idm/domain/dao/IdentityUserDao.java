package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;

public interface IdentityUserDao  extends GenericDao<BaseUser> {
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
    public User getProvisionedUserById(String userId);

    /**
     * Search for a federated user with the specified userId
     *
     * @param userId
     * @return
     */
    public FederatedUser getFederatedUserById(String userId);

    /**
     * Search for end users with the specified domain id.
     *
     * @param domainId
     * @return
     */
    public Iterable<EndUser> getEndUsersByDomainId(String domainId);

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
}
