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

    /**
     * The domain against which the user was authenticated against, and for which this token is valid.
     *
     * @return
     */
    String getAuthenticationDomainId();

    /**
     * Whether or not the token is a delegation token. A delegation token must have a delegationId associated
     * with it ({@link #getDelegationAgreementId()}
     *
     * @return
     */
    boolean isDelegationToken();

    /**
     * The delegation agreement under which this token was issued. Returns null if the token was not issued under an
     * agreement.
     *
     * @return
     */
    String getDelegationAgreementId();
}
