package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;

import java.util.List;

public interface ScopeAccessDao {

    void addScopeAccess(UniqueId object, ScopeAccess scopeAccess);

    void deleteScopeAccess(ScopeAccess scopeAccess);

    void updateScopeAccess(ScopeAccess scopeAccess);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getMostRecentScopeAccessForUser(User user);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getMostRecentScopeAccessByClientId(UniqueId object, String clientId);

    ScopeAccess getMostRecentImpersonatedScopeAccessForUser(BaseUser user, String impersonatingUsername);

    Iterable<ScopeAccess> getScopeAccessesByUserId(String userId);

    Iterable<ScopeAccess> getScopeAccesses(UniqueId object);

    Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUser(BaseUser user);

    /**
     * Return all the impersonated scope acesses objects underneath the specified user that are impersonating
     * the specified impersonatingUsername
     *
     * @param user
     * @param impersonatingUsername
     * @return
     */
    Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUser(BaseUser user, String impersonatingUsername);

    Iterable<ScopeAccess> getScopeAccessesByClientId(UniqueId object, String clientId);

    String getClientIdForParent(ScopeAccess scopeAccess);

    String getUserIdForParent(ScopeAccess scopeAccess);
}
