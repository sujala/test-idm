package com.rackspace.idm.dao;

import java.util.Set;

import com.rackspace.idm.entities.AccessToken;

public interface AccessTokenDao extends GenericTokenDao {
    AccessToken findTokenForOwner(String owner, String requestor);

    /**
     * Search by User's unique ID
     * 
     * @param userName
     * @param tokenRequestors
     *            Set of client service IDs that can request access token for a
     *            user.
     */
    void deleteAllTokensForOwner(String userName, Set<String> tokenRequestors);
}
