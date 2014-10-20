package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface TokenRevocationService {
    /**
     * Static constant defining NULL tokens that can be passed to the {@link #revokeTokensForBaseUser(com.rackspace.idm.domain.entity.BaseUser, java.util.List)} and
     * {@link #revokeTokensForBaseUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_NULL_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.NULL));

    /**
     * Static constant defining ALL tokens that can be passed to the {@link #revokeTokensForBaseUser(com.rackspace.idm.domain.entity.BaseUser, java.util.List)} and
     * {@link #revokeTokensForBaseUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_ALL_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.ALL));

    /**
     * Static constant defining APIKEY tokens that can be passed to the {@link #revokeTokensForBaseUser(com.rackspace.idm.domain.entity.BaseUser, java.util.List)} and
     * {@link #revokeTokensForBaseUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_API_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.APIKEY));

    /**
     * Static constant defining PASSWORD tokens that can be passed to the {@link #revokeTokensForBaseUser(com.rackspace.idm.domain.entity.BaseUser, java.util.List)} and
     * {@link #revokeTokensForBaseUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_PASSWORD_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.PASSWORD));
    /**
     * Static constant defining FEDERATION tokens that can be passed to the {@link #revokeTokensForBaseUser(com.rackspace.idm.domain.entity.BaseUser, java.util.List)} and
     * {@link #revokeTokensForBaseUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_FEDERATION_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.FEDERATION));

    /**
     * Static constant defining IMPERSONATION tokens that can be passed to the {@link #revokeTokensForBaseUser(com.rackspace.idm.domain.entity.BaseUser, java.util.List)} and
     * {@link #revokeTokensForBaseUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_IMPERSONATION_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.IMPERSONATION));

    /**
     * Static constant defining MFA tokens that can be passed to the {@link #revokeTokensForBaseUser(com.rackspace.idm.domain.entity.BaseUser, java.util.List)} and
     * {@link #revokeTokensForBaseUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_MFA_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.PASSWORD_PASSCODE));

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
     *     The provided authenticatedBy argument should be interpreted as following. Every AuthenticatedByMethodGroup in the list defines a
     *     set of (unordered) authentication mechanisms. A token whose authenticatedBy items exactly matches one of the groups in list, must be
     *     revoked. For example, if the authenticatedByList contains a group (PASSWORD,PASSCODE), a token whose authenticatedBy
     *     list is one of (PASSWORD,PASSCODE) or (PASSCODE,PASSWORD) would be revoked, but a token with just (PASSWORD)
     *     would not be. There are also a couple special groups - the null group which only matches tokens that have no auth by values, and
     *     the 'all' group which matches all tokens (regardless of the auth by value).
     * </p>
     * <p>
     *     An atom hopper feed event must be sent, as appropriate, to represent the tokens being revoked.
     * </p>
     *
     * @param userId
     * @param authenticatedByMethodGroups
     */
    void revokeTokensForBaseUser(String userId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups);

    /**
     * Revoke all tokens for the specified EndUser (Provisioned or Federated), which used the specified authentication methods.
     *
     * <p>
     *     The provided authenticatedBy argument should be interpreted as following. Every AuthenticatedByMethodGroup in the list defines a
     *     set of (unordered) authentication mechanisms. A token whose authenticatedBy items exactly matches one of the groups in list, must be
     *     revoked. For example, if the authenticatedByList contains a group (PASSWORD,PASSCODE), a token whose authenticatedBy
     *     list is one of (PASSWORD,PASSCODE) or (PASSCODE,PASSWORD) would be revoked, but a token with just (PASSWORD)
     *     would not be. There are also a couple special groups - the null group which only matches tokens that have no auth by values, and
     *     the 'all' group which matches all tokens (regardless of the auth by value).
     * </p>
     * <p>
     *     An atom hopper feed event must be sent, as appropriate, to represent the tokens being revoked.
     * </p>
     *
     * @param user
     * @param authenticatedByMethodGroups
     */
    void revokeTokensForBaseUser(BaseUser user, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups);

    /**
     * Revoke all tokens for the specified EndUser (Provisioned or Federated). This is a convenience method for passing
     * {@link #AUTH_BY_LIST_ALL_TOKENS} to {@link #revokeTokensForBaseUser(com.rackspace.idm.domain.entity.BaseUser, java.util.List)} or
     * {@link #revokeTokensForBaseUser(String, java.util.List)}
     * <p>
     *     An atom hopper feed event must be sent, as appropriate, to represent the tokens being revoked.
     * </p>
     *
     * @param userId
     */
    void revokeAllTokensForBaseUser(String userId);

    /**
     * Revoke all tokens for the specified EndUser (Provisioned or Federated). This is a convenience method for passing
     * {@link #AUTH_BY_LIST_ALL_TOKENS} to {@link #revokeTokensForBaseUser(com.rackspace.idm.domain.entity.BaseUser, java.util.List)} or
     * {@link #revokeTokensForBaseUser(String, java.util.List)}
     * <p>
     *     An atom hopper feed event must be sent, as appropriate, to represent the tokens being revoked.
     * </p>
     * @param user
     */
    void revokeAllTokensForBaseUser(BaseUser user);

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
