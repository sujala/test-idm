package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;

public interface ScopeAccessObjectDao {

    ScopeAccessObject addScopeAccess(String parentUniqueId, ScopeAccessObject scopeAccess);

    Boolean addPermissionToScopeAccess(String scopeAccessUniqueId,
            PermissionObject permission);

    Boolean deleteScopeAccess(ScopeAccessObject scopeAccess);

    Boolean doesAccessTokenHavePermission(String accessToken,
            PermissionObject permission);

    List<ScopeAccessObject> getScopeAccessesByParent(String parentUniqueId);

    ScopeAccessObject getScopeAccessByAccessToken(String accessToken);

    ScopeAccessObject getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccessObject getScopeAccessForParentByClientId(String parentUniqueId,
            String clientId);

    ScopeAccessObject getScopeAccessByUsernameAndClientId(String username,
            String clientId);

    Boolean removePermissionFromScopeAccess(PermissionObject permission);

    Boolean updateScopeAccess(ScopeAccessObject scopeAccess);

    Boolean updatePermissionForScopeAccess(PermissionObject permission);

    PermissionObject getPermissionByParentAndPermissionId(String parentUniqueId, String permissionId);

}
