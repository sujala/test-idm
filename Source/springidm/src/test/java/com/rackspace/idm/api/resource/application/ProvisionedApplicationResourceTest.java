package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/18/12
 * Time: 4:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProvisionedApplicationResourceTest {

    ScopeAccessService scopeAccessService;
    ApplicationService applicationService;
    AuthorizationService authorizationService;
    ProvisionedApplicationResource provisionedApplicationResource;

    @Before
    public void setUp() throws Exception {
        scopeAccessService = mock(ScopeAccessService.class);
        applicationService = mock(ApplicationService.class);
        authorizationService = mock(AuthorizationService.class);
        provisionedApplicationResource = new ProvisionedApplicationResource(scopeAccessService, applicationService, authorizationService, null);
    }

    @Test
    public void provisionApplicationForApplication_callsAuthorizedService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.loadApplication(anyString())).thenReturn(new Application("applicationId", ClientSecret.newInstance("clientSecret"), "name", "rcn=name", ClientStatus.ACTIVE));
        provisionedApplicationResource.provisionApplicationForApplication(null, null, "provision");
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void provisionApplicationForApplication_callsApplicationService_loadApplication_withApplicationId() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(new Application("applicationId", ClientSecret.newInstance("clientSecret"), "name", "rcn=name", ClientStatus.ACTIVE));
        provisionedApplicationResource.provisionApplicationForApplication(null, "applicationId", null);
        verify(applicationService).loadApplication("applicationId");
    }

    @Test
    public void provisionApplicationForApplication_callsApplicationService_loadApplication_withProvisionedApplicationId() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(new Application("applicationId", ClientSecret.newInstance("clientSecret"), "name", "rcn=name", ClientStatus.ACTIVE));
        provisionedApplicationResource.provisionApplicationForApplication(null, "applicationId", "provisionedApplicationId");
        verify(applicationService).loadApplication("provisionedApplicationId");
    }

    @Test
    public void provisionApplicationForApplication_callsScopeAccessService_addDirectScopeAccess_withApplicationId() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        Application application = new Application("applicationId", ClientSecret.newInstance("clientSecret"), "name", "rcn=name", ClientStatus.ACTIVE);
        application.setUniqueId("applicationUniqueId");
        when(applicationService.loadApplication(anyString())).thenReturn(application);
        provisionedApplicationResource.provisionApplicationForApplication(null, null, null);
        verify(scopeAccessService).addDirectScopeAccess(eq("applicationUniqueId"), any(ScopeAccess.class));
    }

    @Test
    public void provisionApplicationForApplication_returnsNoContentResponse() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(new Application("applicationId", ClientSecret.newInstance("clientSecret"), "name", "rcn=name", ClientStatus.ACTIVE));
        Response response = provisionedApplicationResource.provisionApplicationForApplication(null, null, null);
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test
    public void removeApplicationFromUser_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.loadApplication(anyString())).thenReturn(new Application("applicationId", ClientSecret.newInstance("clientSecret"), "name", "rcn=name", ClientStatus.ACTIVE));
        provisionedApplicationResource.removeApplicationFromUser(null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void removeApplicationFromUser_callsScopeAccessService_deleteScopeAccessesForParentByApplicationId() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        Application application = new Application("applicationId", ClientSecret.newInstance("clientSecret"), "name", "rcn=name", ClientStatus.ACTIVE);
        application.setUniqueId("applicationUniqueId");
        when(applicationService.loadApplication(anyString())).thenReturn(application);
        provisionedApplicationResource.removeApplicationFromUser(null, null, null);
        verify(scopeAccessService).deleteScopeAccessesForParentByApplicationId("applicationUniqueId", "applicationId");
    }

    @Test
    public void removeApplicationFromUser_returnsNoContentResponse() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(new Application("applicationId", ClientSecret.newInstance("clientSecret"), "name", "rcn=name", ClientStatus.ACTIVE));
        Response response = provisionedApplicationResource.removeApplicationFromUser(null, null, null);
        assertThat("response status", response.getStatus(), equalTo(204));
    }
}
