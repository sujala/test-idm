package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Hector
 * Date: 8/1/12
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultRegionServiceTest {

    DefaultRegionService defaultRegionService = new DefaultRegionService();
    ScopeAccessService scopeAccessService = mock(ScopeAccessService.class);
    EndpointService endpointService = mock(EndpointService.class);
    ApplicationService applicationService = mock(ApplicationService.class);

    @Before
    public void setUp() throws Exception {
        defaultRegionService.setScopeAccessService(scopeAccessService);
        defaultRegionService.setEndpointService(endpointService);
        defaultRegionService.setApplicationService(applicationService);
    }

    @Test
    public void testGetRegionList_callsScopeAccessService_getOpenstackEndpointsForScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByUserId(null)).thenReturn(scopeAccess);
        defaultRegionService.getRegionList(null);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(scopeAccess);
    }

    @Test
    public void testGetRegionList_ScopeAccessNotFound_returnsEmptyList() throws Exception {
        when(scopeAccessService.getScopeAccessByUserId(null)).thenReturn(null);
        Set<String> regionList = defaultRegionService.getRegionList(null);
        assertThat("region list", regionList.size(), equalTo(0));
    }

    @Test
    public void testGetRegionList_returnsAllRegions() throws Exception {
        ArrayList<OpenstackEndpoint> openstackEndpoints = new ArrayList<OpenstackEndpoint>();

        OpenstackEndpoint endpoint1 = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls1 = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl baseUrl1 = new CloudBaseUrl();
        baseUrl1.setRegion("DFW");
        baseUrls1.add(baseUrl1);
        endpoint1.setBaseUrls(baseUrls1);

        OpenstackEndpoint endpoint2 = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls2 = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl baseUrl2 = new CloudBaseUrl();
        baseUrl2.setRegion("ORD");
        baseUrls2.add(baseUrl2);
        endpoint2.setBaseUrls(baseUrls2);

        OpenstackEndpoint endpoint3 = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls3 = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl baseUrl3 = new CloudBaseUrl();
        baseUrl3.setRegion("LON");
        baseUrls3.add(baseUrl3);
        endpoint3.setBaseUrls(baseUrls3);

        openstackEndpoints.add(endpoint1);
        openstackEndpoints.add(endpoint2);
        openstackEndpoints.add(endpoint3);

        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(Matchers.any(ScopeAccess.class))).thenReturn(openstackEndpoints);
        Set<String> regionList = defaultRegionService.getRegionList(null);
        assertThat("region list", regionList.size(), equalTo(3));
        assertThat("region list", regionList, hasItem("DFW"));
        assertThat("region list", regionList, hasItem("ORD"));
        assertThat("region list", regionList, hasItem("LON"));
    }

    @Test
    public void testGetRegionList_IgnoresDuplicates() throws Exception {
        ArrayList<OpenstackEndpoint> openstackEndpoints = new ArrayList<OpenstackEndpoint>();

        OpenstackEndpoint endpoint1 = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls1 = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl baseUrl1 = new CloudBaseUrl();
        baseUrl1.setRegion("DFW");
        baseUrls1.add(baseUrl1);
        endpoint1.setBaseUrls(baseUrls1);

        OpenstackEndpoint endpoint2 = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls2 = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl baseUrl2 = new CloudBaseUrl();
        baseUrl2.setRegion("DFW");
        baseUrls2.add(baseUrl2);
        endpoint2.setBaseUrls(baseUrls2);

        OpenstackEndpoint endpoint3 = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls3 = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl baseUrl3 = new CloudBaseUrl();
        baseUrl3.setRegion("LON");
        baseUrls3.add(baseUrl3);
        endpoint3.setBaseUrls(baseUrls3);

        openstackEndpoints.add(endpoint1);
        openstackEndpoints.add(endpoint2);
        openstackEndpoints.add(endpoint3);

        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(Matchers.any(ScopeAccess.class))).thenReturn(openstackEndpoints);
        Set<String> regionList = defaultRegionService.getRegionList(null);
        assertThat("region list", regionList.size(), equalTo(2));
        assertThat("region list", regionList, hasItem("DFW"));
        assertThat("region list", regionList, hasItem("LON"));
    }

    @Test
    public void testGetRegionList_callsScopeAccessService_getScopeAccessByUserId() throws Exception {
        defaultRegionService.getRegionList(null);
        verify(scopeAccessService).getScopeAccessByUserId(null);
    }

    @Test
    public void testGetRegionList_returnsNonNullValue() throws Exception {
        Set<String> regionList = defaultRegionService.getRegionList(null);
        assertThat("region list", regionList, IsNull.notNullValue());
    }

    @Test
    public void testGetRegionList_returnsSetType() throws Exception {
        Set<String> regionList = defaultRegionService.getRegionList(null);
        assertThat("region list", regionList, Is.is(Set.class));
    }

    @Test
    public void getDefaultRegions_returnsNonNullValue() throws Exception {
        Set<String> regionList = defaultRegionService.getDefaultRegions();
        assertThat("region list", regionList, IsNull.notNullValue());
    }

    @Test
    public void getDefaultRegions_returnsSetType() throws Exception {
        Set<String> regionList = defaultRegionService.getDefaultRegions();
        assertThat("region list", regionList, Is.is(Set.class));
    }

    @Test
    public void getDefaultRegions_noOpenStackApplications_returnsEmptyList() throws Exception {
        when(applicationService.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        Set<String> regionList = defaultRegionService.getDefaultRegions();
        assertThat("region list", regionList.size(), equalTo(0));
    }

    @Test
    public void getDefaultRegions_nullOpenStackApplications_returnsEmptyList() throws Exception {
        when(applicationService.getOpenStackServices()).thenReturn(null);
        Set<String> regionList = defaultRegionService.getDefaultRegions();
        assertThat("region list", regionList.size(), equalTo(0));
    }

    @Test
    public void getDefaultRegions_callsEndpointService_forEachDefaultService() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();

        Application application1 = new Application();
        application1.setName("CloudFiles");
        application1.setUseForDefaultRegion(true);
        applications.add(application1);

        Application application2 = new Application();
        application2.setName("CloudServers");
        application2.setUseForDefaultRegion(true);
        applications.add(application2);

        Application application3 = new Application();
        application3.setName("CloudFilesCDN");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);

        when(applicationService.getOpenStackServices()).thenReturn(applications);
        defaultRegionService.getDefaultRegions();
        verify(endpointService,times(3)).getBaseUrlsByServiceName(anyString());
    }

    @Test
    public void getDefaultRegions_callsEndpointService_onlyForDefaultRegions() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();

        Application application1 = new Application();
        application1.setName("CloudFiles");
        application1.setUseForDefaultRegion(true);
        applications.add(application1);

        Application application2 = new Application();
        application2.setName("CloudServers");
        application2.setUseForDefaultRegion(true);
        applications.add(application2);

        Application application3 = new Application();
        application3.setName("CloudFilesCDN");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);

        Application application4 = new Application();
        application4.setName("CloudFilesCDN2");
        application4.setUseForDefaultRegion(false);
        applications.add(application4);

        when(applicationService.getOpenStackServices()).thenReturn(applications);
        defaultRegionService.getDefaultRegions();
        verify(endpointService,times(3)).getBaseUrlsByServiceName(anyString());
    }

    @Test
    public void getDefaultRegions_callsEndpointService_onlyForDefaultRegions_flagIsNull() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();

        Application application1 = new Application();
        application1.setName("CloudFiles");
        application1.setUseForDefaultRegion(true);
        applications.add(application1);

        Application application2 = new Application();
        application2.setName("CloudServers");
        application2.setUseForDefaultRegion(true);
        applications.add(application2);

        Application application3 = new Application();
        application3.setName("CloudFilesCDN");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);

        Application application4 = new Application();
        application4.setName("CloudFilesCDN2");
        application4.setUseForDefaultRegion(null);
        applications.add(application4);

        when(applicationService.getOpenStackServices()).thenReturn(applications);
        defaultRegionService.getDefaultRegions();
        verify(endpointService,times(3)).getBaseUrlsByServiceName(anyString());
    }


    @Test
    public void getDefaultRegions_getsRegions() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();

        Application application1 = new Application();
        application1.setName("CloudFiles");
        application1.setUseForDefaultRegion(true);
        applications.add(application1);

        Application application2 = new Application();
        application2.setName("CloudServers");
        application2.setUseForDefaultRegion(true);
        applications.add(application2);

        Application application3 = new Application();
        application3.setName("CloudFilesCDN");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);

        when(applicationService.getOpenStackServices()).thenReturn(applications);

        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.setRegion("DFW");
        baseUrls.add(baseUrl);

        CloudBaseUrl baseUrl2 = new CloudBaseUrl();
        baseUrl2.setRegion("ORD");
        baseUrls.add(baseUrl2);

        ArrayList<CloudBaseUrl> baseUrls2 = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl baseUrl3 = new CloudBaseUrl();
        baseUrl3.setRegion("LON");
        baseUrls2.add(baseUrl3);

        when(endpointService.getBaseUrlsByServiceName("CloudFiles")).thenReturn(baseUrls);
        when(endpointService.getBaseUrlsByServiceName("CloudServers")).thenReturn(baseUrls2);
        when(endpointService.getBaseUrlsByServiceName("CloudFilesCDN")).thenReturn(baseUrls);

        Set<String> defaultRegions = defaultRegionService.getDefaultRegions();
        assertThat("default regions", defaultRegions.size(), equalTo(3));
        assertThat("default regions", defaultRegions, hasItem("DFW"));
        assertThat("default regions", defaultRegions, hasItem("ORD"));
        assertThat("default regions", defaultRegions, hasItem("LON"));
    }

    @Test
    public void getDefaultRegions_callsApplicationService_getOpenStackServices() throws Exception {
        defaultRegionService.getDefaultRegions();
        verify(applicationService).getOpenStackServices();
    }
}
