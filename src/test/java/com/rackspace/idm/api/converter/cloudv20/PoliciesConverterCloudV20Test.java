package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.policy.v1.Policy;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Policies;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/13/12
 * Time: 4:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class PoliciesConverterCloudV20Test{

    PoliciesConverterCloudV20 policiesConverterCloudV20;
    PolicyConverterCloudV20 policyConverterCloudV20;
    JAXBObjectFactories objectFactories;


    @Before
    public void setUp() throws Exception {
        policiesConverterCloudV20 = new PoliciesConverterCloudV20();
        objectFactories = new JAXBObjectFactories();
        policiesConverterCloudV20.setObjFactories(objectFactories);
        policyConverterCloudV20 = new PolicyConverterCloudV20();
        policyConverterCloudV20.setObjFactories(objectFactories);
        policiesConverterCloudV20.setPolicyConverter(policyConverterCloudV20);
    }

    @Test
    public void testToPolicies() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies policies = policiesConverterCloudV20.toPolicies(getTestPoliciesDO());
        assertThat("check policy", policies.getPolicy().get(0).getId(),equalTo("1"));
        assertThat("check policy", policies.getPolicy().get(0).getName(),equalTo("name"));
        assertThat("check policy", policies.getPolicy().get(0).getBlob(),equalTo("blob"));
        assertThat("check policy", policies.getPolicy().get(0).getType(),equalTo("type"));
        assertThat("check policy", policies.getPolicy().get(0).getDescription(),equalTo("des"));
        assertThat("check policy", policies.getPolicy().get(0).isEnabled(),equalTo(true));
        assertThat("check policy", policies.getPolicy().get(0).isGlobal(),equalTo(false));

    }

    @Test
    public void testToPoliciesDO() throws Exception {
        Policies policies = policiesConverterCloudV20.toPoliciesDO(getTestPolicies());

        assertThat("check policy", policies.getPolicy().get(0).getPolicyId(),equalTo("1"));
        assertThat("check policy", policies.getPolicy().get(0).getName(),equalTo("name"));
        assertThat("check policy", policies.getPolicy().get(0).getBlob(),equalTo("blob"));
        assertThat("check policy", policies.getPolicy().get(0).getPolicyType(),equalTo("type"));
        assertThat("check policy", policies.getPolicy().get(0).getDescription(),equalTo("des"));
        assertThat("check policy", policies.getPolicy().get(0).isEnabled(),equalTo(true));
        assertThat("check policy", policies.getPolicy().get(0).isGlobal(),equalTo(false));
    }

    private com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies getTestPolicies(){
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies policies = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies();
        policies.getPolicy().add(getTestPolicy());
        return policies;
    }

    private Policies getTestPoliciesDO(){
        Policies policies = new Policies();
        policies.getPolicy().add(getTestPolicyDAO());
        return policies;
    }

    private com.rackspace.idm.domain.entity.Policy getTestPolicyDAO() {
        com.rackspace.idm.domain.entity.Policy policyDAO = new com.rackspace.idm.domain.entity.Policy();
        policyDAO.setName("name");
        policyDAO.setPolicyId("1");
        policyDAO.setBlob("blob");
        policyDAO.setPolicyType("type");
        policyDAO.setEnabled(true);
        policyDAO.setGlobal(false);
        policyDAO.setDescription("des");
        return policyDAO;
    }

    private com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy getTestPolicy(){
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policy = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy();
        policy.setId("1");
        policy.setBlob("blob");
        policy.setDescription("des");
        policy.setEnabled(true);
        policy.setGlobal(false);
        policy.setName("name");
        policy.setType("type");
        return policy;
    }


}
