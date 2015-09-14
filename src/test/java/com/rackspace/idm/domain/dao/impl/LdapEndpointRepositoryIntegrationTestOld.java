package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.Application;
import org.junit.*;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.Policy;

import java.util.Random;

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

    @Autowired
    private ApplicationDao applicationDao;

    private String baseUrlId1 = getId();
    private String baseUrlId2 = getId();
    private String policyId1 = getId();
    private String policyId2 = getId();

    @Before
    public void cleanupBefore() {
        try {
            endpointRepository.deleteBaseUrl(baseUrlId1);
            endpointRepository.deleteBaseUrl(baseUrlId2);
            policyRepository.deletePolicy(policyId1);
            policyRepository.deletePolicy(policyId2);
        } catch (Exception e) {}
    }

    @After
    public void cleanupAfter() {
        try {
            endpointRepository.deleteBaseUrl(baseUrlId1);
            endpointRepository.deleteBaseUrl(baseUrlId2);
            policyRepository.deletePolicy(policyId1);
            policyRepository.deletePolicy(policyId2);
        } catch (Exception e) {}
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

    private CloudBaseUrl getCloudBaseUrl() {
        String serviceName = "test";
        Application app = applicationDao.getApplicationByName(serviceName);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlType("NAST");
        cloudBaseUrl.setServiceName(serviceName);
        cloudBaseUrl.setPublicUrl("http://example.com/" + getId());
        cloudBaseUrl.setBaseUrlId(baseUrlId1);
        cloudBaseUrl.setPublicUrlId(getId());
        cloudBaseUrl.setDef(false);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(false);
        cloudBaseUrl.setOpenstackType("cloudServers");
        cloudBaseUrl.setClientId(app.getClientId());
        return cloudBaseUrl;
    }

    private String getId() {
        Random r = new Random();
        return "" + ((int) (100000 + r.nextFloat() * 900000));
    }
}
