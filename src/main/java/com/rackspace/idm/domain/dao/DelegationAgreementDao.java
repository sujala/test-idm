package com.rackspace.idm.domain.dao;

import com.rackspace.idm.api.resource.cloud.v20.FindDelegationAgreementParams;
import com.rackspace.idm.domain.entity.DelegationAgreement;

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
}
