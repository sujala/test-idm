package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.PermissionEntity;
import com.rackspace.idm.domain.entity.ScopeAccess;

public interface ScopeAccessDao {

    ScopeAccess addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    PermissionEntity grantPermission(String scopeAccessUniqueId,
            PermissionEntity permission);

    PermissionEntity definePermission(String scopeAccessUniqueId,
            PermissionEntity permission);

    Boolean deleteScopeAccess(ScopeAccess scopeAccess);

    Boolean doesAccessTokenHavePermission(String accessToken,
            PermissionEntity permission);

    List<ScopeAccess> getScopeAccessesByParent(String parentUniqueId);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getScopeAccessForParentByClientId(String parentUniqueId,
            String clientId);

    ScopeAccess getScopeAccessByUsernameAndClientId(String username,
            String clientId);

    Boolean removePermissionFromScopeAccess(PermissionEntity permission);

    Boolean updateScopeAccess(ScopeAccess scopeAccess);

    Boolean updatePermissionForScopeAccess(PermissionEntity permission);

    PermissionEntity getPermissionByParentAndPermissionId(String parentUniqueId, PermissionEntity permission);

    List<PermissionEntity> getPermissionsByParentAndPermissionId(String parentUniqueId, PermissionEntity permission);
    
    List<PermissionEntity> getPermissionsByPermission(PermissionEntity permission);
}
