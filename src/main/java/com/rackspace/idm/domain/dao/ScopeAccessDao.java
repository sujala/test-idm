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

    /**
     * @deprecated Use {@link #getMostRecentImpersonatedScopeAccessForUserRsIdAndAuthenticatedBy(com.rackspace.idm.domain.entity.BaseUser, String, java.util.List)}
     */
    @Deprecated
    ScopeAccess getMostRecentImpersonatedScopeAccessForUserRsId(BaseUser user, String rsImpersonatingRsId);

    /**
     * <p>Return the most recent impersonated scope access underneath the specified user for the specified impersonated user
     * that has the passed in clientId, and match all authenticatedBy strings passed in.
     * </p>
     * <p>If authenticated by is null or empty then the most
     * recent scope access is returned regardless of its authenticated by state.
     *</p>
     *
     * @param user
     * @param rsImpersonatingRsId
     * @param authenticatedBy
     * @return
     */
    ScopeAccess getMostRecentImpersonatedScopeAccessForUserRsIdAndAuthenticatedBy(BaseUser user, String rsImpersonatingRsId, List<String> authenticatedBy);

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

    Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUserByRsId(BaseUser user, String rsImpersonatingRsId);

    /**
     * Return all the impersonated scope acesses objects underneath the specified user that are impersonating
     * the specified impersonatingUsername that do not have the rsImpersonatingRsId attribute.
     * This method is now deprecated due to the fact that impersonation tokens now have the impersonated user's rsId.
     * Identifying impersonation tokens using the impersonated user's username is no longer valid due to the fact that
     * federated users are now possible to be impersonated and their usernames are only unique within their IDP.
     *
     * @param user
     * @param impersonatingUsername
     * @return
     */
    @Deprecated
    Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUserByUsername(BaseUser user, String impersonatingUsername);

    /**
     * Return the most recent impersonated scope acesses object underneath the specified user that are impersonating
     * the specified impersonatingUsername that do not have the rsImpersonatingRsId attribute.
     * This method is now deprecated due to the fact that impersonation tokens now have the impersonated user's rsId.
     * Identifying impersonation tokens using the impersonated user's username is no longer valid due to the fact that
     * federated users are now possible to be impersonated and their usernames are only unique within their IDP.
     *
     * @param user
     * @param impersonatingUsername
     * @return
     */
    @Deprecated
    ScopeAccess getMostRecentImpersonatedScopeAccessForUserOfUser(BaseUser user, String impersonatingUsername);

    Iterable<ScopeAccess> getScopeAccessesByClientId(UniqueId object, String clientId);

    String getClientIdForParent(ScopeAccess scopeAccess);

}
