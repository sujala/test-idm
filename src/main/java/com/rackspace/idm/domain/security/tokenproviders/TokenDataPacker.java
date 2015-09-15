package com.rackspace.idm.domain.security.tokenproviders;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;

public interface TokenDataPacker {
    public static final String ERROR_CODE_PACKING_EXCEPTION = "MPP-0001";
    public static final String ERROR_CODE_PACKING_INVALID_SCOPE_EXCEPTION = "MPP-0002";
    public static final String ERROR_CODE_PACKING_INVALID_AUTHENTICATEDBY_EXCEPTION = "MPP-0003";

    public static final String ERROR_CODE_UNPACK_INVALID_DATAPACKING_SCHEME = "MPU-0003";
    public static final String ERROR_CODE_UNPACK_INVALID_DATAPACKING_VERSION = "MPU-0004";
    public static final String ERROR_CODE_UNPACK_INVALID_DATA_CONTENTS = "MPU-0005";
    public static final String ERROR_CODE_UNPACK_INVALID_SCOPE_DATA_CONTENTS = "MPU-0006";
    public static final String ERROR_CODE_UNPACK_INVALID_AUTHBY_DATA_CONTENTS = "MPU-0007";
    public static final String ERROR_CODE_UNPACK_INVALID_IMPERSONATED_DATA_CONTENTS = "MPU-0008";

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
