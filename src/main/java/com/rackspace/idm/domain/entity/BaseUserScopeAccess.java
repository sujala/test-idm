package com.rackspace.idm.domain.entity;

/**
 * delineates a token issued to a base user (racker, federated user, provisioned user)
 */
public interface BaseUserScopeAccess {

    /**
     * Retrieve the userId of the user to whom the token was issued.
     * @return
     */
    String getIssuedToUserId();
}
