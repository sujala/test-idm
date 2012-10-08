package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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
    EndpointService endpointService = mock(EndpointService.class);
    ApplicationService applicationService = mock(ApplicationService.class);

    @Before
    public void setUp() throws Exception {
        defaultRegionService.setEndpointService(endpointService);
        defaultRegionService.setApplicationService(applicationService);
    }

    @Test
    public void addUser_callerIsServiceAdmin_defaultRegionDoesNotMatchUserRegion_returnsCorrectMessage() throws Exception {
        HashSet<String> defaultRegions = new HashSet<String>();
        defaultRegions.add("DFW");
        DefaultRegionService spy = spy(defaultRegionService);
        doReturn(defaultRegions).when(spy).getDefaultRegions();
        try {
            spy.validateDefaultRegion("ORD");
        } catch (Exception e) {
            assertThat("exception", e.getMessage(), equalTo("Invalid defaultRegion value, accepted values are: DFW."));
        }
    }

    @Test(expected = BadRequestException.class)
    public void addUser_callerIsServiceAdmin_defaultRegionDoesNotMatchUserRegion_ThrowsBadRequestException() throws Exception {
        HashSet<String> defaultRegions = new HashSet<String>();
        defaultRegions.add("DFW");
        DefaultRegionService spy = spy(defaultRegionService);
        doReturn(defaultRegions).when(spy).getDefaultRegions();
        spy.validateDefaultRegion("ORD");
    }

    @Test
    public void validateDefaultRegion_nullRegion_NoExceptionThrown() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegions();
        spy.validateDefaultRegion(null);
    }

    @Test
    public void validateDefaultRegion_matchingRegion_NoExceptionThrown() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegions();
        spy.validateDefaultRegion("DFW");
    }

    @Test
    public void validateDefaultRegion_callsGetDefaultRegions() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegions();
        spy.validateDefaultRegion("DFW");
        verify(spy).getDefaultRegions();
    }

    @Test(expected = BadRequestException.class)
    public void validateDefaultRegion_nonMatchingRegion_BadRequestExceptionThrown() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegions();
        spy.validateDefaultRegion("LON");
    }


    @Test(expected = BadRequestException.class)
    public void validateDefaultRegion_regionIsEmptyString_throwBadRequestException() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegions();
        spy.validateDefaultRegion("");
    }

    @Test(expected = BadRequestException.class)
    public void validateDefaultRegion_regionIsBlankString_throwBadRequestException() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegions();
        spy.validateDefaultRegion("   ");
    }

    @Test (expected = BadRequestException.class)
    public void validateDefaultRegionByScopeAccess_invalidRegion_throwsBadRequest() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        ScopeAccess sa = new ScopeAccess();
        Set<String> regions = new HashSet<String>();
        regions.add("ORD-TEST");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegionsForUser(sa);

        spy.validateDefaultRegion("DFW", sa);
    }

    @Test (expected = BadRequestException.class)
    public void validateDefaultRegionByScopeAccess_emptyString_throwsBadRequest() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        ScopeAccess sa = new ScopeAccess();
        Set<String> regions = new HashSet<String>();
        regions.add("ORD-TEST");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegionsForUser(sa);

        spy.validateDefaultRegion("", sa);
    }

    @Test (expected = BadRequestException.class)
    public void validateDefaultRegionByScopeAccess_spaceString_throwsBadRequest() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        ScopeAccess sa = new ScopeAccess();
        Set<String> regions = new HashSet<String>();
        regions.add("ORD-TEST");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegionsForUser(sa);

        spy.validateDefaultRegion(" ", sa);
    }

    @Test (expected = BadRequestException.class)
    public void checkDefaultRegion_nonMatchingRegion_throwsBadRequest() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("ORD");
        regions.add("ORD-TEST");

        spy.checkDefaultRegion("DFW", regions);
    }

    @Test
    public void checkDefaultRegion_matchingRegion_noException() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("ORD");
        regions.add("ORD-TEST");

        spy.checkDefaultRegion("ORD", regions);
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
        verify(endpointService, times(3)).getBaseUrlsByServiceName(anyString());
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
        verify(endpointService, times(3)).getBaseUrlsByServiceName(anyString());
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
        verify(endpointService, times(3)).getBaseUrlsByServiceName(anyString());
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
