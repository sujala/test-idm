package com.rackspace.idm.domain.entity;

import java.util.Date;
import java.util.List;

public interface Token {

    /**
     * The list of authentication mechanisms that were provided to prove the identity of the caller
     * @return
     */
    List<String> getAuthenticatedBy();

    /**
     * The date this token was created
     * @return
     */
    Date getCreateTimestamp();

    /**
     * The websafe token string returned to users to represent this token
     */
    String getAccessTokenString();

    /**
     * Return a masked version of the access token string suitable for logging
     *
     * @return
     */
    String getMaskedAccessTokenString();

    /**
     * When the token should be considered expired
     *
     * @return
     */
    Date getAccessTokenExp();

    /**
     * Helper method to determine whether access token is expired given current date/time
     *
     * @return
     */
    boolean isAccessTokenExpired();

    /**
     * Retrieves the scope of the token
     *
     * @return
     */
    String getScope();
}
