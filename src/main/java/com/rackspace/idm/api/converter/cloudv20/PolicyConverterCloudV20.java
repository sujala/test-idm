package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
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
public class PolicyConverterCloudV20 {
    @Autowired
    private JAXBObjectFactories objFactories;

    public Policy toPolicy(com.rackspace.idm.domain.entity.Policy policy) {
        Policy jaxbPolicy = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createPolicy();
        jaxbPolicy.setId(policy.getPolicyId());
        jaxbPolicy.setName(policy.getName());
        jaxbPolicy.setDescription(policy.getDescription());
        jaxbPolicy.setEnabled(policy.isEnabled());
        jaxbPolicy.setBlob(policy.getBlob());
        jaxbPolicy.setGlobal(policy.isGlobal());
        jaxbPolicy.setType(policy.getPolicyType());
        return jaxbPolicy;
    }

    public com.rackspace.idm.domain.entity.Policy toPolicyDO(Policy policy) {
        com.rackspace.idm.domain.entity.Policy policyDO = new com.rackspace.idm.domain.entity.Policy();
        policyDO.setPolicyId(policy.getId());
        policyDO.setName(policy.getName());
        policyDO.setDescription(policy.getDescription());
        policyDO.setEnabled(policy.isEnabled());
        policyDO.setBlob(policy.getBlob());
        policyDO.setGlobal(policy.isGlobal());
        policyDO.setPolicyType(policy.getType());
        return policyDO;
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }

    public Policy toPolicyForPolicies(com.rackspace.idm.domain.entity.Policy policy) {
        Policy jaxbPolicy = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createPolicy();
        jaxbPolicy.setId(policy.getPolicyId());
        jaxbPolicy.setName(policy.getName());
        jaxbPolicy.setEnabled(policy.isEnabled());
        jaxbPolicy.setGlobal(policy.isGlobal());
        jaxbPolicy.setType(policy.getPolicyType());
        return jaxbPolicy;
    }
}
