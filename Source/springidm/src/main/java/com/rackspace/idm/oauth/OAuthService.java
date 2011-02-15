package com.rackspace.idm.oauth;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import org.joda.time.DateTime;

/**
 * User: john.eo
 * Date: 1/10/11
 * Time: 8:24 AM
 */
public interface OAuthService {
    AccessToken getAccessTokenByAuthHeader(String authHeader);

    AuthData getTokens(OAuthGrantType grantType, AuthCredentials trParam, DateTime currentTime) throws
            NotAuthenticatedException;

    /**
     * Only other IDM instances are authorized to call this method.
     * 
     * @param tokenStringRequestingDelete
     * @param tokenToDelete
     * @throws NotAuthorizedException
     */
    void revokeTokensLocally(String tokenStringRequestingDelete, String tokenToDelete) throws NotAuthorizedException;
    
    /**
     * Can be invoked by either IDM or any client.
     * 
     * @param tokenStringRequestingDelete
     * @param tokenToDelete
     * @throws NotAuthorizedException
     */
    void revokeTokensGlobally(String tokenStringRequestingDelete, String tokenToDelete) throws NotAuthorizedException;

    /**
     * Only other IDM instances are authorized to call this method.
     * 
     * @param authTokenString
     * @param ownerId
     */
    void revokeTokensLocallyForOwner(String idmAuthTokenStr, String ownerId);
    
    /**
     * To be ONLY used internally within the SAME IDM instance.
     * 
     * @param ownerId
     */
    void revokeTokensGloballyForOwner(String ownerId);
    
    /**
     * Only other IDM instance are allowed to call this method.
     * 
     * @param authTokenString
     * @param customerId
     */
    void revokeTokensLocallyForCustomer(String idmAuthTokenStr, String customerId);
    
    /**
     * To be ONLY used internally within the SAME IDM instance.
     * 
     * @param customerId
     */
    void revokeTokensGloballyForCustomer(String customerId);
}
