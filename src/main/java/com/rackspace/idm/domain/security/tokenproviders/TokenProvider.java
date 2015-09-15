package com.rackspace.idm.domain.security.tokenproviders;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;

/**
 *
 */
public interface TokenProvider {

    /**
     * The supported use cases for the token provider.
     *
     * @param object
     * @param scopeAccess
     * @return
     */
    boolean supportsCreatingTokenFor(UniqueId object, ScopeAccess scopeAccess);

    /**
     * The supported use cases for the token provider.
     *
     * @return
     */
    boolean canDecryptScheme(byte scheme);

    /**
     * Generate a new token for the user
     *
     * @param user
     * @param token
     * @return
     */
    String marshallTokenForUser(BaseUser user, ScopeAccess token);

    /**
     * Unpack the given token
     *
     * @param userProvidedToken
     * @return
     */
    ScopeAccess unmarshallToken(String userProvidedToken);

}
