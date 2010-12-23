package com.rackspace.idm.dao;

import java.util.Set;

import org.joda.time.DateTime;

import com.rackspace.idm.entities.RefreshToken;

public interface RefreshTokenDao extends GenericTokenDao {

    RefreshToken findTokenForOwner(String owner, String requestor,
        DateTime expiredAfter);

    void updateToken(RefreshToken refreshToken);

    void deleteAllTokensForUser(String user, Set<String> tokenRequestors);
}
