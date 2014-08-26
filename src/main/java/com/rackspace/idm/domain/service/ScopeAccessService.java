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

    void expireAccessToken(String tokenString);

    void expireAllTokensForClient(String clientId);

    void expireAllTokensForUser(String username);

    void expireAllTokensForUserById(String userId);

    /**
     * Expire all valid tokens for the user that do NOT have one of the specified authenticatedBy attribute. The token's authenticatedBy must
     * only contain the specified value(s) in order to not be revoked. The order of the authenticated by does
     * not matter. The boolean keepEmpty parameter specifies whether or not to revoke tokens which do not contain any value for authenticated by.
     *
     * <p>
     *     For example, if the specified authenticatedBy=PASSCODE, PASSWORD then:
     *     <ul>
     *         <li>a token with 2 values for authenticatedBy (PASSWORD, PASSCODE) would NOT be revoked</li>
     *         <li>a token with 1 values for authenticatedBy (PASSWORD) would be revoked</li>
     *         <li>a token with 0 values for authenticatedBy would be revoked (this would NOT be revoked if keepEmpty is true)</li>
     *     </ul>
     * </p>
     *
     * @param user
     * @param keepAuthenticatedByOptions
     */
    void expireAllTokensExceptTypeForEndUser(EndUser user, List<List<String>> keepAuthenticatedByOptions, boolean keepEmpty);

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

    String getUserIdForParent(ScopeAccess scopeAccessByAccessToken);

    UserScopeAccess createInstanceOfUserScopeAccess(User user, String clientId, String clientRCN);

    /**
     * Processes an impersonation request and returns an appropriate ImpersonatedScopeAccess.
     *
     * @param impersonator
     * @param userBeingImpersonated
     * @param impersonationRequest
     * @param impersonatorType
     * @return
     */
    ImpersonatedScopeAccess processImpersonatedScopeAccessRequest(BaseUser impersonator, User userBeingImpersonated, ImpersonationRequest impersonationRequest, ImpersonatorType impersonatorType);

    boolean isSetupMfaScopedToken(ScopeAccess scopeAccess);
 }
