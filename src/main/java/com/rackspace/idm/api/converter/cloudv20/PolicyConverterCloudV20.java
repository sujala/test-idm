package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PolicyAlgorithm;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import org.dozer.Mapper;
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
public class PolicyConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    public com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy toPolicy(com.rackspace.idm.domain.entity.Policy policy) {
          return mapper.map(policy, com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy.class);
    }

    public Policy fromPolicy(com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policyEntity) {
        Policy policy = mapper.map(policyEntity, Policy.class);
        policy.setEnabled(policyEntity.isEnabled());
        policy.setGlobal(policyEntity.isGlobal());
        return policy;
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }

    public com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy toPolicyForPolicies(Policy policyEntity) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policy = mapper.map(
                policyEntity, com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy.class
        );

        policy.setBlob(null);
        policy.setDescription(null);
        return policy;
    }

    public com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies toPolicies(Policies policies) {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies jaxbPolicies = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createPolicies();
        jaxbPolicies.setAlgorithm(PolicyAlgorithm.IF_FALSE_DENY);
        for(com.rackspace.idm.domain.entity.Policy policy : policies.getPolicy()){
            com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy jaxbPolicy = toPolicyForPolicies(policy);
            jaxbPolicies.getPolicy().add(jaxbPolicy);
        }
        return jaxbPolicies;
    }

    public Policies fromPolicies(com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies policies) {
        com.rackspace.idm.domain.entity.Policies policiesDO = new com.rackspace.idm.domain.entity.Policies();
        policiesDO.setAlgorithm(policies.getAlgorithm());
        for(com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policy : policies.getPolicy()){
            com.rackspace.idm.domain.entity.Policy entityPolicy = fromPolicy(policy);
            policiesDO.getPolicy().add(entityPolicy);
        }
        return policiesDO;
    }
}
