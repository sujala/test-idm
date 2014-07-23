package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.User;

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
     * Search for a provisioned user with the specified userId. Returns null if no provisioned user matches the userId.
     *
     * @param userId
     * @return
     */
    public User getProvisionedUserById(String userId);

    /**
     * Search for a federated user with the specified userId. Returns null if no federated user matches the userId.
     *
     * @param userId
     * @return
     */
    public FederatedUser getFederatedUserById(String userId);
}
