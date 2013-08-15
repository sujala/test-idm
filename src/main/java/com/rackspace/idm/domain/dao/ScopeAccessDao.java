package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface ScopeAccessDao {
    
    void addScopeAccess(UniqueId object, ScopeAccess scopeAccess);

    void deleteScopeAccess(ScopeAccess scopeAccess);

    boolean doesParentHaveScopeAccess(String parentUniqueId, ScopeAccess scopeAccess);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByUserId(String userId);
    List<ScopeAccess> getScopeAccessListByUserId(String userId);
    
    DelegatedClientScopeAccess getDelegatedScopeAccessByAuthorizationCode(String authorizationCode);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    List<DelegatedClientScopeAccess> getDelegatedClientScopeAccessByUsername(String username);

    List<ScopeAccess> getScopeAccesses(UniqueId object);

    List<ScopeAccess> getDirectScopeAccessForParentByClientId(String parentUniqueId, String clientId);

    ScopeAccess getMostRecentScopeAccessByClientId(UniqueId object, String clientId);

    void updateScopeAccess(ScopeAccess scopeAccess);

    List<ScopeAccess> getAllImpersonatedScopeAccessForParentByUser(String parentUniqueId, String username);

    List<ScopeAccess> getAllImpersonatedScopeAccessForUser(User user);

    ScopeAccess getMostRecentImpersonatedScopeAccessByParentForUser(String parentUniqueId, String username);

    List<ScopeAccess> getScopeAccessesByParentAndClientId(String parentUniqueId, String clientId);
}
