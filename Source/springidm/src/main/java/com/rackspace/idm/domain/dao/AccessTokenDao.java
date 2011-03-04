package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.AccessToken;

public interface AccessTokenDao extends TokenDao<AccessToken> {
    AccessToken findTokenForOwner(String owner, String requestor);

    void deleteAllTokensForOwner(String owner);
}
