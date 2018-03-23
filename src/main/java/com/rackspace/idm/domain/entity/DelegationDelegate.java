package com.rackspace.idm.domain.entity;

import com.rackspace.idm.api.resource.cloud.v20.DelegateReference;
import com.unboundid.ldap.sdk.DN;

/**
 * Represents an entity that can be set as a delegate on a delegation agreement (DA). Just because an entity implements
 * this interface does <b>not</b> mean it is actually a delegate on any given DA, or even any DA. It just means the
 * entity could be set as a delegate.
 */
public interface DelegationDelegate {
    /**
     * Identifies unique reference to the delegate
     *
     * @return
     */
    DelegateReference getDelegateReference();

    /**
     * The DN of the delegate that will be stored in the list of delegates on the delegation agreement.
     *
     * @return
     */
    DN getDn();

    /**
     * The domainId of the delegate
     *
     * @return
     */
    String getDomainId();
}
