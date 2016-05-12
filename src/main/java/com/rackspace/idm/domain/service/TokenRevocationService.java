package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionRequest;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionResponse;
import com.rackspace.idm.domain.entity.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface TokenRevocationService {
    /**
     * Static constant defining NULL tokens that can be passed to the {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} and
     * {@link #revokeTokensForEndUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_NULL_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.NULL));

    /**
     * Static constant defining ALL tokens (except "impersonation" user tokens) that can be passed to the {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} and
     * {@link #revokeTokensForEndUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_ALL_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.ALL));

    /**
     * Static constant defining APIKEY tokens that can be passed to the {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} and
     * {@link #revokeTokensForEndUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_API_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.APIKEY));

    /**
     * Static constant defining PASSWORD tokens that can be passed to the {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} and
     * {@link #revokeTokensForEndUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_PASSWORD_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.PASSWORD));
    /**
     * Static constant defining FEDERATION tokens that can be passed to the {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} and
     * {@link #revokeTokensForEndUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_FEDERATION_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.FEDERATION));

    /**
     * Static constant defining IMPERSONATION tokens that can be passed to the {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} and
     * {@link #revokeTokensForEndUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_IMPERSONATION_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.IMPERSONATION));

    /**
     * Static constant defining MFA tokens that can be passed to the {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} and
     * {@link #revokeTokensForEndUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_MFA_TOKENS = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.PASSWORD_PASSCODE, AuthenticatedByMethodGroup.PASSWORD_OTPPASSCODE));
    /**
     * Static constant defining tokens groups that should be revoked when MFA is enabled for a user. This can be passed to the {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} and
     * {@link #revokeTokensForEndUser(String, java.util.List)} methods
     */
    public static final List<AuthenticatedByMethodGroup> AUTH_BY_LIST_REVOKE_ON_MFA_ENABLE = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodGroup.PASSWORD, AuthenticatedByMethodGroup.EMAIL));

    /**
     * Revoke the specified token if it is valid (not expired) and has not been revoked previously.
     * <p>
     *     If a valid user token is revoked, an atom hopper feed event must be sent to announce the token revocation
     * </p>
     *
     * @param tokenString
     * @throws java.lang.UnsupportedOperationException if the service does not support this particular type of token
     */
    void revokeToken(String tokenString);

    /**
     * Revoke the specified token if it is valid (not expired) and has not been revoked previously.
     * <p>
     *     If a valid user token is revoked, an atom hopper feed event must be sent to announce the token revocation
     * </p>
     * @param token
     * @throws java.lang.UnsupportedOperationException if the service does not support this particular type of token
     */
    void revokeToken(Token token);

    /**
     * Revoke the specified token if it is valid (not expired) and has not been revoked previously.
     * <p>
     *     If a valid user token is revoked, an atom hopper feed event must be sent to announce the token revocation
     * </p>
     * <p>
     * The inclusion of the user argument is unnecessary (and potentially inconsistent with the scope access). This method
     * is provided SOLELY for efficiency reasons so the underlying implementation does not have to retrieve the user from
     * the backend in order to send the cloud feed event. This leaks implementation, but given performance needs and the
     * desire to not do a ton of refactoring at once, this method was added. If the calling method does NOT already have
     * the user object loaded, it is highly recommended to call the {@link #revokeToken(com.rackspace.idm.domain.entity.Token)}
     * instead.
     *
     * </p>
     * @param user
     * @param token
     * @throws java.lang.UnsupportedOperationException if the service does not support this particular type of token
     */
    void revokeToken(BaseUser user, Token token);

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
    void revokeTokensForEndUser(String userId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups);

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
    void revokeTokensForEndUser(EndUser user, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups);

    /**
     * Revoke all tokens for the specified EndUser (Provisioned or Federated). This is a convenience method for passing
     * {@link #AUTH_BY_LIST_ALL_TOKENS} to {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} or
     * {@link #revokeTokensForEndUser(String, java.util.List)}
     * <p>
     *     An atom hopper feed event must be sent, as appropriate, to represent the tokens being revoked.
     * </p>
     *
     * @param userId
     */
    void revokeAllTokensForEndUser(String userId);

    /**
     * Revoke all tokens for the specified EndUser (Provisioned or Federated). This is a convenience method for passing
     * {@link #AUTH_BY_LIST_ALL_TOKENS} to {@link #revokeTokensForEndUser(com.rackspace.idm.domain.entity.EndUser, java.util.List)} or
     * {@link #revokeTokensForEndUser(String, java.util.List)}
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
     * @throws java.lang.UnsupportedOperationException if the service does not support this particular type of token
     * @return
     */
    boolean isTokenRevoked(String token);

    /**
     * Whether the specified token has been revoked.
     *
     * @param token
     * @throws java.lang.IllegalArgumentException If supplied token is null
     * @throws java.lang.UnsupportedOperationException if the service does not support this particular type of token
     *
     * @return
     */
    boolean isTokenRevoked(Token token);

    /**
     * Whether the service supports revoking the specified token.
     *
     * @param sa
     * @return
     */
    boolean supportsRevokingFor(Token sa);

    /**
     * Purge up to the specified number of obsolete TRRs. Delay the specified amount of ms between delete requests.
     *
     * @return
     */
    TokenRevocationRecordDeletionResponse purgeObsoleteTokenRevocationRecords(int limit, int delay);
}
