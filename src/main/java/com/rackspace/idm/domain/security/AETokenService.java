package com.rackspace.idm.domain.security;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;

public interface AETokenService {
    /**
     * Take in a token and generate a web safe representation of it. The user/token must not be null and the user related information in the token (userRsId, username) must match that
     * of the provided user or an error will be thrown. The token is expected to have an expiration date.
     *
     * @param token
     * @return
     * @throws IllegalArgumentException if provided arguments don't meet requirements
     * @throws MarshallTokenException if an error is encountered generating the token
     */
    String marshallTokenForUser(BaseUser user, ScopeAccess token);

    /**
     * Take in a web safe representation of a token generated by {@link #marshallTokenForUser(com.rackspace.idm.domain.entity.BaseUser, com.rackspace.idm.domain.entity.ScopeAccess)}
     * and populate an associated ScopeAccess object
     *
     * @param webSafeToken
     * @return
     * @throws IllegalArgumentException if provided arguments don't meet requirements
     * @throws UnmarshallTokenException if an error is encountered unmarshalling the token
     */
    ScopeAccess unmarshallToken(String webSafeToken);

    /**
     * Take in a web safe representation of a token generated by {@link #marshallTokenForUser(com.rackspace.idm.domain.entity.BaseUser,
     * com.rackspace.idm.domain.entity.ScopeAccess)}
     * and populate an associated ScopeAccess object. Then validate the resultant token to determine whether it's been revoked.
     * Returns null if the token can be unmarshalled, but the token is revoked.
     *
     * @param webSafeToken
     * @return
     * @throws IllegalArgumentException if provided arguments don't meet requirements
     * @throws UnmarshallTokenException if an error is encountered unmarshalling the token
     */
    ScopeAccess unmarshallTokenAndCheckRevoked(String webSafeToken);

    /**
     * Take in a web safe representation of a token generated by {@link #marshallTokenForUser(com.rackspace.idm.domain.entity.BaseUser,
     * com.rackspace.idm.domain.entity.ScopeAccess)}
     * and populate an associated ScopeAccess object. Then validate the resultant token to determine whether it's expired
     * or revoked. Returns null if the token can't be unmarshalled or can be unmarshalled but the token is expired or revoked.
     *
     * @param webSafeToken
     * @return
     * @throws IllegalArgumentException if provided arguments don't meet requirements
     * @throws UnmarshallTokenException if an error is encountered unmarshalling the token
     */
    ScopeAccess unmarshallTokenAndValidate(String webSafeToken);

    /**
     * Verify marshall/unmarshall support for a set of object and scopeAccess
     *
     * @param object
     * @param scopeAccess
     * @return
     */
    boolean supportsCreatingTokenFor(UniqueId object, ScopeAccess scopeAccess);
}
