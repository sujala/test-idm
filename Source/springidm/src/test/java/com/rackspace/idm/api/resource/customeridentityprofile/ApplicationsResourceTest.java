package com.rackspace.idm.api.resource.customeridentityprofile;

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/18/12
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationsResourceTest {
    ApplicationConverter applicationConverter;
    AuthorizationService authorizationService;
    ApplicationService applicationService;
    ApplicationsResource applicationsResource;

    @Before
    public void setUp() throws Exception {
        applicationConverter = mock(ApplicationConverter.class);
        applicationService = mock(ApplicationService.class);
        authorizationService = mock(AuthorizationService.class);
        applicationsResource = new ApplicationsResource(null, applicationConverter, authorizationService, applicationService);
    }

    @Test
    public void getApplications_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        applicationsResource.getApplications(null, null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void getApplications_callsApplicationService_getByCustomerId_withNullLimitAndOffset() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        applicationsResource.getApplications(null, "customerId", null, null);
        verify(applicationService).getByCustomerId("customerId", -1, -1);
    }

    @Test
    public void getApplications_callsApplicationService_getByCustomerId_withLimitAndOffset() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        applicationsResource.getApplications(null, "customerId", 10, 10);
        verify(applicationService).getByCustomerId("customerId", 10, 10);
    }

    @Test
    public void getApplications_returns200Status() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        Response response = applicationsResource.getApplications(null, null, null, null);
        assertThat("response status", response.getStatus(), equalTo(200));
    }
}
