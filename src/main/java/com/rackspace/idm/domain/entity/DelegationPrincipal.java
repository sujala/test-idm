package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.unboundid.ldap.sdk.DN;

/**
 * Identifies the rolespace for a delegation agreement
 */
public interface DelegationPrincipal extends DelegationConsumer {

    /**
     * Identifies the type of principal
     * @return
     */
    PrincipalType getPrincipalType();

}
