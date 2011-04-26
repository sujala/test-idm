package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccessObject;

public interface ScopeAccessObjectDao {

    ScopeAccessObject addScopeAccess(String parentUniqueId, ScopeAccessObject scopeAccess);

    Boolean addPermissionToScopeAccess(String scopeAccessUniqueId,
            Permission permission);

    Boolean deleteScopeAccess(ScopeAccessObject scopeAccess);

    Boolean doesAccessTokenHavePermission(String accessToken,
            Permission permission);

    List<ScopeAccessObject> getScopeAccessesByParent(String parentUniqueId);

    ScopeAccessObject getScopeAccessByAccessToken(String accessToken);

    ScopeAccessObject getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccessObject getScopeAccessForParentByClientId(String parentUniqueId,
            String clientId);

    ScopeAccessObject getScopeAccessByUsernameAndClientId(String username,
            String clientId);

    Boolean removePermissionFromScopeAccess(Permission permission);

    Boolean updateScopeAccess(ScopeAccessObject scopeAccess);

    Boolean updatePermissionForScopeAccess(String scopeAccessUniqueId, Permission permission);

}
