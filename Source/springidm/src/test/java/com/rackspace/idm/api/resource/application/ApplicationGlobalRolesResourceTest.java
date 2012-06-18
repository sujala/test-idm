package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.api.converter.RolesConverter;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.TenantService;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/18/12
 * Time: 10:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationGlobalRolesResourceTest {
    ApplicationGlobalRolesResource applicationGlobalRolesResource;
    private TenantService tenantService;
    private AuthorizationService authorizationService;
    private ApplicationService clientService;
    private ApplicationGlobalRoleResource roleResource;
    private RolesConverter rolesConverter;

    @Before
    public void setUp() throws Exception {
        tenantService = mock(TenantService.class);
        authorizationService = mock(AuthorizationService.class);
        clientService = mock(ApplicationService.class);
        applicationGlobalRolesResource = mock(ApplicationGlobalRolesResource.class);
        roleResource = mock(ApplicationGlobalRoleResource.class);
        rolesConverter = mock(RolesConverter.class);
        applicationGlobalRolesResource = new ApplicationGlobalRolesResource(tenantService, authorizationService, clientService, null, roleResource, rolesConverter);
    }

    @Test
    public void getRoles_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        applicationGlobalRolesResource.getRoles(null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());

    }
}
