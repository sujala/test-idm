package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.DelegationAgreement;

public interface DelegationAgreementDao {
    String getNextAgreementId();
    void addAgreement(DelegationAgreement delegationAgreement);
    void deleteAgreement(DelegationAgreement delegationAgreement);
    DelegationAgreement getAgreementById(String agreementId);
}
