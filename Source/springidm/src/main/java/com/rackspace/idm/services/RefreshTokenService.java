package com.rackspace.idm.services;

import com.rackspace.idm.entities.RefreshToken;
import org.joda.time.DateTime;

public interface RefreshTokenService {

    RefreshToken createRefreshTokenForUser(String username, String clientId);

    RefreshToken getRefreshTokenByTokenString(String tokenString);

    RefreshToken getRefreshTokenByUserAndClient(String username, String clientId, DateTime validAfter);

    void resetTokenExpiration(RefreshToken token);
}
