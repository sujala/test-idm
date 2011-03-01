package com.rackspace.idm.domain.service;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.RefreshToken;

public interface RefreshTokenService {

    RefreshToken createRefreshTokenForUser(String username, String clientId);

    RefreshToken getRefreshTokenByTokenString(String tokenString);

    RefreshToken getRefreshTokenByUserAndClient(String username, String clientId, DateTime validAfter);

    void resetTokenExpiration(RefreshToken token);

    void deleteAllTokensForUser(String username);

    void deleteTokenForUserByClientId(String username, String clientId);
}
