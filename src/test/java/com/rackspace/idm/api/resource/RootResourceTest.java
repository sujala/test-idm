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
    private ServiceProfileDescriptionBuilder serviceProfileDescriptionBuilder;
    private Configuration config;

    @Before
    public void setUp() throws Exception {
        cloudVersionsResource = mock(CloudVersionsResource.class);
        config = mock(Configuration.class);
        serviceProfileDescriptionBuilder = mock(ServiceProfileDescriptionBuilder.class);
        rootResource = new RootResource();
        rootResource.setCloudVersionResource(cloudVersionsResource);
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
