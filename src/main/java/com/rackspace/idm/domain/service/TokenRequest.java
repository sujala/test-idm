package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;

import java.time.Instant;

/**
 * Transfer object that provides information necessary to generate a new AE token.
 */
public interface TokenRequest<T extends BaseUser, S extends ScopeAccess> {
    /**
     * The user to whom the token will be issued.
     *
     * @return
     */
    T getIssuedToUser();

    /**
     * The requested expiration date of the token
     *
     * @return
     */
    Instant getExpirationDate();

    /**
     * The authentication methods the user used to verify their identity
     *
     * @return
     */
    AuthenticatedByMethodGroup getAuthenticatedByMethodGroup();

    /**
     * The domain into which the user is requesting authentication
     *
     * @return
     */
    String getAuthenticationDomainId();

    /**
     * This is a legacy holdover from OAuth based initial architecture. It's, for all current scenarios, the standard
     * Identity "client". However, the clientId can change per environment.
     *
     * @return
     */
    String getClientId();

    /**
     * This adapts the token request to a scopeaccess object, which is the legacy representation of a token. Legacy code
     * used a scope access both as a "request" to generate a new token, and a representation of an already generated
     * token.
     *
     * @return
     */
    S generateShellScopeAccessForRequest();
}
