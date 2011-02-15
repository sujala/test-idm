package com.rackspace.idm.dao;

import com.rackspace.idm.entities.RefreshToken;
import org.joda.time.DateTime;

public interface RefreshTokenDao extends TokenDao<RefreshToken> {

    RefreshToken findTokenForOwner(String owner, String requestor, DateTime expiredAfter);

    void updateToken(RefreshToken refreshToken);

    void deleteTokenForUserByClientId(String username, String clientId);

    void deleteAllTokensForUser(String username);
}
