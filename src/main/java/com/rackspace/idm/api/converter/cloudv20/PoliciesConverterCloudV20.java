package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PolicyAlgorithm;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: jorge.munoz
 * Date: 9/6/12
 * Time: 7:51 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class PoliciesConverterCloudV20 {
    @Autowired
    private JAXBObjectFactories objFactories;
    @Autowired
    private PolicyConverterCloudV20 policyConverterCloudV20;

    public Policies toPolicies(com.rackspace.idm.domain.entity.Policies policies) {
        Policies jaxbPolicies = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createPolicies();
        jaxbPolicies.setAlgorithm(PolicyAlgorithm.IF_FALSE_DENY);
        for(com.rackspace.idm.domain.entity.Policy policy : policies.getPolicy()){
            Policy jaxbPolicy = policyConverterCloudV20.toPolicyForPolicies(policy);
            jaxbPolicies.getPolicy().add(jaxbPolicy);
        }
        return jaxbPolicies;
    }

    public com.rackspace.idm.domain.entity.Policies toPoliciesDO(Policies policies) {
        com.rackspace.idm.domain.entity.Policies policiesDO = new com.rackspace.idm.domain.entity.Policies();
        policiesDO.setAlgorithm(policies.getAlgorithm());
        for(Policy policy : policies.getPolicy()){
            com.rackspace.idm.domain.entity.Policy entityPolicy = policyConverterCloudV20.toPolicyDO(policy);
            policiesDO.getPolicy().add(entityPolicy);
        }
        return policiesDO;
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }

    public void setPolicyConverter(PolicyConverterCloudV20 policyConverter) {
        this.policyConverterCloudV20 = policyConverter;
    }
}
