package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.dao.PolicyDao;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class LdapEndpointRepositoryIntegrationTestOld extends InMemoryLdapIntegrationTest {

    @Autowired
    private EndpointDao endpointRepository;
    @Autowired
    private PolicyDao policyRepository;

    private String baseUrlId1 = "100000000";
    private String baseUrlId2 = "1010110110";
    private String policyId1 = "100000000";
    private String policyId2 = "100000002";

    @Before
    public void preTestSetUp() throws Exception {
        CloudBaseUrl repositoryCloudBaseUrl1 = endpointRepository.getBaseUrlById(baseUrlId1);
        if (repositoryCloudBaseUrl1 != null) {
            endpointRepository.deleteBaseUrl(baseUrlId1);
        }

        CloudBaseUrl repositoryCloudBaseUrl2 = endpointRepository.getBaseUrlById(baseUrlId2);
        if (repositoryCloudBaseUrl2 != null) {
            endpointRepository.deleteBaseUrl(baseUrlId2);
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

    @Test
    public void addBaseUrl_validBaseUrl_returnsBaseUrl() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        CloudBaseUrl repositoryCloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId1);

        assertThat("repositoryCloudBaseUrl", repositoryCloudBaseUrl, notNullValue());
    }

    @Test
    public void updatePolicyToBaseUrl_noPolicy_addsPolicy() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        Policy policy1 = getPolicy(policyId1);
        policyRepository.addPolicy(policy1);

        Policy policy2 = getPolicy(policyId2);
        policyRepository.addPolicy(policy2);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId1);
        cloudBaseUrl.getPolicyList().add(policyId1);
        cloudBaseUrl.getPolicyList().add(policyId2);

        endpointRepository.updateCloudBaseUrl(cloudBaseUrl);

        cloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId1);

        assertThat("repositoryCloudBaseUrl", cloudBaseUrl.getPolicyList().size(), equalTo(2));
    }

    @Test
    public void getBaseUrlsWithPolicyId_doesNotContainsPolicy_returnsEmptyList() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        Iterable<CloudBaseUrl> baseUrls = endpointRepository.getBaseUrlsWithPolicyId(policyId1);

        assertThat("repositoryCloudBaseUrl", baseUrls.iterator().hasNext(), equalTo(false));
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

    private CloudBaseUrl getCloudBaseUrl(Integer baseUrlId) {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlType("MOSSO");
        cloudBaseUrl.setServiceName("test");
        cloudBaseUrl.setPublicUrl("test");
        cloudBaseUrl.setBaseUrlId(String.valueOf(baseUrlId));
        cloudBaseUrl.setDef(false);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(false);
        cloudBaseUrl.setOpenstackType("cloudServers");
        return cloudBaseUrl;
    }


    private CloudBaseUrl getCloudBaseUrl() {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlType("NAST");
        cloudBaseUrl.setServiceName("test");
        cloudBaseUrl.setPublicUrl("test");
        cloudBaseUrl.setBaseUrlId(baseUrlId1);
        cloudBaseUrl.setDef(false);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(false);
        cloudBaseUrl.setOpenstackType("cloudServers");
        return cloudBaseUrl;
    }
}
