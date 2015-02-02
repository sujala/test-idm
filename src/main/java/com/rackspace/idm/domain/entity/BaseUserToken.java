package com.rackspace.idm.domain.entity;

/**
 * A token issued to a base user (racker, federated user, provisioned user)
 */
public interface BaseUserToken extends Token {

    /**
     * Retrieve the userId of the user to whom the token was issued.
     * @return
     */
    String getIssuedToUserId();

}
