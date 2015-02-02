package com.rackspace.idm.domain.security;

import com.rackspace.idm.domain.entity.BaseUser;

public interface TokenFormatSelector {

    /**
     * Returns the format of the token that will be generated for the user on authentication
     *
     * @param user
     * @return
     */
    TokenFormat formatForNewToken(BaseUser user);

    /**
     * Returns the format of the supplied token
     */
    TokenFormat formatForExistingToken(String tokenString);

}
