package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.TenantService;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/18/12
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationTenantsResourceTest {

     ApplicationService applicationService;
     AuthorizationService authorizationService;
     RolesConverter rolesConverter;
     TenantService tenantService;
     ApplicationTenantsResource applicationTenantsResource;

    @Before
    public void setUp() throws Exception {
        applicationService = mock(ApplicationService.class);
        authorizationService = mock(AuthorizationService.class);
        rolesConverter = mock(RolesConverter.class);
        tenantService = mock(TenantService.class);

        applicationTenantsResource = new ApplicationTenantsResource(applicationService, authorizationService, rolesConverter, tenantService, null);
    }

    @Test
    public void getAllTenantRolesForApplication_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        applicationTenantsResource.getAllTenantRolesForApplication(null, null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void getAllTenantRolesForApplication_callsTenantService_getTenantRolesForApplication() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        applicationTenantsResource.getAllTenantRolesForApplication(null, null, null, null);
        verify(tenantService).getTenantRolesForApplication(any(Application.class), any(FilterParam[].class));
    }

    @Test
    public void getAllTenantRolesForApplication_returns200Status() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        Response response = applicationTenantsResource.getAllTenantRolesForApplication(null, null, null, null);
        assertThat("response status", response.getStatus(), equalTo(200));
    }
}
