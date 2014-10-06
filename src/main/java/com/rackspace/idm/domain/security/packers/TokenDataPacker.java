package com.rackspace.idm.domain.security.packers;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;

public interface TokenDataPacker {
    /**
     * Pack/Compress the the provided scope access into a minimal representation for subsequent encryption. Reverse this
     * process via {@link #unpackTokenData(String, byte[])}
     *
     * @param user
     * @param token
     * @return
     */
    byte[] packTokenDataForUser(BaseUser user, ScopeAccess token);

    /**
     * Reverse the process of {@link #packTokenDataForUser(com.rackspace.idm.domain.entity.BaseUser, com.rackspace.idm.domain.entity.ScopeAccess)}
     *
     * @param webSafeToken
     * @param packedData
     * @return
     */
    ScopeAccess unpackTokenData(String webSafeToken, byte[] packedData);
}
