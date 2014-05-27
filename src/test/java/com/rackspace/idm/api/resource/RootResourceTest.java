package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.cloud.CloudVersionsResource;
import com.rackspace.idm.api.serviceprofile.ServiceProfileDescriptionBuilder;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/15/12
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class RootResourceTest {
    private RootResource rootResource;
    private CloudVersionsResource cloudVersionsResource;
    private Version10Resource version10Resource;
    private ServiceProfileDescriptionBuilder serviceProfileDescriptionBuilder;
    private Configuration config;

    @Before
    public void setUp() throws Exception {
        cloudVersionsResource = mock(CloudVersionsResource.class);
        version10Resource = mock(Version10Resource.class);
        config = mock(Configuration.class);
        serviceProfileDescriptionBuilder = mock(ServiceProfileDescriptionBuilder.class);
        rootResource = new RootResource();
        rootResource.setCloudVersionResource(cloudVersionsResource);
        rootResource.setVersion10Resource(version10Resource);
        rootResource.setConfig(config);
        rootResource.setServiceProfileDescriptionBuilder(serviceProfileDescriptionBuilder);
    }

    @Test
    public void getPublicServiceProfile_serviceProfileDescriptionBuilder_buildPublicServiceProfile() throws Exception {
        rootResource.getPublicServiceProfile();
        verify(serviceProfileDescriptionBuilder).buildPublicServiceProfile(any(UriInfo.class));
    }

    @Test
    public void getPublicServiceProfile_statusOk_returns200() throws Exception {
        Response response = rootResource.getPublicServiceProfile();
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void getCloudVersionsResource_returnsCloudVersionResource() throws Exception {
        CloudVersionsResource resource = rootResource.getCloudVersionsResource();
        assertThat("cloud resource", resource, equalTo(cloudVersionsResource));
    }

    @Test
    public void getBuildInfo_callsConfig_getStringVersion() throws Exception {
        when(config.getString("version")).thenReturn("version");
        when(config.getString("buildVersion")).thenReturn("build");
        String buildInfo = rootResource.getBuildInfo();
        assertThat("version", buildInfo, equalTo("{\"build\":\"build\",\"version\":\"version\"}"));
    }

    @Test
    public void getVersionResource_versionIdMatchesV10_returnsVersionResource() throws Exception {
        when(config.getBoolean("feature.access.to.foundation.api", true)).thenReturn(true);
        Version10Resource resource = rootResource.getVersionResource("v1.0");
        assertThat("version resource", resource, equalTo(version10Resource));
    }

    @Test
    public void getVersionResource_versionIdMatchesV1_returnsVersionResource() throws Exception {
        when(config.getBoolean("feature.access.to.foundation.api", true)).thenReturn(true);
        Version10Resource resource = rootResource.getVersionResource("v1");
        assertThat("version resource", resource, equalTo(version10Resource));
    }

    @Test (expected = NotFoundException.class)
    public void getVersionResource_versionIdDoesNotMatch_throwsNotFoundException() throws Exception {
        rootResource.getVersionResource("notMatch");
    }

    @Test
    public void getInternalServiceProfile_callsServiceProfileDescriptionBuilder_buildInternalServiceProfile() throws Exception {
        rootResource.getInternalServiceProfile();
        verify(serviceProfileDescriptionBuilder).buildInternalServiceProfile(any(UriInfo.class));
    }

    @Test
    public void getInternalServiceProfile_responseOk_returns200() throws Exception {
        Response response = rootResource.getInternalServiceProfile();
        assertThat("response code", response.getStatus(), equalTo(200));
    }
}
