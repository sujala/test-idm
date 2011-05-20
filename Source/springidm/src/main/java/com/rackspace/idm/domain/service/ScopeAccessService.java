package com.rackspace.idm.domain.service;

import java.util.List;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.DelegatedPermission;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;

public interface ScopeAccessService {

    List<Permission> getPermissionsForParent(String scopeAccessUniqueId,
        Permission permission);
        
    ScopeAccess addDirectScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);
    
    ScopeAccess addDelegateScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    boolean authenticateAccessToken(String accessTokenStr);

    void deleteScopeAccess(ScopeAccess scopeAccess);

    boolean doesAccessTokenHavePermission(ScopeAccess token, Permission permission);
    
    boolean doesAccessTokenHaveService(ScopeAccess token, String clientId);

    void expireAccessToken(String tokenString);

    void expireAllTokensForClient(String clientId);

    void expireAllTokensForCustomer(String customerId);

    void expireAllTokensForUser(String username);

    ScopeAccess getAccessTokenByAuthHeader(String authHeader);

    ClientScopeAccess getClientScopeAccessForClientId(String clientUniqueId, String clientId);

    PasswordResetScopeAccess getOrCreatePasswordResetScopeAccessForUser(BaseUser user);

    Permission getPermissionForParent(String parentUniqueId, Permission permission);

    RackerScopeAccess getRackerScopeAccessForClientId(String rackerUniqueId, String clientId);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getDirectScopeAccessForParentByClientId(String parentUniqueID, String clientId);

    UserScopeAccess getUserScopeAccessForClientId(String userUniqueId, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByMossoIdAndApiCredentials(int mossoId, String apiKey, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByNastIdAndApiCredentials(String nastId, String apiKey, String clientId);

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
        Client client);
}
