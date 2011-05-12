package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.PermissionEntity;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;

public interface ScopeAccessService {

    PermissionEntity addPermissionToScopeAccess(String scopeAccessUniqueId, PermissionEntity permission);

    ScopeAccess addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    boolean authenticateAccessToken(String accessTokenStr);

    void deleteScopeAccess(ScopeAccess scopeAccess);

    boolean doesAccessTokenHavePermission(String accessTokenString, PermissionEntity permission);

    void expireAccessToken(String tokenString);

    void expireAllTokensForClient(String clientId);

    void expireAllTokensForCustomer(String customerId);

    void expireAllTokensForUser(String username);

    ScopeAccess getAccessTokenByAuthHeader(String authHeader);

    ClientScopeAccess getClientScopeAccessForClientId(String clientUniqueId, String clientId);

    PasswordResetScopeAccess getOrCreatePasswordResetScopeAccessForUser(BaseUser user);

    PermissionEntity getPermissionForParent(String parentUniqueId, PermissionEntity permission);

    RackerScopeAccess getRackerScopeAccessForClientId(String rackerUniqueId, String clientId);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getScopeAccessForParentByClientId(String parentUniqueID, String clientId);

    UserScopeAccess getUserScopeAccessForClientId(String userUniqueId, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByMossoIdAndApiCredentials(int mossoId, String apiKey, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByNastIdAndApiCredentials(String nastId, String apiKey, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndApiCredentials(String username, String apiKey, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(String username, String password, String clientId);

    PermissionEntity grantPermissionToClient(String parentUniqueId, PermissionEntity permission);

    PermissionEntity grantPermissionToUser(User user, PermissionEntity permission);
    
    void removePermission(PermissionEntity permission);

    void updatePermission(PermissionEntity permission);

    void updateScopeAccess(ScopeAccess scopeAccess);
}
