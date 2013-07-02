package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.api.serviceprofile.ServiceDescriptionTemplateUtil;
import com.rackspace.idm.domain.dao.impl.FileSystemApiDocRepository;
import com.rackspace.idm.exception.NotFoundException;
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
    Cloud20VersionResource spy;
    DefaultCloud20Service defaultCloud20Service;
    UriInfo uriInfo;

    @Before
    public void setUp() throws Exception {
        // setting up for version resource
        fileSystemApiDocRepository = new FileSystemApiDocRepository();
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

        // setter
        cloud20VersionResource.setCloud20Service(defaultCloud20Service);

        spy = spy(cloud20VersionResource);
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

    @Test
    public void impersonate_callsCloud20Service_callsImpersonate() throws Exception {
        when(defaultCloud20Service.impersonate(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.impersonate(httpHeaders, null, null);
        verify(defaultCloud20Service).impersonate(httpHeaders, null, null);
    }

    @Test
    public void impersonate_responseOk_returns200() throws Exception {
        when(defaultCloud20Service.impersonate(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.impersonate(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test (expected = NotFoundException.class)
    public void deleteSOftDeletedUser_softDeleteNotAllowed_thrwosNotFoundException() throws Exception {
        spy.deleteSoftDeletedUser(httpHeaders, null, null);
    }

    @Test
    public void listUsersWithRole_responseOk_returns200() throws Exception {
        when(defaultCloud20Service.listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null)).thenReturn(Response.ok());
        Response response = spy.listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null);
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void listUsersWithRole_callsDefaultCloud20Service_listUsersWithRole() throws Exception {
        when(defaultCloud20Service.listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null)).thenReturn(Response.ok());
        spy.listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null);
        verify(defaultCloud20Service).listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null);
    }

    @Test
    public void revokeToken_callsDefaultCloud20Service() throws Exception {
        String token = "1234567890";
        when(defaultCloud20Service.revokeToken(httpHeaders, token)).thenReturn(Response.ok());
        spy.revokeToken(httpHeaders, token);
        verify(defaultCloud20Service).revokeToken(httpHeaders, token);
    }

    @Test
    public void revokeUserToken_callsDefaultCloud20Service() throws Exception {
        String token = "1234567890";
        when(defaultCloud20Service.revokeToken(httpHeaders, token, token)).thenReturn(Response.ok());
        spy.revokeUserToken(httpHeaders, token, token);
        verify(defaultCloud20Service).revokeToken(httpHeaders, token, token);
    }

}
