package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TokenRevocationRecord;

import java.util.List;
import java.util.Set;

/**
 * Interface for backend persistence mechanisms for token revocation records (TRR). Must NOT be tied to any specific mechanism (e.g LDAP),
 * which is why this interface does NOT extend "GenericDao" like os many other "DAOs" do.
 */
public interface TokenRevocationRecordPersistenceStrategy {
    /**
     * Add a token TRR
     *
     * @param tokenStr
     * @return
     */
    TokenRevocationRecord addTokenTrrRecord(String tokenStr);

    /**
     * Add a user TRR
     *
     * @param targetUserId
     * @param authenticatedBy
     * @return
     */
    TokenRevocationRecord addUserTrrRecord(String targetUserId, List<Set<String>> authenticatedBy);

    /**
     * Retrieve the specified record, or null, if not found.
     *
     * @param id
     * @return
     */
    TokenRevocationRecord getTokenRevocationRecord(String id);

    /**
     * Get all the TRRs for which:
     *
     * <p>
     *     <ol>
     *         <li>The TRR is for the provided token AND the token creation date is before the TRR's targetCreatedBefore</li>
     *         <li>The TRR is for the provider user AND the token creation date is before the TRR's targetCreatedBefore</li>
     *     </ol>
     * </p>
     *
     * <p>
     *     The userId is explicitly specified to allow the underlying implementations to not have to worry about determining whether a token is
     *     a racker token, impersonation token, regular user token, etc in order to determine the user. This could change if ScopeAccess is
     *     modified to provide a consistent 'userId' that can be retrieved off the token.
     * </p>
     * @param userId
     * @param token
     * @return
     */
    Iterable<? extends TokenRevocationRecord> getActiveTokenRevocationRecordsMatchingToken(String userId, ScopeAccess token);

    /**
     * Whether a TRR exists such that it would be returned by the {@link #getActiveTokenRevocationRecordsMatchingToken(String, ScopeAccess)}
     *
     * @param userId
     * @param token
     * @return
     */
    boolean doesActiveTokenRevocationRecordExistMatchingToken(String userId, ScopeAccess token);
}