package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationPrincipal;
import com.unboundid.ldap.sdk.DN;

import java.util.Set;

public interface DelegationService {

    /**
     * Adds a new delegation agreement (DA). Service assumes the caller has verified the DA is valid. The id of the passed
     * in delegation agreement will be updated with the unique id when saving. Any existing value in the field will
     * be overwritten.
     *
     * The returned DA may, or may not, be the same DA instance as passed in.
     *
     * @param delegationAgreement
     * @return
     */
    DelegationAgreement addDelegationAgreement(DelegationAgreement delegationAgreement);

    /**
     * Deletes the specified delegation agreement
     *
     * @param delegationAgreement
     */
    void deleteDelegationAgreement(DelegationAgreement delegationAgreement);

    /**
     * Retrieves the delegation agreement with the specified Id
     *
     * @param delegationAgreementId
     * @return
     */
    DelegationAgreement getDelegationAgreementById(String delegationAgreementId);
}
