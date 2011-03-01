package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.RefreshToken;

import org.joda.time.DateTime;

public interface RefreshTokenDao extends TokenDao<RefreshToken> {

    RefreshToken findTokenForOwner(String owner, String requestor, DateTime expiredAfter);

    void updateToken(RefreshToken refreshToken);

    void deleteTokenForUserByClientId(String username, String clientId);

    void deleteAllTokensForUser(String username);
}
