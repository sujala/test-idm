package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Policy;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/13/12
 * Time: 3:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class PolicyConverterCloudV20Test{

    PolicyConverterCloudV20 policyConverterCloudV20;
    JAXBObjectFactories objectFactories;
    private final ObjectFactory rackspaceIdentityExtRaxgaV1Factory = mock(ObjectFactory.class);

    @Before
    public void setUp() throws Exception {
        policyConverterCloudV20 = new PolicyConverterCloudV20();
        objectFactories = new JAXBObjectFactories();
        policyConverterCloudV20.setObjFactories(objectFactories);
    }

    @Test
    public void testToPolicy() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policy = policyConverterCloudV20.toPolicy(getTestPolicyDAO());
        assertThat("check policy", policy.getId(),equalTo("id"));
        assertThat("check policy", policy.getName(),equalTo("name"));
        assertThat("check policy", policy.getBlob(),equalTo("blob"));
        assertThat("check policy", policy.getType(),equalTo("type"));
        assertThat("check policy", policy.getDescription(),equalTo("des"));
        assertThat("check policy", policy.isEnabled(),equalTo(true));
        assertThat("check policy", policy.isGlobal(),equalTo(false));


    }

    @Test
    public void testToPolicyDO() throws Exception {
        Policy policyDO = policyConverterCloudV20.toPolicyDO(getTestPolicy());
        assertThat("check policy", policyDO.getPolicyId(),equalTo("1"));
        assertThat("check policy", policyDO.getName(),equalTo("name"));
        assertThat("check policy", policyDO.getBlob(),equalTo("blob"));
        assertThat("check policy", policyDO.getPolicyType(),equalTo("type"));
        assertThat("check policy", policyDO.getDescription(),equalTo("Des"));
        assertThat("check policy", policyDO.isEnabled(),equalTo(true));
        assertThat("check policy", policyDO.isGlobal(),equalTo(false));
    }

    @Test
    public void testSetObjFactories() throws Exception {

    }

    private Policy getTestPolicyDAO() {
        Policy policyDAO = new Policy();
        policyDAO.setName("name");
        policyDAO.setPolicyId("id");
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
        policy.setDescription("Des");
        policy.setEnabled(true);
        policy.setGlobal(false);
        policy.setName("name");
        policy.setType("type");
        return policy;
    }
}
