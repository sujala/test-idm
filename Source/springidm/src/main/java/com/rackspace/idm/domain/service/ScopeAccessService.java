package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;

public interface ScopeAccessService {
    
    void addScopeAccess(String parentUniqueId, ScopeAccessObject scopeAccess);
    
    boolean authenticateAccessToken(String accessTokenStr);
    
    boolean doesAccessTokenHavePermission(String accessTokenString, PermissionObject permission);
    
    void expireAccessToken(String tokenString);
    
    void expireAllTokensForClient(String clientId);

    void expireAllTokensForCustomer(String customerId);
    
    void expireAllTokensForUser(String username);
    
    ScopeAccessObject getAccessTokenByAuthHeader(String authHeader);
    
    ClientScopeAccessObject getClientScopeAccessForClientId(String clientUniqueId, String clientId);
    
    PasswordResetScopeAccessObject getOrCreatePasswordResetScopeAccessForUser(String userUniqueId);
    
    RackerScopeAccessObject getRackerScopeAccessForClientId(String rackerUniqueId, String clientId);
    
    ScopeAccessObject getScopeAccessByAccessToken(String accessToken);
    
    ScopeAccessObject getScopeAccessByRefreshToken(String refreshToken);
    
    UserScopeAccessObject getUserScopeAccessForClientId(String userUniqueId, String clientId);
    
    UserScopeAccessObject getUserScopeAccessForClientIdByUsernameAndApiCredentials(String username, String apiKey, String clientId);

    UserScopeAccessObject getUserScopeAccessForClientIdByMossoIdAndApiCredentials(int mossoId, String apiKey, String clientId);
    
    UserScopeAccessObject getUserScopeAccessForClientIdByNastIdAndApiCredentials(String nastId, String apiKey, String clientId);
    
    UserScopeAccessObject getUserScopeAccessForClientIdByUsernameAndPassword(String username, String password, String clientId);
    
    void updateScopeAccess(ScopeAccessObject scopeAccess);
}
