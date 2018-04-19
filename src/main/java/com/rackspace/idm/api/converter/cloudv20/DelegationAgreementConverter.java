package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateReference;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegateReferences;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreements;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.DelegationDelegate;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.service.DelegationService;
import com.rackspace.idm.domain.service.IdentityUserService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DelegationAgreementConverter {
    @Autowired
    private Mapper mapper;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private DelegationService delegationService;

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private Configuration config;

    /**
     * Converts from the request/response web service representation of a userGroup to the LDAP based representation.
     *
     * @param delegationAgreementWeb
     * @return
     */
    public com.rackspace.idm.domain.entity.DelegationAgreement fromDelegationAgreementWeb(DelegationAgreement delegationAgreementWeb) {
        com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = mapper.map(delegationAgreementWeb, com.rackspace.idm.domain.entity.DelegationAgreement.class);
        delegationAgreement.setAllowSubAgreements(delegationAgreementWeb.isAllowSubAgreements());

        return delegationAgreement;
    }

    public DelegationAgreement toDelegationAgreementWeb(com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreementEntity) {
        DelegationAgreement delegationAgreement = mapper.map(delegationAgreementEntity, DelegationAgreement.class);

        delegationAgreement.setAllowSubAgreements(delegationAgreementEntity.getAllowSubAgreements());
        delegationAgreement.setPrincipalType(delegationAgreementEntity.getPrincipalType());
        delegationAgreement.setPrincipalId(delegationAgreementEntity.getPrincipalId());

        if (CollectionUtils.isNotEmpty(delegationAgreementEntity.getDelegates())) {
            delegationAgreement.setDelegateId(delegationAgreementEntity.getFirstDelegateId());
        }

        return delegationAgreement;
    }

    public DelegationAgreements toDelegationAgreementsWeb(List<com.rackspace.idm.domain.entity.DelegationAgreement> delegationAgreements) {
        DelegationAgreements agreementsWeb = new DelegationAgreements();
        for (com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement : delegationAgreements) {
            agreementsWeb.getDelegationAgreement().add(toDelegationAgreementWeb(delegationAgreement));
        }
        return agreementsWeb;
    }

    public DelegateReferences toDelegatesWeb(List<DelegationDelegate> delegationDelegates) {
        DelegateReferences delegateReferences = new DelegateReferences();
        for (DelegationDelegate delegationDelegate : delegationDelegates) {
            com.rackspace.idm.api.resource.cloud.v20.DelegateReference entityRef = delegationDelegate.getDelegateReference();

            DelegateReference ref = new DelegateReference();
            ref.setDelegateId(entityRef.getId());
            ref.setDelegateType(entityRef.getDelegateType().toWebType());
            delegateReferences.getDelegateReference().add(ref);
        }
        return delegateReferences;
    }
}
