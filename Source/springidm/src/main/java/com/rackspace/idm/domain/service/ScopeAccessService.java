package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;

public interface ScopeAccessService {
    
    boolean authenticateAccessToken(String accessTokenStr);
    
    boolean doesAccessTokenHavePermission(String accessTokenString, Permission permission);
    
    ClientScopeAccessObject getClientScopeAccessForClientId(String clientUniqueId, String clientId);
    
    RackerScopeAccessObject getRackerScopeAccessForClientId(String rackerUniqueId, String clientId);

    UserScopeAccessObject getUserScopeAccessForClientId(String userUniqueId, String clientId);
    
    ScopeAccessObject getScopeAccessByRefreshToken(String refreshToken);
    
    ScopeAccessObject getScopeAccessByAccessToken(String accessToken);
    
    PasswordResetScopeAccessObject getOrCreatePasswordResetScopeAccessForUser(String userUniqueId);
    
    void updateScopeAccess(ScopeAccessObject scopeAccess);
    
    void expireAccessToken(String tokenString);
    
    void expireAllTokensForUser(String username);
    
    void expireAllTokensForClient(String clientId);
    
    void expireAllTokensForCustomer(String customerId);

    ScopeAccessObject getAccessTokenByAuthHeader(String authHeader);
}
