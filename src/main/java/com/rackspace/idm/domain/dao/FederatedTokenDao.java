package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.UserScopeAccess;

/**
 * @deprecated Use normal ldapscopeaccessrepository
 */
@Deprecated
public interface FederatedTokenDao {

    public Iterable<UserScopeAccess> getFederatedTokensByUserId(String userId);

}
