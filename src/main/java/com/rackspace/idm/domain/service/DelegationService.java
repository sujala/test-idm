package com.rackspace.idm.domain.service;

import com.rackspace.idm.api.resource.cloud.v20.DelegateReference;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationDelegate;
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

    /**
     * Given a reference to a delegate, look up the delegate.
     *
     * @param delegateReference
     * @return
     */
    DelegationDelegate getDelegateByReference(DelegateReference delegateReference);

    /**
     * Adds the delegate to the delegation agreement. This service does not validate that the delegate is appropriate
     * for the delegation agreement. Such checks need to be made prior to calling this service.
     *
     * The passed in delegation agreement will be modified to reflect the change and used to update the backend. As
     * such, no changes should be made to the passed in DA by the caller.
     *
     * @param delegationAgreement
     * @param delegate
     */
    void addDelegate(DelegationAgreement delegationAgreement, DelegationDelegate delegate);

    /**
     * Deletes the specified delegate from the delegation agreement if it exists. Returns true if the delegate existed
     * on the DA, false otherwise.
     *
     * The passed in delegation agreement will be modified to reflect the change and used to update the backend. As
     * such, no changes should be made to the passed in DA by the caller.
     *
     * @param delegationAgreement
     * @param delegateReference
     */
    boolean deleteDelegate(DelegationAgreement delegationAgreement, DelegateReference delegateReference);
}
