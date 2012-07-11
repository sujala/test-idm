package com.rackspace.idm.api.resource.application;

import com.rackspace.api.idm.v1.Application;
import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.IdmException;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/9/12
 * Time: 5:10 PM
 */
public class ApplicationResourceTest {

    ApplicationResource applicationResource;
    ApplicationService applicationService;
    ApplicationConverter applicationConverter;
    AuthorizationService authorizationService;
    ProvisionedApplicationsResource customerClientServicesResource;
    ApplicationTenantsResource applicationTenantsResource;
    ApplicationGlobalRolesResource applicationGlobalRolesResource;

    @Before
    public void setUp() throws Exception {
        applicationService = mock(ApplicationService.class);
        applicationConverter = mock(ApplicationConverter.class);
        authorizationService = mock(AuthorizationService.class);
        customerClientServicesResource = mock(ProvisionedApplicationsResource.class);
        applicationTenantsResource = mock(ApplicationTenantsResource.class);
        applicationGlobalRolesResource = mock(ApplicationGlobalRolesResource.class);
        applicationResource = new ApplicationResource(applicationTenantsResource,applicationGlobalRolesResource,customerClientServicesResource,applicationService,null,applicationConverter,authorizationService,null);
    }

    @Test
    public void getApplication_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        applicationResource.getApplication(null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void getApplication_callsApplicationService_loadApplication() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        applicationResource.getApplication(null, "applicationId");
        verify(applicationService).loadApplication("applicationId");
    }

    @Test
    public void getApplication_callsApplicationConverter_toClientJaxbWithoutPermissionsOrCredentials() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        applicationResource.getApplication(null, "applicationId");
        verify(applicationConverter).toClientJaxbWithoutPermissionsOrCredentials(any(com.rackspace.idm.domain.entity.Application.class));
    }

    @Test
    public void updateApplication_callsAuthorizeService_verifyIdmSuperAdminAccess() throws Exception {
        EntityHolder<Application> application = mock(EntityHolder.class);
        when(application.hasEntity()).thenReturn(true);
        com.rackspace.idm.domain.entity.Application applicationDo = mock(com.rackspace.idm.domain.entity.Application.class);
        when(applicationService.loadApplication(anyString())).thenReturn(applicationDo);
        applicationResource.updateApplication(null, null, application);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test(expected = BadRequestException.class)
    public void updateApplication_withNonMatchingApplicationIdInPayload_throwsBadRequestException() throws Exception {
        com.rackspace.idm.domain.entity.Application application = new com.rackspace.idm.domain.entity.Application("clientId", null, null, null, null);
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationConverter.toClientDO(Matchers.<Application>any())).thenReturn(application);
        when(applicationService.loadApplication(null)).thenReturn(null);
        Application application1 = new Application();
        application1.setClientId("foo");
        applicationResource.updateApplication(null,"bar",new EntityHolder<Application>(application1));
    }

    @Test
    public void updateApplication_copiesChangesToDomainObject() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        EntityHolder<Application> application = mock(EntityHolder.class);
        when(application.hasEntity()).thenReturn(true);
        com.rackspace.idm.domain.entity.Application applicationDo = mock(com.rackspace.idm.domain.entity.Application.class);
        when(applicationService.loadApplication(anyString())).thenReturn(applicationDo);
        applicationResource.updateApplication(null, null, application);
        verify(applicationDo).copyChanges(null);
    }

    @Test
    public void updateApplication_callsApplicationService_updateClient() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        EntityHolder<Application> application = mock(EntityHolder.class);
        when(application.hasEntity()).thenReturn(true);
        com.rackspace.idm.domain.entity.Application applicationDo = mock(com.rackspace.idm.domain.entity.Application.class);
        when(applicationService.loadApplication(anyString())).thenReturn(applicationDo);
        applicationResource.updateApplication(null, null, application);
        verify(applicationService).updateClient(applicationDo);
    }

    @Test
    public void updateApplication_callsApplicationConverter_toClientJaxbWithoutPermissionsOrCredentials() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        EntityHolder<Application> application = mock(EntityHolder.class);
        when(application.hasEntity()).thenReturn(true);
        com.rackspace.idm.domain.entity.Application applicationDo = mock(com.rackspace.idm.domain.entity.Application.class);
        when(applicationService.loadApplication(anyString())).thenReturn(applicationDo);
        applicationResource.updateApplication(null, null, application);
        verify(applicationConverter).toClientJaxbWithoutPermissionsOrCredentials(applicationDo);
    }

    @Test
    public void deleteApplication_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        applicationResource.deleteApplication(null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void deleteApplication_callsApplicationService_deleteApplication() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        applicationResource.deleteApplication(null, "applicationId");
        verify(applicationService).delete("applicationId");
    }

    @Test
    public void deleteApplication_returnsNoContentResponse() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        Response response = applicationResource.deleteApplication(null, "applicationId");
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test
    public void resetApplicationSecretCredential_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.resetClientSecret(any(com.rackspace.idm.domain.entity.Application.class))).thenReturn(ClientSecret.newInstance("clientSecret"));
        applicationResource.resetApplicationSecretCredential(null, "applicationId");
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test(expected = IdmException.class)
    public void resetApplicationSecretCredential_whenIllegalStateExceptionThrown_throwsIdmException() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.resetClientSecret(any(com.rackspace.idm.domain.entity.Application.class))).thenThrow(new IllegalStateException());
        applicationResource.resetApplicationSecretCredential(null, "applicationId");
    }

    @Test
    public void resetApplicationSecretCredential_callsApplicationService_resetClientSecret() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.resetClientSecret(any(com.rackspace.idm.domain.entity.Application.class))).thenReturn(ClientSecret.newInstance("clientSecret"));
        applicationResource.resetApplicationSecretCredential(null, "applicationId");
        verify(applicationService).resetClientSecret(any(com.rackspace.idm.domain.entity.Application.class));
    }

    @Test
    public void resetApplicationSecretCredential_returns200Status() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.resetClientSecret(any(com.rackspace.idm.domain.entity.Application.class))).thenReturn(ClientSecret.newInstance("clientSecret"));
        Response response = applicationResource.resetApplicationSecretCredential(null, "applicationId");
        assertThat("response status", response.getStatus(), equalTo(200));
    }

    @Test
    public void getProvisionedApplicationsResource_returnsProvisionedApplicationsResource() throws Exception {
        ProvisionedApplicationsResource provisionedApplicationsResource = applicationResource.getProvisionedApplicationsResource();
        assertThat("provisioned applications resource", provisionedApplicationsResource, equalTo(customerClientServicesResource));
    }

    @Test
    public void getTenantsResource_returnsTenantsResource() throws Exception {
        ApplicationTenantsResource tenantsResource = applicationResource.getTenantsResource();
        assertThat("tenants resource", tenantsResource, equalTo(applicationTenantsResource));
    }

    @Test
    public void getGlobalRolesResource_returnsGlobalRolesResource() throws Exception {
        ApplicationGlobalRolesResource globalRolesResource = applicationResource.getGlobalRolesResource();
        assertThat("global roles resource", globalRolesResource, equalTo(applicationGlobalRolesResource));
    }
}
