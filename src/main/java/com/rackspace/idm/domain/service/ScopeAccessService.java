package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspace.idm.util.AuthHeaderHelper;
import org.apache.commons.configuration.Configuration;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public interface ScopeAccessService {
        
    ScopeAccess addDirectScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);
    
    ScopeAccess addDelegateScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    ScopeAccess addImpersonatedScopeAccess(User user, String clientId, String impersonatingToken, ImpersonationRequest impersonationRequest);
    
    boolean authenticateAccessToken(String accessTokenStr);

    void deleteScopeAccess(ScopeAccess scopeAccess);
    
    void deleteScopeAccessesForParentByApplicationId(String parentUniqueId, String clientId);
    
    void deleteDelegatedToken(User user, String tokenString);
    
    boolean doesAccessTokenHaveService(ScopeAccess token, String clientId);
    
    void expireAccessToken(String tokenString) throws IOException, JAXBException;

    void expireAllTokensForClient(String clientId);

    void expireAllTokensForCustomer(String customerId) throws IOException, JAXBException;

    void expireAllTokensForUser(String username) throws IOException, JAXBException;
    
    void expireAllTokensForUserById(String userId) throws IOException, JAXBException;

    ScopeAccess getAccessTokenByAuthHeader(String authHeader);

    ClientScopeAccess getClientScopeAccessForClientId(String clientUniqueId, String clientId);

    PasswordResetScopeAccess getOrCreatePasswordResetScopeAccessForUser(User user);

    RackerScopeAccess getRackerScopeAccessForClientId(String rackerUniqueId, String clientId);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByUserId(String userId);

    List<ScopeAccess> getScopeAccessListByUserId(String userId);
    
    ScopeAccess loadScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getMostRecentDirectScopeAccessForParentByClientId(String parentUniqueID, String clientId);

    void updateUserScopeAccessTokenForClientIdByUser(User user, String clientId, String token, Date expires);

    UserScopeAccess getUserScopeAccessForClientId(String userUniqueId, String clientId);

    UserScopeAccess getValidUserScopeAccessForClientId(String userUniqueId, String clientId);

    RackerScopeAccess getValidRackerScopeAccessForClientId(String uniqueId, String rackerId, String clientId);
    
    DelegatedClientScopeAccess getDelegatedScopeAccessByRefreshToken(User user, String accessToken);
    
    List<DelegatedClientScopeAccess> getDelegatedUserScopeAccessForUsername(String userUniqueId);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndApiCredentials(String username, String apiKey, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(String username, String password, String clientId);

    void updateScopeAccess(ScopeAccess scopeAccess);

    DelegatedClientScopeAccess getScopeAccessByAuthCode(String authorizationCode);

    ScopeAccess getDelegateScopeAccessForParentByClientId(String parentUniqueID, String clientId);
    
    List<ScopeAccess> getDelegateScopeAccessesForParent(String parentUniqueId);

    List<ScopeAccess> getScopeAccessesForParentByClientId(String parentUniqueId, String clientId);

    List<ScopeAccess> getScopeAccessesForParent(String parentDn);

    List<OpenstackEndpoint> getOpenstackEndpointsForUser(User user);

    List<OpenstackEndpoint> getOpenstackEndpointsForScopeAccess(ScopeAccess scopeAccess);

    UserScopeAccess updateExpiredUserScopeAccess(String parentUniqueId, String clientId);

    UserScopeAccess updateExpiredUserScopeAccess(UserScopeAccess scopeAccess, boolean impersonated);

    boolean isScopeAccessExpired(ScopeAccess scopeAccess);
    
    void deleteScopeAccessByDn(String scopeAccessDn);

    DefinedPermission definePermission(String parentDn, DefinedPermission permission);

    @Deprecated
    List<Permission> getPermissionsForParent(String scopeAccessUniqueId);

    Permission getPermissionForParent(String parentUniqueId, Permission permission);

    List<Permission> getPermissionsByPermission(Permission permission);

    List<Permission> getPermissionsForParentByPermission(String parentDn, Permission permission);

    DelegatedPermission delegatePermission(String scopeAccessUniqueId, DelegatedPermission permission);

    boolean doesAccessTokenHavePermission(ScopeAccess token, Permission permission);

    boolean doesUserHavePermissionForClient(User user, Permission permission, Application client);

    GrantedPermission grantPermissionToClient(String parentUniqueId, GrantedPermission permission);

    GrantedPermission grantPermissionToUser(User user, GrantedPermission permission);
    
    void removePermission(Permission permission);

    void updatePermission(Permission permission);
}
