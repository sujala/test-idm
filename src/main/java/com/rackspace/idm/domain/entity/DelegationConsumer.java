package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.DN;

/**
 * Represents an entity that can be set as a delegate or principal on a delegation agreement (DA). Just because an
 * entity implements this interface does <b>not</b> mean it is actually a delegate or principal on any given DA, or
 * even any DA. It just means the entity could be set as a delegate or principal.
 */
public interface DelegationConsumer {

    /**
     * Retrieves the unique identifier of the DA consumer
     *
     * @return
     */
    String getId();

    /**
     * The DN of the consumer that will be stored on the DA.
     *
     * @return
     */
    DN getDn();

    /**
     * The domainId of the consumer
     *
     * @return
     */
    String getDomainId();

}
