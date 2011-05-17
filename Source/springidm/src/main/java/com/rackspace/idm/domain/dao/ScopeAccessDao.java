package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.DefinedPermission;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.DelegatedPermission;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;

public interface ScopeAccessDao {
    
    DelegatedPermission delegatePermission(String scopeAccessUniqueId,
        DelegatedPermission permission);

    ScopeAccess addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    GrantedPermission grantPermission(String scopeAccessUniqueId,
        GrantedPermission permission);

    DefinedPermission definePermission(String scopeAccessUniqueId,
        DefinedPermission permission);

    Boolean deleteScopeAccess(ScopeAccess scopeAccess);

    Boolean doesAccessTokenHavePermission(String accessToken,
            Permission permission);

    List<ScopeAccess> getScopeAccessesByParent(String parentUniqueId);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getScopeAccessForParentByClientId(String parentUniqueId,
            String clientId);

    ScopeAccess getScopeAccessByUsernameAndClientId(String username,
            String clientId);

    Boolean removePermissionFromScopeAccess(Permission permission);

    Boolean updateScopeAccess(ScopeAccess scopeAccess);

    Boolean updatePermissionForScopeAccess(Permission permission);

    Permission getPermissionByParentAndPermission(String parentUniqueId, Permission permission);

    List<Permission> getPermissionsByParentAndPermission(String parentUniqueId, Permission permission);
    
    List<Permission> getPermissionsByPermission(Permission permission);

    DelegatedClientScopeAccess getScopeAccessByAuthorizationCode(
        String authorizationCode);
}
