package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.IdentityProvider;

/**
 * Common operations for all external identity provider persistence mechanisms
 * @param <T>
 */
public interface FederatedBaseUserDao<T extends BaseUser> {

    /**
     * Add the user to the external provider
     *
     * @param provider
     * @param user
     */
    void addUser(IdentityProvider provider, T user);

    /**
     * Retrieve the user associated with the given token, or null if no such user exists.
     *
     * @param token
     * @return
     */
    T getUserByToken(BaseUserToken token);

    /**
     * Retrieve the federated user of the appropriate type based on the id.
     *
     * @param id
     * @return
     */
    T getUserById(String id);

    /**
     * Retrieve the appropriate users by username for the specified identity provider with the given id
     * @param username
     * @param identityProviderId
     * @return
     */
    T getUserByUsernameForIdentityProviderId(String username, String identityProviderId);

    /**
     * Update the user. This will only update non-null values that have changed from the previous value. Not all
     * FederatedDaos may support this.
     *
     * @param user
     */
    void updateUser(T user);

    /**
     * Update the user. This will update ALL values that have changed from the previous value. Not all
     * FederatedDaos may support this.
     *
     * @param user
     */
    void updateUserAsIs(T user);
}
