package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.UserScopeAccess;

public interface FederatedUserDao {

    void addUser(IdentityProvider provider, FederatedUser user);

    FederatedUser getUserByToken(UserScopeAccess token);

    FederatedUser getUserByUsernameForIdentityProviderName(String username, String idp);

    FederatedUser getUserById(String id);

    Iterable<FederatedUser> getUsersByDomainId(String domainId);

    /**
     * Update the federated user. This will only update non-null values that have changed from the previous value.
     * @param user
     */
    void updateUser(FederatedUser user);
}
