package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.DelegationAgreementDao;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.service.DelegationService;
import org.eclipse.persistence.jpa.jpql.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultDelegationService implements DelegationService {
    @Autowired
    private DelegationAgreementDao delegationAgreementDao;

    @Override
    public DelegationAgreement addDelegationAgreement(DelegationAgreement delegationAgreement) {
        Assert.isNotNull(delegationAgreement, "Delegation agreement must be provided");
        delegationAgreementDao.addAgreement(delegationAgreement);

        return delegationAgreement;
    }

    @Override
    public DelegationAgreement getDelegationAgreementById(String delegationAgreementId) {
        return delegationAgreementDao.getAgreementById(delegationAgreementId);
    }

    @Override
    public void deleteDelegationAgreement(DelegationAgreement delegationAgreement) {
        delegationAgreementDao.deleteAgreement(delegationAgreement);
    }
}
