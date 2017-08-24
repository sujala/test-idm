package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.LdapTokenRevocationRecord;
import com.rackspace.idm.domain.entity.Token;
import com.rackspace.idm.domain.entity.TokenRevocationRecord;

import java.util.List;

/**
 * Defines additional services that must be supported for revoking AE Tokens
 */
public interface AETokenRevocationService extends TokenRevocationService {
    /**
     * Return all Token Revocation Records that would cause the specified token to be revoked
     * @param token
     * @return
     */
    List<LdapTokenRevocationRecord> findTokenRevocationRecordsMatchingToken(Token token);

}
