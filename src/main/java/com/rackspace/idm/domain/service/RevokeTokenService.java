package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;

import java.util.List;
import java.util.Set;

public interface RevokeTokenService {
    /**
     * Revoke the specified token if it is valid (not expired).
     * <p>
     *     If a valid user token is revoked, an atom hopper feed event must be sent to announce the token revocation
     * </p>
     *
     * @param tokenString
     */
    void revokeToken(String tokenString);

    /**
     * Revoke the specified token if it is valid (not expired).
     * <p>
     *     If a valid user token is revoked, an atom hopper feed event must be sent to announce the token revocation
     * </p>
     * @param tokenString
     */
    void revokeToken(ScopeAccess tokenString);

    /**
     * Revoke the specified token if it is valid (not expired).
     * <p>
     *     If a valid user token is revoked, an atom hopper feed event must be sent to announce the token revocation
     * </p>
     * @param user
     * @param scopeAccess
     */
    void revokeToken(BaseUser user, ScopeAccess scopeAccess);

    /**
     * Revoke all tokens for the specified EndUser (Provisioned or Federated), which used the specified authentication methods.
     *
     * <p>
     *     The provided authenticatedByList should be interpreted as following. Every item in the list is a set of
     *     unordered authentication mechanisms. A token whose authenticatedBy items exactly matches one of the sets in list, must be
     *     revoked. For example, if the authenticatedByList contains an item (PASSWORD,PASSCODE), a token whose authenticatedBy
     *     list is one of (PASSWORD,PASSCODE) or (PASSCODE,PASSWORD) would be revoked, but a token with just (PASSWORD)
     *     would not be. An empty set can be included to revoke tokens that do not contain any authenticatedBy options.
     * </p>
     * <p>
     *     An atom hopper feed event must be sent, as appropriate, to represent the tokens being revoked.
     * </p>
     *
     * @param userId
     * @param authenticatedByList
     */
    void revokeTokensForEndUser(String userId, List<Set<String>> authenticatedByList);

    /**
     * Revoke all tokens for the specified EndUser (Provisioned or Federated), which used the specified authentication methods.
     *
     * <p>
     *     The provided authenticatedByList should be interpreted as following. Every item in the list is a set of
     *     unordered authentication mechanisms. A token whose authenticatedBy items exactly matches one of the sets in list, must be
     *     revoked. For example, if the authenticatedByList contains an item (PASSWORD,PASSCODE), a token whose authenticatedBy
     *     list is one of (PASSWORD,PASSCODE) or (PASSCODE,PASSWORD) would be revoked, but a token with just (PASSWORD)
     *     would not be.  An empty set can be included to revoke tokens that do not contain any authenticatedBy options.
     * </p>
     * <p>
     *     An atom hopper feed event must be sent, as appropriate, to represent the tokens being revoked.
     * </p>
     *
     * @param user
     * @param authenticatedByList
     */
    void revokeTokensForEndUser(EndUser user, List<Set<String>> authenticatedByList);

    /**
     * Revoke all tokens for the specified EndUser (Provisioned or Federated),
     * <p>
     *     An atom hopper feed event must be sent, as appropriate, to represent the tokens being revoked.
     * </p>
     *
     * @param userId
     */
    void revokeAllTokensForEndUser(String userId);

    /**
     * Revoke all tokens for the specified EndUser (Provisioned or Federated),
     * <p>
     *     An atom hopper feed event must be sent, as appropriate, to represent the tokens being revoked.
     * </p>
     * @param user
     */
    void revokeAllTokensForEndUser(EndUser user);

    /**
     * Whether the specified token has been revoked.
     *
     * @param token
     * @return
     */
    boolean isTokenRevoked(String token);

    /**
     * Whether the specified token has been revoked.
     *
     * @param token
     * @return
     */
    boolean isTokenRevoked(ScopeAccess token);
}
