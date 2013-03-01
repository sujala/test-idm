package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.ScopeAccess;
import org.apache.commons.configuration.Configuration;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public interface TokenService {
    ScopeAccess getAccessTokenByAuthHeader(String authHeader);

    boolean doesTokenHaveAccessToApplication(String token, String applicationId);
    
    boolean doesTokenHaveApplicationRole(String token, String applicationId, String roleId);
    
    void revokeAccessToken(String tokenStringRequestingDelete, String tokenToDelete);
    
    void revokeAllTokensForClient(String clientId);
    
    void revokeAllTokensForCustomer(String customerId) throws IOException, JAXBException;

    void revokeAllTokensForUser(String username) throws IOException, JAXBException;

    void setAuthorizationService(AuthorizationService authorizationService);

    void setConfig(Configuration appConfig);

    void setScopeAccessService(ScopeAccessService scopeAccessService);

    void setTenantService(TenantService tenantService);

    void setUserService(UserService userService);

    void setClientService(ApplicationService applicationService);
}
