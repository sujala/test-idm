package com.rackspace.idm.api.resource.cloud.v20;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Test;

import java.util.HashSet;
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
@RunWith(MockitoJUnitRunner.class)
public class DefaultRegionServiceTestOLD {

    @InjectMocks
    DefaultRegionService defaultRegionService = new DefaultRegionService();
    @Mock
    EndpointService endpointService;
    @Mock
    ApplicationService applicationService;

    @Test
    public void addUser_callerIsServiceAdmin_defaultRegionDoesNotMatchUserRegion_returnsCorrectMessage() throws Exception {
        HashSet<String> defaultRegions = new HashSet<String>();
        defaultRegions.add("DFW");
        DefaultRegionService spy = spy(defaultRegionService);
        doReturn(defaultRegions).when(spy).getDefaultRegionsForCloudServersOpenStack();
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
        doReturn(defaultRegions).when(spy).getDefaultRegionsForCloudServersOpenStack();
        spy.validateDefaultRegion("ORD");
    }

    @Test
    public void validateDefaultRegion_nullRegion_NoExceptionThrown() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegionsForCloudServersOpenStack();
        spy.validateDefaultRegion(null);
    }

    @Test
    public void validateDefaultRegion_matchingRegion_NoExceptionThrown() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegionsForCloudServersOpenStack();
        spy.validateDefaultRegion("DFW");
    }

    @Test
    public void validateDefaultRegion_callsGetDefaultRegions() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegionsForCloudServersOpenStack();
        spy.validateDefaultRegion("DFW");
        verify(spy).getDefaultRegionsForCloudServersOpenStack();
    }

    @Test(expected = BadRequestException.class)
    public void validateDefaultRegion_nonMatchingRegion_BadRequestExceptionThrown() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegionsForCloudServersOpenStack();
        spy.validateDefaultRegion("LON");
    }


    @Test(expected = BadRequestException.class)
    public void validateDefaultRegion_regionIsEmptyString_throwBadRequestException() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegionsForCloudServersOpenStack();
        spy.validateDefaultRegion("");
    }

    @Test(expected = BadRequestException.class)
    public void validateDefaultRegion_regionIsBlankString_throwBadRequestException() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("DFW");
        regions.add("ORD");
        doReturn(regions).when(spy).getDefaultRegionsForCloudServersOpenStack();
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

    @Test (expected = BadRequestException.class)
    public void checkDefaultRegion_nonMatchingRegion_withNoCSOSEndpoint_throwsBadReqeust() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();

        spy.checkDefaultRegion("ORD", regions);
    }

    @Test
    public void checkDefaultRegion_matchingRegion_noException() throws Exception {
        DefaultRegionService spy = spy(defaultRegionService);
        Set<String> regions = new HashSet<String>();
        regions.add("ORD");
        regions.add("ORD-TEST");

        spy.checkDefaultRegion("ORD", regions);
    }
}
