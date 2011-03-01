package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.AccessToken;

import java.util.Set;

public interface AccessTokenDao extends TokenDao<AccessToken> {
    AccessToken findTokenForOwner(String owner, String requestor);

    /**
     * Search by User's unique ID
     * 
     * @param owner Username in this case
     * @param tokenRequestors
     *            Set of client service IDs that can request access token for a
     *            user.
     */
    void deleteAllTokensForOwner(String owner, Set<String> tokenRequestors);
}
