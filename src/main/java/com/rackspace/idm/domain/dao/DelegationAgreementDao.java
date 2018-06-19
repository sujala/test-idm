package com.rackspace.idm.domain.dao;

import com.rackspace.idm.api.resource.cloud.v20.FindDelegationAgreementParams;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationDelegate;
import com.rackspace.idm.domain.entity.DelegationPrincipal;
import com.unboundid.ldap.sdk.DN;

import java.util.List;

public interface DelegationAgreementDao {
    String getNextAgreementId();
    void addAgreement(DelegationAgreement delegationAgreement);
    void deleteAgreement(DelegationAgreement delegationAgreement);

    /**
     * Updates the existing delegation agreement
     *
     * @param delegationAgreement
     */
    void updateAgreement(DelegationAgreement delegationAgreement);

    DelegationAgreement getAgreementById(String agreementId);

    /**
     * Retrieves a list of delegation agreements given the specified search parameters. Will return a maximum of 100
     * results as a way to minimize impact of this service until better limits are put in place w/ respect to number
     * of DAs a user can create and be a delegate. If more than 100 are found, an error will be thrown.
     *
     * // TODO: Due to how DAs are currently loaded, the principal is automatically loaded when a DA is retrieved. When
     * returning a large set of DAs, this means a lot of additional searches. Need a way to specify whether or not to
     * automatically look up principals.
     *
     * @param findDelegationAgreementParams
     * @return
     */
    List<DelegationAgreement> findDelegationAgreements(FindDelegationAgreementParams findDelegationAgreementParams);

    /**
     * Retrieve the delegates listed on the delegation agreement. References to non-existant delegates are ignored, while
     * references to objects that are not delegates (e.g. linked to a Domain rather than a user, user group, or fed user)
     * will cause an IllegalStateException.
     *
     * @param delegationAgreement
     * @return
     *
     * @throws IllegalStateException if the DA contains a delegate reference that does not resolve to a permissible delegate
     */
    List<DelegationDelegate> getDelegationAgreementDelegates(DelegationAgreement delegationAgreement);

    /**
     * Returns a delegate referenced by the DN. Returns null if the DN does not resolve to any object.
     *
     * @param dn
     * @return
     * @throws IllegalStateException If the DN resolves to an object that can not be a delegate.
     */
    DelegationDelegate getDelegateByDn(DN dn);

    /**
     * Count the number of DAs for specified principal.
     *
     * @return
     * @param delegationPrincipal
     */
    int countNumberOfDelegationAgreementsByPrincipal(DelegationPrincipal delegationPrincipal);

    /**
     * Retrieves the direct child DelegationAgreements for the given DelegationAgreement.
     * Note: This does not return all the child DelegationAgreements. This only loads the child delegation agreements
     * one level deep.
     *
     * @param parentDelegationAgreementId
     * @return
     */
    Iterable<DelegationAgreement> getChildDelegationAgreements(String parentDelegationAgreementId);

}
