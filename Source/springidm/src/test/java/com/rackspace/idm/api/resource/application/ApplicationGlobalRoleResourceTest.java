package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/9/12
 * Time: 4:27 PM
 */
public class ApplicationGlobalRoleResourceTest {

    ApplicationGlobalRoleResource applicationGlobalRoleResource;
    TenantService tenantService;
    AuthorizationService authorizationService;
    ApplicationService applicationService;

    @Before
    public void setUp() throws Exception {
        authorizationService = mock(AuthorizationService.class);
        tenantService = mock(TenantService.class);
        applicationService = mock(ApplicationService.class);
        applicationGlobalRoleResource =  new ApplicationGlobalRoleResource(authorizationService,applicationService,tenantService);
    }

    @Test
    public void grantGlobalRoleToApplication_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        applicationGlobalRoleResource.grantGlobalRoleToApplication(null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void grantGlobalRoleToApplication_callsTenantService_addTenantRoleToClient() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        applicationGlobalRoleResource.grantGlobalRoleToApplication(null, null, null);
        verify(tenantService).addTenantRoleToClient(any(Application.class), any(TenantRole.class));
    }

    @Test
    public void grantGlobalRoleToApplication_returnsNoContentResponse() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        Response response = applicationGlobalRoleResource.grantGlobalRoleToApplication(null, null, null);
        assertThat("response status", response.getStatus(), equalTo(204));
    }


    @Test(expected = NotFoundException.class)
    public void deleteGlobalRoleFromUser_throwsNotFoundExceptionWhenRoleIsNotFound() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(new Application());
        when(tenantService.getTenantRoleForParentById(anyString(), anyString())).thenThrow(new NotFoundException());
        applicationGlobalRoleResource.deleteGlobalRoleFromApplication(null, null, null);
    }

    @Test(expected = NotFoundException.class)
    public void deleteGlobalRoleFromApplication_throwsBadRequestExceptionWhenApplicationIsNotFound() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenThrow(new NotFoundException());
        when(tenantService.getTenantRoleForParentById(anyString(), anyString())).thenReturn(null);
        applicationGlobalRoleResource.deleteGlobalRoleFromApplication(null, null, null);
    }

    @Test
    public void deleteGlobalRoleFromApplication_callsTenantService_deleteTenantRole() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(new Application());
        when(tenantService.getTenantRoleForParentById(anyString(), anyString())).thenReturn(new TenantRole());
        applicationGlobalRoleResource.deleteGlobalRoleFromApplication(null, null, null);
        verify(tenantService).deleteTenantRole(anyString(), any(TenantRole.class));
    }
    @Test
    public void deleteGlobalRoleFromApplication_returnsNoContentResponse() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(new Application());
        when(tenantService.getTenantRoleForParentById(anyString(), anyString())).thenReturn(new TenantRole());
        Response response = applicationGlobalRoleResource.deleteGlobalRoleFromApplication(null, null, null);
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test
    public void grantTenantRoleToApplication_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        applicationGlobalRoleResource.grantTenantRoleToApplication(null, null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void grantTenantRoleToApplication_callsTenantService_addTenantRoleToClient() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        applicationGlobalRoleResource.grantTenantRoleToApplication(null, null, null, null);
        verify(tenantService).addTenantRoleToClient(any(Application.class), any(TenantRole.class));
    }

    @Test
    public void grantTenantRoleToApplication_returnsNoContentResponse() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        Response response = applicationGlobalRoleResource.grantTenantRoleToApplication(null, null, null, null);
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test
    public void deleteTenantRoleFromApplication_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        when(applicationService.loadApplication(anyString())).thenReturn(new Application());
        applicationGlobalRoleResource.deleteTenantRoleFromApplication(null, null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(null);
    }

    @Test
    public void deleteTenantRoleFromApplication_callsTenantService_deleteTenantRole() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.loadApplication(anyString())).thenReturn(new Application());
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        applicationGlobalRoleResource.deleteTenantRoleFromApplication(null, null, null, null);
        verify(tenantService).deleteTenantRole(anyString(), any(TenantRole.class));
    }

    @Test
    public void deleteTenantRoleFromApplication_returnsNoContentResponse() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.loadApplication(anyString())).thenReturn(new Application());
        when(applicationService.getClientRoleById(anyString())).thenReturn(new ClientRole());
        Response response = applicationGlobalRoleResource.deleteTenantRoleFromApplication(null, null, null, null);
        assertThat("Response status", response.getStatus(), equalTo(204));
    }

    @Test (expected = BadRequestException.class)
    public void createTenantRole_roleIsNull_throwsBadRequest() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(null);
        when(applicationService.loadApplication(anyString())).thenReturn(new Application());
        when(applicationService.getClientRoleById(anyString())).thenReturn(null);
        applicationGlobalRoleResource.deleteTenantRoleFromApplication(null, null, null, null);
    }

}
