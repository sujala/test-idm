package com.rackspace.idm.domain.service;

import org.joda.time.DateTime;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.AuthCredentials;
import com.rackspace.idm.domain.entity.OAuthGrantType;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.exception.NotAuthenticatedException;

/**
 * User: john.eo
 * Date: 1/10/11
 * Time: 8:24 AM
 */
public interface OAuthService {
    ScopeAccessObject getAccessTokenByAuthHeader(String authHeader);

    ScopeAccessObject getTokens(OAuthGrantType grantType, AuthCredentials trParam, DateTime currentTime) throws
            NotAuthenticatedException;

//    /**
//     * Only other IDM instances are authorized to call this method.
//     * 
//     * @param tokenStringRequestingDelete
//     * @param tokenToDelete
//     * @throws NotAuthorizedException
//     */
//    void revokeTokensLocally(String tokenStringRequestingDelete, String tokenToDelete) throws NotAuthorizedException;
//    
//    /**
//     * Can be invoked by either IDM or any client.
//     * 
//     * @param tokenStringRequestingDelete
//     * @param tokenToDelete
//     * @throws NotAuthorizedException
//     */
//    void revokeTokensGlobally(String tokenStringRequestingDelete, String tokenToDelete) throws NotAuthorizedException;
//
//    /**
//     * Only other IDM instances are authorized to call this method.
//     * 
//     * @param authTokenString
//     * @param ownerId
//     */
//    void revokeTokensLocallyForOwner(String idmAuthTokenStr, String ownerId);
//    
//    /**
//     * To be ONLY used internally within the SAME IDM instance.
//     * 
//     * @param ownerId
//     */
//    void revokeTokensGloballyForOwner(String ownerId);
//    
//    /**
//     * Only other IDM instance are allowed to call this method.
//     * 
//     * @param authTokenString
//     * @param customerId
//     */
//    void revokeTokensLocallyForCustomer(String idmAuthTokenStr, String customerId);
//    
//    /**
//     * To be ONLY used internally within the SAME IDM instance.
//     * 
//     * @param customerId
//     */
//    void revokeTokensGloballyForCustomer(String customerId);

    /**
     * Convert string to OAuth Grant Type.
     *
     * @param grantTypeString
     */
    OAuthGrantType getGrantType(String grantTypeString);
    
//    void revokeTokensLocallyForOwnerOrCustomer(String idmAuthTokenStr, TokenDeleteByType queryType, String ownerId);

    /**
     * Validate a grant type
     *
     * @param trParam
     * @param grantType
     */
    ApiError validateGrantType(AuthCredentials trParam, OAuthGrantType grantType);
}
