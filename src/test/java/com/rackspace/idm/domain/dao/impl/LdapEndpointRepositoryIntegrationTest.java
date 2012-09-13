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
    private String policyId = "100000000";

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

        Policy repositoryPolicy = policyRepository.getPolicy(policyId);
        if (repositoryPolicy != null) {
            policyRepository.deletePolicy(policyId);
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

        Policy policy = getPolicy();
        policyRepository.addPolicy(policy);

        endpointRepository.addPolicyToEndpoint(baseUrlId, policy.getPolicyId());

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);

        assertThat("repositoryCloudBaseUrl", cloudBaseUrl.getPolicyList().get(0), equalTo(policyId));
    }

    @Test(expected = NotFoundException.class)
    public void addPolicyToBaseUrl_withoutBaseUrl_containsPolicy() {
        Policy policy = getPolicy();
        policyRepository.addPolicy(policy);

        endpointRepository.addPolicyToEndpoint(baseUrlId, policy.getPolicyId());
    }

    @Test
    public void deletePolicyToBaseUrl_validBaseUrlAndPolicy_doesNotContainsPolicy() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        Policy policy = getPolicy();
        policyRepository.addPolicy(policy);

        endpointRepository.addPolicyToEndpoint(Integer.valueOf(cloudBaseUrl.getBaseUrlId()), policy.getPolicyId());
        endpointRepository.deletePolicyFromEndpoint(Integer.valueOf(cloudBaseUrl.getBaseUrlId()), policy.getPolicyId());

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId);

        assertThat("repositoryCloudBaseUrl", cloudBaseUrl.getPolicyList().size(), equalTo(0));
    }

    private Policy getPolicy() {
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
