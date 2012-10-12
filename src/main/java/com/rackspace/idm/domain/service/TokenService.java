package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.ScopeAccess;
import org.apache.commons.configuration.Configuration;

public interface TokenService {
    ScopeAccess getAccessTokenByAuthHeader(String authHeader);

    boolean doesTokenHaveAccessToApplication(String token, String applicationId);
    
    boolean doesTokenHaveApplicationRole(String token, String applicationId, String roleId);
    
    void revokeAccessToken(String tokenStringRequestingDelete, String tokenToDelete);
    
    void revokeAllTokensForClient(String clientId);
    
    void revokeAllTokensForCustomer(String customerId);

    void revokeAllTokensForUser(String username);

    void setClientService(ApplicationService clientService);

    void setAuthorizationService(AuthorizationService authorizationService);

    void setConfig(Configuration appConfig);

    void setScopeAccessService(ScopeAccessService scopeAccessService);

    void setUserDao(UserDao userDao);

    void setTenantService(TenantService tenantService);
}
