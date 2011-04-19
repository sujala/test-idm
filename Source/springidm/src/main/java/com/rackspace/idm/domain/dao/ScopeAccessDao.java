package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;

public interface ScopeAccessDao {

    void addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    void addPermissionToScopeAccess(String scopeAccessUniqueId,
        Permission permission);

    void deleteScopeAccess(ScopeAccess scopeAccess);

    boolean doesAccessTokenHavePermission(String accessToken,
        Permission permission);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getScopeAccessForParentByClientId(String parentUniqueId,
        String clientId);

    ScopeAccess getScopeAccessByUsernameAndClientId(String username,
        String clientId);

    void removePermissionFromScopeAccess(Permission permission);

    void updateScopeAccess(ScopeAccess scopeAccess);
}
