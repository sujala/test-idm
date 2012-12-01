package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface ScopeAccessDao {
    
    ScopeAccess addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);
    
    ScopeAccess addDelegateScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);
    
    ScopeAccess addImpersonatedScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    ScopeAccess addDirectScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    DefinedPermission definePermission(String scopeAccessUniqueId,
        DefinedPermission permission);

    DelegatedPermission delegatePermission(String scopeAccessUniqueId,
        DelegatedPermission permission);

    boolean deleteScopeAccess(ScopeAccess scopeAccess);

    boolean doesAccessTokenHavePermission(ScopeAccess token, Permission permission);
    
    boolean doesParentHaveScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    Permission getPermissionByParentAndPermission(String parentUniqueId, Permission permission);

    List<Permission> getPermissionsByParentAndPermission(String parentUniqueId, Permission permission);
    
    List<Permission> getPermissionsByParent(String parentUniqueId);

    List<Permission> getPermissionsByPermission(Permission permission);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByUserId(String userId);
    List<ScopeAccess> getScopeAccessListByUserId(String userId);
    
    DelegatedClientScopeAccess getScopeAccessByAuthorizationCode(String authorizationCode);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getScopeAccessByUsernameAndClientId(String username, String clientId);
    
    List<DelegatedClientScopeAccess> getDelegatedClientScopeAccessByUsername(String username);

    List<ScopeAccess> getScopeAccessesByParent(String parentUniqueId);

    List<ScopeAccess> getDirectScopeAccessForParentByClientId(String parentUniqueId,
                                                              String clientId);

    ScopeAccess getMostRecentDirectScopeAccessForParentByClientId(String parentUniqueId,
                                                              String clientId);

    GrantedPermission grantPermission(String scopeAccessUniqueId,
        GrantedPermission permission);

    boolean removePermissionFromScopeAccess(Permission permission);
    
    boolean updatePermissionForScopeAccess(Permission permission);

    boolean updateScopeAccess(ScopeAccess scopeAccess);

    ScopeAccess getDelegateScopeAccessForParentByClientId(
        String parentUniqueId, String clientId);
    
    List<ScopeAccess> getDelegateScopeAccessesByParent(String parentUniqueId);

    ScopeAccess getImpersonatedScopeAccessForParentByClientId(String parentUniqueId, String username);

    ScopeAccess getScopeAccessByParentAndClientId(String parentUniqueId, String clientId);

    List<ScopeAccess> getScopeAccessesByParentAndClientId(
        String parentUniqueId, String clientId);

}
