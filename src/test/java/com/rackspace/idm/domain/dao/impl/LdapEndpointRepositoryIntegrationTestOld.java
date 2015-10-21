package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.Application;
import org.junit.*;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rackspace.idm.domain.entity.CloudBaseUrl;

import java.util.Random;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class LdapEndpointRepositoryIntegrationTestOld extends InMemoryLdapIntegrationTest {

    @Autowired
    private EndpointDao endpointRepository;

    @Autowired
    private ApplicationDao applicationDao;

    private String baseUrlId1 = getId();
    private String baseUrlId2 = getId();

    @Before
    public void cleanupBefore() {
        try {
            endpointRepository.deleteBaseUrl(baseUrlId1);
            endpointRepository.deleteBaseUrl(baseUrlId2);
        } catch (Exception e) {}
    }

    @After
    public void cleanupAfter() {
        try {
            endpointRepository.deleteBaseUrl(baseUrlId1);
            endpointRepository.deleteBaseUrl(baseUrlId2);
        } catch (Exception e) {}
    }

    @Test
    public void addBaseUrl_validBaseUrl_returnsBaseUrl() {
        CloudBaseUrl cloudBaseUrl = getCloudBaseUrl();
        endpointRepository.addBaseUrl(cloudBaseUrl);

        CloudBaseUrl repositoryCloudBaseUrl = endpointRepository.getBaseUrlById(baseUrlId1);

        assertThat("repositoryCloudBaseUrl", repositoryCloudBaseUrl, notNullValue());
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
