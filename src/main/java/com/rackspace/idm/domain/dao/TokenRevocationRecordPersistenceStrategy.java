package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup;
import com.rackspace.idm.domain.entity.Token;
import com.rackspace.idm.domain.entity.TokenRevocationRecord;

import java.util.List;

/**
 * Interface for backend persistence mechanisms for token revocation records (TRR). Must NOT be tied to any specific mechanism (e.g LDAP),
 * which is why this interface does NOT extend "GenericDao" like so many other "DAOs" do.
 */
public interface TokenRevocationRecordPersistenceStrategy<T extends TokenRevocationRecord> {
    /**
     * Add a token TRR to revoke the specified token string. Returns a TokenRevocationRecord representing the request
     * that contains an ID that can subsequently be used to retrieve the request at a later date.
     *
     * @param tokenStr
     * @return
     */
    T addTokenTrrRecord(String tokenStr);

    /**
     * Add a user TRR to revoke all previously issued tokens to the specified user matching one of the authentication
     * method groups supplied.
     *
     *
     * @param targetUserId
     * @param authenticatedBy
     * @return
     */
    T addUserTrrRecord(String targetUserId, List<AuthenticatedByMethodGroup> authenticatedBy);

    /**
     * Retrieve the specified record, or null, if not found.
     *
     * @param id
     * @return
     */
    T getTokenRevocationRecord(String id);

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
     * @param token
     * @return
     */
    Iterable<T> getActiveTokenRevocationRecordsMatchingToken(Token token);

    /**
     * Whether a TRR exists such that it would be returned by the {@link #getActiveTokenRevocationRecordsMatchingToken(Token)}
     *
     * @param token
     * @return
     */
    boolean doesActiveTokenRevocationRecordExistMatchingToken(Token token);

    /**
     * Find up to the specified number of obsolete TRRs
     *
     * @param max
     * @return
     */
    List<T> findObsoleteTrrs(int max);

    /**
     * Delete the specific TRR
     *
     * @param record
     */
    void deleteTokenRevocationRecord(T record);

}