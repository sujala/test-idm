package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;

public interface ScopeAccessDao {

    void addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    void addPermissionToScopeAccess(String scopeAccessUniqueId,
        Permission permission);

    void deleteScopeAccess(ScopeAccess scopeAccess);

    boolean doesAccessTokenHavePermission(String accessToken,
        Permission permission);
    
    List<ScopeAccess> getScopeAccessesByParent(String parentUniqueId);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getScopeAccessForParentByClientId(String parentUniqueId,
        String clientId);

    ScopeAccess getScopeAccessByUsernameAndClientId(String username,
        String clientId);

    void removePermissionFromScopeAccess(Permission permission);

    void updateScopeAccess(ScopeAccess scopeAccess);

    void updatePermissionForScopeAccess(String scopeAccessUniqueId, Permission permission);
}
