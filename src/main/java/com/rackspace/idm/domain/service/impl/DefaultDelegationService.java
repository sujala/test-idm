package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.DelegationAgreementDao;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationPrincipal;
import com.rackspace.idm.domain.service.DelegationService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.Validator20;
import com.unboundid.ldap.sdk.DN;
import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.jpa.jpql.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DefaultDelegationService implements DelegationService {
    @Autowired
    private DelegationAgreementDao delegationAgreementDao;

    @Autowired
    private Validator20 validator20;

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
