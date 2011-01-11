package com.rackspace.idm.dao;

import com.rackspace.idm.entities.RefreshToken;
import org.joda.time.DateTime;

import java.util.Set;

public interface RefreshTokenDao extends GenericTokenDao<RefreshToken> {

    RefreshToken findTokenForOwner(String owner, String requestor,
                                   DateTime expiredAfter);

    void updateToken(RefreshToken refreshToken);

    void deleteAllTokensForUser(String user, Set<String> tokenRequestors);
}
