package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.api.resource.cloud.v20.ImpersonatorType;
import com.rackspace.idm.domain.entity.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;

public interface ScopeAccessService {

    /**
     * Adds a scoped access token for a BaseUser
     *
     * @param user
     * @param clientId
     * @param authenticatedBy
     * @param expirationSeconds
     * @param scope
     * @return
     */
    ScopeAccess addScopedScopeAccess(BaseUser user, String clientId, List<String> authenticatedBy, int expirationSeconds, String scope);

    void addUserScopeAccess(BaseUser user, ScopeAccess scopeAccess);

    void addApplicationScopeAccess(Application application, ScopeAccess scopeAccess);

    boolean authenticateAccessToken(String accessTokenStr);

    void deleteScopeAccess(ScopeAccess scopeAccess);

    void deleteScopeAccessesForApplication(Application application, String clientId);

    void deleteScopeAccessesForUser(User user, String clientId);

    /**
     * Delete a user's tokens - and error if there was an error deleting one. In general, the {@link #deleteExpiredTokensQuietly(com.rackspace.idm.domain.entity.EndUser)} should
     * be used in preference to this one.
     *
     * @param user
     */
    void deleteExpiredTokens(EndUser user);

    /**
     * Delete all the user's tokens quietly. An error trying to delete an expired token does not result in an error. It is simply logged.
     * @param user
     */
    void deleteExpiredTokensQuietly(EndUser user);

    /**
     * @deprecated Use appropriate method from TokenRevocationService
     */
    @Deprecated
    void expireAccessToken(String tokenString);

    void expireAllTokensForClient(String clientId);

    /**
     * @deprecated Use appropriate method from TokenRevocationService
     */
    @Deprecated
    void expireAllTokensForUser(String username);

    /**
     * @deprecated Use appropriate method from TokenRevocationService
     */
    @Deprecated
    void expireAllTokensForUserById(String userId);

    ScopeAccess getAccessTokenByAuthHeader(String authHeader);

    ClientScopeAccess getApplicationScopeAccess(Application application);

    RackerScopeAccess getRackerScopeAccessByClientId(Racker racker, String clientId);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessForUser(User user);

    Iterable<ScopeAccess> getScopeAccessListByUserId(String userId);

    ScopeAccess loadScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getMostRecentDirectScopeAccessForUserByClientId(User user, String clientId);

    UserScopeAccess getUserScopeAccessByClientId(User user, String clientId);

    UserScopeAccess getValidUserScopeAccessForClientId(User user, String clientId, List<String> authenticateBy);

    RackerScopeAccess getValidRackerScopeAccessForClientId(Racker racker, String clientId, List<String> authenticatedBy);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndApiCredentials(String username, String apiKey, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(String username, String password, String clientId);

    void updateScopeAccess(ScopeAccess scopeAccess);

    Iterable<ScopeAccess> getScopeAccessesForUserByClientId(User user, String clientId);

    Iterable<ScopeAccess> getScopeAccessesForApplicationByClientId(Application application, String clientId);

    Iterable<ScopeAccess> getScopeAccessesForUser(User user);

    Iterable<ScopeAccess> getScopeAccessesForApplication(Application application);

    List<OpenstackEndpoint> getOpenstackEndpointsForUser(User user);

    List<OpenstackEndpoint> getOpenstackEndpointsForScopeAccess(ScopeAccess scopeAccess);

    UserScopeAccess updateExpiredUserScopeAccess(User user, String clientId, List<String> authenticatedBy);

    boolean isScopeAccessExpired(ScopeAccess scopeAccess);

    void setApplicationService(ApplicationService applicationService);

    void setTenantService(TenantService tenantService);

    void setUserService(UserService userService);

    int getTokenExpirationSeconds(int value);

    String getClientIdForParent(ScopeAccess scopeAccess);

    UserScopeAccess createInstanceOfUserScopeAccess(EndUser user, String clientId, String clientRCN);

    /**
     * Processes an impersonation request and returns an appropriate ImpersonatedScopeAccess.
     *
     * @param impersonator
     * @param userBeingImpersonated
     * @param impersonationRequest
     * @param impersonatorType
     * @return
     */
    ImpersonatedScopeAccess processImpersonatedScopeAccessRequest(BaseUser impersonator, EndUser userBeingImpersonated, ImpersonationRequest impersonationRequest, ImpersonatorType impersonatorType, List<String> impersonatorAuthByMethods);

    boolean isSetupMfaScopedToken(ScopeAccess scopeAccess);
 }
