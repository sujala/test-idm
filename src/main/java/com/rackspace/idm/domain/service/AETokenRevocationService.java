package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.ScopeAccess;

/**
 * Defines additional services that must be supported for revoking AE Tokens
 */
public interface AETokenRevocationService extends TokenRevocationService {

    /**
     * Whether the service supports revoking the specified token type issued against the specified object.
     *
     * @param obj
     * @param sa
     * @return
     */
    boolean supportsRevokingFor(UniqueId obj, ScopeAccess sa);
}
