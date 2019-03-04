package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.api.resource.cloud.v20.ImpersonatorType;
import com.rackspace.idm.domain.entity.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Date;
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

    /**
     * Adds a scoped access token for a BaseUser with a specific expiration date
     *
     * @param user
     * @param clientId
     * @param authenticatedBy
     * @param expirationDate
     * @param scope
     * @return
     */
    ScopeAccess addScopedScopeAccess(BaseUser user, String clientId, List<String> authenticatedBy, Date expirationDate, String scope);

    /**
     * Use on of the add
     * @param user
     * @param scopeAccess
     */
    @Deprecated
    void addUserScopeAccess(BaseUser user, ScopeAccess scopeAccess);

    boolean authenticateAccessToken(String accessTokenStr);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    /**
     * Converts the token into a ScopeAccess object, returning null if the provided tokenString is invalid, doesn't resolve
     * to a real identity token, is expired, or is revoked.
     * @param tokenString
     * @return
     */
    ScopeAccess unmarshallScopeAccess(String tokenString);

    RackerScopeAccess getValidRackerScopeAccessForClientId(Racker racker, String clientId, List<String> authenticatedBy);

    /**
     * This is a legacy method used by v1.1 API. It should not be used for new code.
     *
     * @param username
     * @param password
     * @param clientId
     * @return
     */
    @Deprecated
    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(String username, String password, String clientId);

    List<OpenstackEndpoint> getOpenstackEndpointsForUser(User user);

    /**
     * Returns the ServiceCatalogInfo for the given user. This object contains all of the tenant roles, tenants, and
     * OpenstackEndpoints for a user.
     *
     * @param baseUser
     * @return
     */
    ServiceCatalogInfo getServiceCatalogInfo(BaseUser baseUser);

    /**
     * Similar to {@link #getServiceCatalogInfo(BaseUser)}, except the RCN Role logic is applied to the user's roles.
     * When applied all non-RCN global roles are granted on all tenants within the user's domain and RCN roles are applied
     * across domains within the RCN.
     *
     * @param baseUser
     * @return
     */
    ServiceCatalogInfo getServiceCatalogInfoApplyRcnRoles(BaseUser baseUser);

    @Deprecated
    UserScopeAccess addScopeAccess(User user, String clientId, List<String> authenticatedBy);

    boolean isScopeAccessExpired(ScopeAccess scopeAccess);

    String getClientIdForParent(ScopeAccess scopeAccess);

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

    AuthResponseTuple createScopeAccessForUserAuthenticationResult(UserAuthenticationResult userAuthenticationResult);

    /**
     * Generate a UUID token string
     * @return
     */
    String generateToken();
}