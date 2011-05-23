package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.DefinedPermission;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.DelegatedPermission;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;

public interface ScopeAccessDao {
    
//    ScopeAccess addScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);
    
    ScopeAccess addDelegateScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);
    
    ScopeAccess addDirectScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    DefinedPermission definePermission(String scopeAccessUniqueId,
        DefinedPermission permission);

    DelegatedPermission delegatePermission(String scopeAccessUniqueId,
        DelegatedPermission permission);

    boolean deleteScopeAccess(ScopeAccess scopeAccess);

    boolean doesAccessTokenHavePermission(ScopeAccess token,
            Permission permission);
    
    boolean doesParentHaveScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    Permission getPermissionByParentAndPermission(String parentUniqueId, Permission permission);

    List<Permission> getPermissionsByParentAndPermission(String parentUniqueId, Permission permission);

    List<Permission> getPermissionsByPermission(Permission permission);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    DelegatedClientScopeAccess getScopeAccessByAuthorizationCode(
        String authorizationCode);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getScopeAccessByUsernameAndClientId(String username,
            String clientId);
    
    List<DelegatedClientScopeAccess> getScopeAccessByUsername(String username);

    List<ScopeAccess> getScopeAccessesByParent(String parentUniqueId);

    ScopeAccess getDirectScopeAccessForParentByClientId(String parentUniqueId,
            String clientId);

    GrantedPermission grantPermission(String scopeAccessUniqueId,
        GrantedPermission permission);

    boolean removePermissionFromScopeAccess(Permission permission);
    
    boolean updatePermissionForScopeAccess(Permission permission);

    boolean updateScopeAccess(ScopeAccess scopeAccess);

    ScopeAccess getDelegateScopeAccessForParentByClientId(
        String parentUniqueId, String clientId);
    
    List<ScopeAccess> getDelegateScopeAccessesByParent(String parentUniqueId);
    
    ScopeAccess getScopeAccessByParentAndClientId(String parentUniqueId, String clientId);

    List<ScopeAccess> getScopeAccessesByParentAndClientId(
        String parentUniqueId, String clientId);
}
