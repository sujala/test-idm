package com.rackspace.idm.domain.entity;

/**
 * Represents a delegate
 */
public interface EndUserDelegate extends EndUser {
    DelegationAgreement getDelegationAgreement();
}
