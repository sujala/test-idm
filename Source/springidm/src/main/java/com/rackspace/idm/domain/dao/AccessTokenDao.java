package com.rackspace.idm.domain.dao;

import java.util.Set;

import com.rackspace.idm.domain.entity.AccessToken;

public interface AccessTokenDao extends TokenDao<AccessToken> {
    AccessToken findTokenForOwner(String owner, String requestor);

    @Deprecated
    void deleteAllTokensForOwner(String owner, Set<String> tokenRequestors);

    void deleteAllTokensForOwner(String owner);
}
