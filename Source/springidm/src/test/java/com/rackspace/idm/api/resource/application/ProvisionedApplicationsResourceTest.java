package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
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
 * Time: 5:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProvisionedApplicationsResourceTest {

    ProvisionedApplicationResource provisionedApplicationResource;
    ApplicationConverter applicationConverter;
    ApplicationService applicationService;
    AuthorizationService authorizationService;
    ProvisionedApplicationsResource provisionedApplicationsResource;

    @Before
    public void setUp() throws Exception {
        provisionedApplicationResource = mock(ProvisionedApplicationResource.class);
        applicationConverter = mock(ApplicationConverter.class);
        applicationService = mock(ApplicationService.class);
        authorizationService = mock(AuthorizationService.class);
        provisionedApplicationsResource = new ProvisionedApplicationsResource(applicationConverter, applicationService, authorizationService, provisionedApplicationResource, null);
    }

    @Test
    public void getApplicationsForApplication_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        provisionedApplicationsResource.getApplicationsForApplication(null,null);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void getApplicationsForApplication_returns200Status() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        Response response = provisionedApplicationsResource.getApplicationsForApplication(null, null);
        assertThat("response status", response.getStatus(), equalTo(200));
    }

    @Test
    public void getProvisionedApplicationResource_returnsProvisionedApplicationResource() throws Exception {
        ProvisionedApplicationResource provisionedApplicationResource1 = provisionedApplicationsResource.getProvisionedApplicationResource();
        assertThat("provisioned application resource", provisionedApplicationResource1, equalTo(provisionedApplicationResource));
    }
}
