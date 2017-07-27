package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.api.serviceprofile.ServiceDescriptionTemplateUtil
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.FileSystemApiDocRepository;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/6/12
 * Time: 9:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class Cloud20VersionResourceTestOld {
    def cloud20VersionResource;
    Configuration config;
    CloudContractDescriptionBuilder cloudContractDescriptionBuilder;
    FileSystemApiDocRepository fileSystemApiDocRepository;
    ServiceDescriptionTemplateUtil serviceDescriptionTemplateUtil;
    HttpHeaders httpHeaders;
    AuthenticationRequest authenticationRequest;
    DefaultCloud20Service defaultCloud20Service;
    UriInfo uriInfo;

    @Before
    public void setUp() throws Exception {
        // setting up for version resource
        fileSystemApiDocRepository = new FileSystemApiDocRepository(mock(IdentityConfig.class));
        serviceDescriptionTemplateUtil = new ServiceDescriptionTemplateUtil();
        config = mock(Configuration.class);
        cloudContractDescriptionBuilder = new CloudContractDescriptionBuilder(fileSystemApiDocRepository, serviceDescriptionTemplateUtil);

        cloud20VersionResource = new Cloud20VersionResource();
        cloud20VersionResource.config = config
        cloud20VersionResource.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder

        // mock
        httpHeaders = mock(HttpHeaders.class);
        authenticationRequest = mock(AuthenticationRequest.class);
        defaultCloud20Service = mock(DefaultCloud20Service.class);
        uriInfo = mock(UriInfo.class);

        IdentityConfig identityConfig = new IdentityConfig(mock(Configuration.class), mock(Configuration.class))

        // setter
        cloud20VersionResource.setCloud20Service(defaultCloud20Service);
        cloud20VersionResource.setIdentityConfig(identityConfig)
    }

    @Test
    public void setDefaultRegionServices_callsDefaultService_listDefaultRegionServices() throws Exception {
        DefaultRegionServices defaultRegionServices = new DefaultRegionServices();
        when(defaultCloud20Service.setDefaultRegionServices("token",defaultRegionServices)).thenReturn(Response.noContent());
        cloud20VersionResource.setDefaultRegionServices("token", defaultRegionServices);
        verify(defaultCloud20Service).setDefaultRegionServices("token", defaultRegionServices);
    }

    @Test
    public void getDefaultRegionServices_callsDefaultService_listDefaultRegionServices() throws Exception {
        when(defaultCloud20Service.listDefaultRegionServices("token")).thenReturn(Response.ok());
        cloud20VersionResource.listDefaultRegionServices("token");
        verify(defaultCloud20Service).listDefaultRegionServices("token");
    }

    @Test
    public void getCloud20VersionInfo_returnsVersionInfo() throws Exception {
        Response response = cloud20VersionResource.getCloud20VersionInfo();
        VersionChoice object =  (VersionChoice)response.getEntity();
        assertThat("version", object.getId(), equalTo("v2.0"));
    }

}
