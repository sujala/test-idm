package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.unboundid.ldap.sdk.DN;

/**
 * Identifies the rolespace for a delegation agreement
 */
public interface DelegationPrincipal {

    /**
     * Retrieves the unique identifier of the principal
     *
     * @return
     */
    String getId();

    /**
     * Identifies the type of principal
     * @return
     */
    PrincipalType getPrincipalType();

    DN getDn();

    String getDomainId();
}
