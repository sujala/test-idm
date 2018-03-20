package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.service.DelegationService;
import com.rackspace.idm.domain.service.IdentityUserService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

        return delegationAgreement;
    }

    public DelegationAgreement toDelegationAgreementWeb(com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreementEntity) {
        DelegationAgreement delegationAgreement = mapper.map(delegationAgreementEntity, DelegationAgreement.class);

        delegationAgreement.setPrincipalType(delegationAgreementEntity.getPrincipalType());
        delegationAgreement.setPrincipalId(delegationAgreementEntity.getPrincipalId());

        if (CollectionUtils.isNotEmpty(delegationAgreementEntity.getDelegates())) {
            delegationAgreement.setDelegateId(delegationAgreementEntity.getFirstDelegateId());
        }

        return delegationAgreement;
    }

}
