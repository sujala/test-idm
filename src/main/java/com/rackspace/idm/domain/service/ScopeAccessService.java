package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.util.AuthHeaderHelper;
import org.apache.commons.configuration.Configuration;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public interface ScopeAccessService {

    void addUserScopeAccess(User user, ScopeAccess scopeAccess);

    void addApplicationScopeAccess(Application application, ScopeAccess scopeAccess);

    ScopeAccess addImpersonatedScopeAccess(User user, String clientId, String impersonatingToken, ImpersonationRequest impersonationRequest);

    boolean authenticateAccessToken(String accessTokenStr);

    void deleteScopeAccess(ScopeAccess scopeAccess);

    void deleteScopeAccessesForApplication(Application application, String clientId);

    void deleteScopeAccessesForUser(User user, String clientId);

    void expireAccessToken(String tokenString) throws IOException, JAXBException;

    void expireAllTokensForClient(String clientId);

    void expireAllTokensForUser(String username) throws IOException, JAXBException;

    void expireAllTokensForUserById(String userId) throws IOException, JAXBException;

    ScopeAccess getAccessTokenByAuthHeader(String authHeader);

    ClientScopeAccess getApplicationScopeAccess(Application application);

    RackerScopeAccess getRackerScopeAccessByClientId(User user, String clientId);

    ScopeAccess getScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessForUser(User user);

    List<ScopeAccess> getScopeAccessListByUserId(String userId);

    ScopeAccess loadScopeAccessByAccessToken(String accessToken);

    ScopeAccess getScopeAccessByRefreshToken(String refreshToken);

    ScopeAccess getMostRecentDirectScopeAccessForUserByClientId(User user, String clientId);

    UserScopeAccess getUserScopeAccessByClientId(User user, String clientId);

    UserScopeAccess getValidUserScopeAccessForClientId(User user, String clientId, List<String> authenticateBy);

    RackerScopeAccess getValidRackerScopeAccessForClientId(User user, String clientId, List<String> authenticatedBy);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndApiCredentials(String username, String apiKey, String clientId);

    UserScopeAccess getUserScopeAccessForClientIdByUsernameAndPassword(String username, String password, String clientId);

    void updateScopeAccess(ScopeAccess scopeAccess);

    List<ScopeAccess> getScopeAccessesForUserByClientId(User user, String clientId);

    List<ScopeAccess> getScopeAccessesForApplicationByClientId(Application application, String clientId);

    List<ScopeAccess> getScopeAccessesForUser(User user);

    List<ScopeAccess> getScopeAccessesForApplication(Application application);

    List<OpenstackEndpoint> getOpenstackEndpointsForUser(User user);

    List<OpenstackEndpoint> getOpenstackEndpointsForScopeAccess(ScopeAccess scopeAccess);

    UserScopeAccess updateExpiredUserScopeAccess(User user, String clientId, List<String> authenticatedBy);

    UserScopeAccess updateExpiredUserScopeAccess(UserScopeAccess scopeAccess, boolean impersonated);

    boolean isScopeAccessExpired(ScopeAccess scopeAccess);

    void setApplicationService(ApplicationService applicationService);

    void setTenantService(TenantService tenantService);

    void setUserService(UserService userService);

    int getTokenExpirationSeconds(int value);

    String getClientIdForParent(ScopeAccess scopeAccess);

    String getUserIdForParent(ScopeAccess scopeAccessByAccessToken);
}
