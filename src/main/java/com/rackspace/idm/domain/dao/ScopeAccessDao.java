package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface ScopeAccessDao {

    void addScopeAccess(UniqueId object, ScopeAccess scopeAccess);

    void deleteScopeAccess(ScopeAccess scopeAccess);

    void updateScopeAccess(ScopeAccess scopeAccess);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getMostRecentScopeAccessForUser(User user);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getMostRecentScopeAccessByClientId(UniqueId object, String clientId);

    ScopeAccess getMostRecentImpersonatedScopeAccessForUser(User user, String impersonatingUsername);

    List<ScopeAccess> getScopeAccessesByUserId(String userId);

    List<ScopeAccess> getScopeAccesses(UniqueId object);

    List<ScopeAccess> getAllImpersonatedScopeAccessForUser(User user);

    List<ScopeAccess> getScopeAccessesByClientId(UniqueId object, String clientId);

    String getClientIdForParent(ScopeAccess scopeAccess);
}
