package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.User;

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
}
