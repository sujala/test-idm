package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.ScopeAccess;

public interface TokenService {
    ScopeAccess getAccessTokenByAuthHeader(String authHeader);

//    OAuthGrantType getGrantType(String grantTypeString);

//    ScopeAccess getTokens(OAuthGrantType grantType, Credentials trParam, DateTime currentTime) throws
//            NotAuthenticatedException;
    
    boolean doesTokenHaveAccessToApplication(String token, String applicationId);
    
    boolean doesTokenHaveAplicationRole(String token, String applicationId, String roleId);
    
    void revokeAccessToken(String tokenStringRequestingDelete, String tokenToDelete);
    
    void revokeAllTokensForClient(String clientId);
    
    void revokeAllTokensForCustomer(String customerId);

    void revokeAllTokensForUser(String username);

//    void validateGrantType(Credentials trParam, OAuthGrantType grantType);
}
