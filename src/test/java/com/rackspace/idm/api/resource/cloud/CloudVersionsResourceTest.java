package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.resource.cloud.v10.Cloud10VersionResource;
import com.rackspace.idm.api.resource.cloud.v11.Cloud11VersionResource;
import com.rackspace.idm.api.resource.cloud.v20.Cloud20VersionResource;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.api.serviceprofile.ServiceDescriptionTemplateUtil;
import com.rackspace.idm.domain.dao.impl.FileSystemApiDocRepository;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/10/12
 * Time: 1:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudVersionsResourceTest {
    CloudVersionsResource cloudVersionsResource;
    CloudClient cloudClient;
    Cloud10VersionResource cloud10VersionResource;
    Cloud11VersionResource cloud11VersionResource;
    Cloud20VersionResource cloud20VersionResource;
    Configuration config;
    CloudContractDescriptionBuilder cloudContractDescriptionBuilder;
    FileSystemApiDocRepository fileSystemApiDocRepository;
    ServiceDescriptionTemplateUtil serviceDescriptionTemplateUtil;

    @Before
    public void setUp() throws Exception {
        serviceDescriptionTemplateUtil = new ServiceDescriptionTemplateUtil();
        fileSystemApiDocRepository = new FileSystemApiDocRepository();
        cloudClient = mock(CloudClient.class);
        cloud10VersionResource = mock(Cloud10VersionResource.class);
        cloud11VersionResource = mock(Cloud11VersionResource.class);
        cloud20VersionResource = mock(Cloud20VersionResource.class);
        config = mock(Configuration.class);
        cloudContractDescriptionBuilder = new CloudContractDescriptionBuilder(fileSystemApiDocRepository, serviceDescriptionTemplateUtil);

        cloudVersionsResource = new CloudVersionsResource(cloud10VersionResource, cloud11VersionResource, cloud20VersionResource, config, cloudContractDescriptionBuilder, cloudClient);
    }

    @Test
    public void getInternalCloudVersionsInfo_responseOk_returns200() throws Exception {
        Response result = cloudVersionsResource.getInternalCloudVersionsInfo();
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getPublicCloudVersionsInfo_responseOk_returns200() throws Exception {
        when(config.getString("cloudAuth20url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v2.0/");
        when(cloudClient.get(anyString(), any(HttpHeaders.class))).thenReturn(Response.ok());
        Response result = cloudVersionsResource.getPublicCloudVersionsInfo(null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getCloud10AuthResource_returnsCloud10VersionResource() throws Exception {
        Cloud10VersionResource result = cloudVersionsResource.getCloud10AuthResource();
        assertThat("resource", result instanceof Cloud10VersionResource, equalTo(true));
    }

    @Test
    public void getCloud10VersionResource_returnsCloud10VersionResource() throws Exception {
        Cloud10VersionResource result = cloudVersionsResource.getCloud10VersionResource();
        assertThat("resource", result instanceof Cloud10VersionResource, equalTo(true));
    }

    @Test
    public void getCloud11VersionResource_returnsCloud11VersionResource() throws Exception {
        Cloud11VersionResource result = cloudVersionsResource.getCloud11VersionResource();
        assertThat("resource", result instanceof Cloud11VersionResource, equalTo(true));
    }

    @Test
    public void getCloud20VersionResource_returnsCloud20VersionResource() throws Exception {
        Cloud20VersionResource result = cloudVersionsResource.getCloud20VersionResource();
        assertThat("resource", result instanceof Cloud20VersionResource, equalTo(true));
    }
}
