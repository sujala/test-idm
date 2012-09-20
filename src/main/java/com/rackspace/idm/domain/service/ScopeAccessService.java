package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.domain.entity.*;

import java.util.Date;
import java.util.List;

public interface ScopeAccessService {

    List<Permission> getPermissionsForParent(String scopeAccessUniqueId,
        Permission permission);
    
    List<Permission> getPermissionsForParent(String scopeAccessUniqueId);
        
    ScopeAccess addDirectScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);
    
    ScopeAccess addDelegateScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    ScopeAccess addImpersonatedScopeAccess(User user, String clientId, String impersonatingToken, ImpersonationRequest impersonationRequest);
    
    ScopeAccess addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    boolean authenticateAccessToken(String accessTokenStr);

    void deleteScopeAccess(ScopeAccess scopeAccess);
    
    void deleteScopeAccessesForParentByApplicationId(String parentUniqueId, String clientId);
    
    void deleteDelegatedToken(User user, String tokenString);

    boolean doesAccessTokenHavePermission(ScopeAccess token, Permission permission);
    
    boolean doesAccessTokenHaveService(ScopeAccess token, String clientId);
    
    void expireAccessToken(String tokenString);

    void expireAllTokensForClient(String clientId);

    void expireAllTokensForCustomer(String customerId);

    void expireAllTokensForUser(String username);
    
    void expireAllTokensForUserById(String userId);

    ScopeAccess getAccessTokenByAuthHeader(String authHeader);

    ClientScopeAccess getClientScopeAccessForClientId(String clientUniqueId, String clientId);

    PasswordResetScopeAccess getOrCreatePasswordResetScopeAccessForUser(User user);

    Permission getPermissionForParent(String parentUniqueId, Permission permission);

    RackerScopeAccess getRackerScopeAccessForClientId(String rackerUniqueId, String clientId);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByUserId(String userId);

    List<ScopeAccess> getScopeAccessListByUserId(String userId);
    
    ScopeAccess loadScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getDirectScopeAccessForParentByClientId(String parentUniqueID, String clientId);

    void updateUserScopeAccessTokenForClientIdByUser(User user, String clientId, String token, Date expires);

    UserScopeAccess getUserScopeAccessForClientId(String userUniqueId, String clientId);

    UserScopeAccess getValidUserScopeAccessForClientId(String userUniqueId, String clientId);
    
    DelegatedClientScopeAccess getDelegatedScopeAccessByRefreshToken(User user, String accessToken);
    
    List<DelegatedClientScopeAccess> getDelegatedUserScopeAccessForUsername(String userUniqueId);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndApiCredentials(String username, String apiKey, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(String username, String password, String clientId);

    GrantedPermission grantPermissionToClient(String parentUniqueId, GrantedPermission permission);

    GrantedPermission grantPermissionToUser(User user, GrantedPermission permission);
    
    void removePermission(Permission permission);

    void updatePermission(Permission permission);

    void updateScopeAccess(ScopeAccess scopeAccess);

    DelegatedClientScopeAccess getScopeAccessByAuthCode(String authorizationCode);

    DelegatedPermission delegatePermission(String scopeAccessUniqueId, DelegatedPermission permission);

    ScopeAccess getDelegateScopeAccessForParentByClientId(
        String parentUniqueID, String clientId);
    
    List<ScopeAccess> getDelegateScopeAccessesForParent(String parentUniqueId);

    List<ScopeAccess> getScopeAccessesForParentByClientId(String parentUniqueId,
        String clientId);

    boolean doesUserHavePermissionForClient(User user, Permission permission,
        Application client);

    List<OpenstackEndpoint> getOpenstackEndpointsForScopeAccess(ScopeAccess scopeAccess);

    UserScopeAccess updateExpiredUserScopeAccess(UserScopeAccess scopeAccess);

    UserScopeAccess updateExpiredUserScopeAccess(UserScopeAccess scopeAccess, boolean impersonated);
}
