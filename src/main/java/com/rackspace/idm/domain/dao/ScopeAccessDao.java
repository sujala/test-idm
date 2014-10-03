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

    /**
     * Return the most recent scope accesses object underneath the specified user that has the passed in clientId,
     * and match all authenticatedBy strings passed in. If authenticated by is null or empty then the most
     * recent scope access is returned regardless of its authenticated by state.
     *
     * @param object
     * @param clientId
     * @param authenticatedBy
     * @return
     */
    ScopeAccess getMostRecentScopeAccessByClientIdAndAuthenticatedBy(UniqueId object, String clientId, List<String> authenticatedBy);

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

}
