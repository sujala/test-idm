package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        applicationGlobalRoleResource =  new ApplicationGlobalRoleResource(authorizationService,applicationService,null,tenantService);
    }

    @Test(expected = NotFoundException.class)
    public void deleteGlobalRoleFromUser_throwsNotFoundExceptionWhenRoleIsNotFound() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(new Application());
        when(tenantService.getTenantRoleForParentById(anyString(), anyString())).thenReturn(null);
        applicationGlobalRoleResource.deleteGlobalRoleFromApplication(null, null, null);
    }

    @Test(expected = BadRequestException.class)
    public void deleteGlobalRoleFromUser_throwsBadRequestExceptionWhenApplicationIsNotFound() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(null);
        when(tenantService.getTenantRoleForParentById(anyString(), anyString())).thenReturn(null);
        applicationGlobalRoleResource.deleteGlobalRoleFromApplication(null, null, null);
    }
}
