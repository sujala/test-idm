package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class LdapEndpointRepositoryIntegrationTest extends InMemoryLdapIntegrationTest {

    private static LdapEndpointRepository endpointRepository;
    private static LdapPolicyRepository policyRepository;
    private static LdapConnectionPools connectionPools;

    private int baseUrlId = 100000000;
    private String policyId1 = "100000000";
    private String policyId2 = "100000002";

    @BeforeClass
    public static void setUp() {
        connectionPools = new LdapConfiguration(new PropertyFileConfiguration().getConfig()).connectionPools();
        endpointRepository = new LdapEndpointRepository(connectionPools, new PropertyFileConfiguration().getConfig());
        policyRepository = new LdapPolicyRepository(connectionPools, new PropertyFileConfiguration().getConfig());
    }

    @Before
    public void preTestSetUp() throws Exception {
        CloudBaseUrl repositoryCloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);
        if (repositoryCloudBaseUrl != null) {
            endpointRepository.deleteBaseUrl(baseUrlId);
        }

        Policy repositoryPolicy1 = policyRepository.getPolicy(policyId1);
        if (repositoryPolicy1 != null) {
            policyRepository.deletePolicy(policyId1);
        }

        Policy repositoryPolicy2 = policyRepository.getPolicy(policyId2);
        if (repositoryPolicy2 != null) {
            policyRepository.deletePolicy(policyId2);
        }
    }

    @AfterClass
    public static void tearDown() {
        connectionPools.close();
    }

    @Test
    public void addBaseUrl_validBaseUrl_returnsBaseUrl() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        CloudBaseUrl repositoryCloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);

        assertThat("repositoryCloudBaseUrl", repositoryCloudBaseUrl, notNullValue());
    }

    @Test
    public void addPolicyToBaseUrl_validBaseUrlAndPolicy_containsPolicy() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        Policy policy = getPolicy(policyId1);
        policyRepository.addPolicy(policy);

        endpointRepository.addPolicyToEndpoint(baseUrlId, policy.getPolicyId());

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);

        assertThat("repositoryCloudBaseUrl", cloudBaseUrl.getPolicyList().get(0), equalTo(policyId1));
    }

    @Test(expected = NotFoundException.class)
    public void addPolicyToBaseUrl_withoutBaseUrl_containsPolicy() {
        Policy policy = getPolicy(policyId1);
        policyRepository.addPolicy(policy);

        endpointRepository.addPolicyToEndpoint(baseUrlId, policy.getPolicyId());
    }

    @Test
    public void deletePolicyToBaseUrl_validBaseUrlAndPolicy_doesNotContainsPolicy() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        Policy policy = getPolicy(policyId1);
        policyRepository.addPolicy(policy);

        endpointRepository.addPolicyToEndpoint(Integer.valueOf(cloudBaseUrl.getBaseUrlId()), policy.getPolicyId());
        endpointRepository.deletePolicyFromEndpoint(Integer.valueOf(cloudBaseUrl.getBaseUrlId()), policy.getPolicyId());

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);

        assertThat("repositoryCloudBaseUrl", cloudBaseUrl.getPolicyList().size(), equalTo(0));
    }

    @Test
    public void updatePolicyToBaseUrl_noPolicy_addsPolicy() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        Policy policy1 = getPolicy(policyId1);
        policyRepository.addPolicy(policy1);

        Policy policy2 = getPolicy(policyId2);
        policyRepository.addPolicy(policy2);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);
        cloudBaseUrl.getPolicyList().add(policyId1);
        cloudBaseUrl.getPolicyList().add(policyId2);

        endpointRepository.updateCloudBaseUrl(cloudBaseUrl);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);

        assertThat("repositoryCloudBaseUrl", cloudBaseUrl.getPolicyList().size(), equalTo(2));
    }

    @Test
    public void updatePolicyToBaseUrl_withPolicy_removesPolicy() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        Policy policy1 = getPolicy(policyId1);
        policyRepository.addPolicy(policy1);

        Policy policy2 = getPolicy(policyId2);
        policyRepository.addPolicy(policy2);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);
        endpointRepository.addPolicyToEndpoint(baseUrlId, policyId1);
        endpointRepository.addPolicyToEndpoint(baseUrlId, policyId2);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);
        cloudBaseUrl.getPolicyList().clear();

        endpointRepository.updateCloudBaseUrl(cloudBaseUrl);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);

        assertThat("repositoryCloudBaseUrl", cloudBaseUrl.getPolicyList().size(), equalTo(0));
    }

    @Test
    public void updatePolicyToBaseUrl_withPolicy_removesPolicyAndAdds() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        Policy policy1 = getPolicy(policyId1);
        policyRepository.addPolicy(policy1);

        Policy policy2 = getPolicy(policyId2);
        policyRepository.addPolicy(policy2);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);
        endpointRepository.addPolicyToEndpoint(baseUrlId, policyId1);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);
        cloudBaseUrl.getPolicyList().clear();
        cloudBaseUrl.getPolicyList().add(policyId2);

        endpointRepository.updateCloudBaseUrl(cloudBaseUrl);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);

        assertThat("repositoryCloudBaseUrl", cloudBaseUrl.getPolicyList().size(), equalTo(1));
    }

    private Policy getPolicy(String policyId) {
        Policy policy = new Policy();
        policy.setBlob("blob");
        policy.setDescription("description");
        policy.setEnabled(true);
        policy.setGlobal(true);
        policy.setName("name");
        policy.setPolicyType("type");
        policy.setPolicyId(policyId);
        return policy;
    }

    private CloudBaseUrl getCloudBaseUrl() {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlType("test");
        cloudBaseUrl.setServiceName("test");
        cloudBaseUrl.setPublicUrl("test");
        cloudBaseUrl.setBaseUrlId(baseUrlId);

        return cloudBaseUrl;
    }
}
