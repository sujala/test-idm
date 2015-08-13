package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.IdentityConfig.ReloadableConfig;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class TenantServiceTests {

    private TenantDao tenantDao;
    private TenantRoleDao tenantRoleDao;
    private ApplicationService applicationService;
    private UserService userService;
    private ScopeAccessService scopeAccessService;
    private EndpointService endpointService;
    private TenantService tenantService;
    private IdentityConfig identityConfig;
    private ReloadableConfig reloadableConfig;

    private final String tenantName = "tenantName";
    private final String tenantId1 = "tenantId1";
    private final String tenantId2 = "tenantId2";
    private final String description = "description";
    private final String clientId = "clientId";
    private final String roleName = "Role";
    
    private final String clientName = "Client";
    
    private final String id = "XXX";
    private final String id2 = "YYY";

    @Autowired
    IdentityConfig identityConfig;

    @Before
    public void setUp() throws Exception {

        tenantDao = EasyMock.createMock(TenantDao.class);
        applicationService = EasyMock.createMock(ApplicationService.class);
        userService = EasyMock.createMock(UserService.class);
        endpointService = EasyMock.createMock(EndpointService.class);
        scopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        identityConfig = EasyMock.createMock(IdentityConfig.class)
        reloadableConfig = EasyMock.createMock(IdentityConfig.ReloadableConfig.class)

        tenantService = new DefaultTenantService();
        tenantService.tenantDao = tenantDao;
        tenantService.tenantRoleDao = tenantRoleDao;
        tenantService.userService = userService;
        tenantService.applicationService = applicationService;
        tenantService.endpointService = endpointService;
        tenantService.scopeAccessService = scopeAccessService;
        tenantService.identityConfig = identityConfig
    }

    @Test
    public void shouldAddTenant() {
        Tenant tenant = getTestTenant();
        EasyMock.expect(tenantDao.getTenant(tenantName)).andReturn(null);
        tenantDao.addTenant(tenant);
        EasyMock.replay(tenantDao);
        tenantService.addTenant(tenant);
        EasyMock.verify(tenantDao);
    }

    @Test
    public void shouldGetTenant() {
        Tenant tenant = getTestTenant();
        EasyMock.expect(tenantDao.getTenant(tenantId1)).andReturn(tenant);
        EasyMock.replay(tenantDao);
        Tenant returned = tenantService.getTenant(tenantId1);
        Assert.assertNotNull(returned);
        EasyMock.verify(tenantDao);
    }

    @Test
    public void shouldGetTenants() {
        Tenant tenant = getTestTenant();
        List<Tenant> tenants = new ArrayList<Tenant>();
        tenants.add(tenant);
        EasyMock.expect(tenantDao.getTenants()).andReturn(tenants);
        EasyMock.replay(tenantDao);
        List<Tenant> returned = tenantService.getTenants();
        Assert.assertNotNull(returned);
        Assert.assertTrue(returned.size() == 1);
        EasyMock.verify(tenantDao);
    }

    @Test
    public void shouldUpdateTenant() {
        Tenant tenant = getTestTenant();
        EasyMock.expect(identityConfig.getReloadableConfig()).andReturn(reloadableConfig);
        EasyMock.expect(reloadableConfig.getIdentityRoleDefaultTenant()).andReturn('identity');
        EasyMock.replay(identityConfig)
        EasyMock.replay(reloadableConfig)
        tenantDao.updateTenant(tenant);
        EasyMock.replay(tenantDao);
        tenantService.updateTenant(tenant);
        EasyMock.verify(tenantDao);
    }

    private Tenant getTestTenant() {
        Tenant tenant = new Tenant();
        tenant.setDescription(description);
        tenant.setEnabled(true);
        tenant.setName(tenantName);
        tenant.setTenantId(tenantName);
        return tenant;
    }

    private TenantRole getTestGlobalTenantRole() {
        TenantRole role = new TenantRole();
        role.setRoleRsId(id2);
        role.setClientId(clientId);
        role.setName(roleName);
        role.setTenantIds(null);
        return role;
    }

    private TenantRole getTestSingleTenantRole() {
        TenantRole role = new TenantRole();
        String[] array = new String[1];
        array[0] = tenantId1;
        role.setRoleRsId(id);
        role.setClientId(clientId);
        role.setName(roleName);
        role.setTenantIds(array);
        return role;
    }

    private TenantRole getTestMultipleTenantRole() {
        TenantRole role = new TenantRole();
        String[] array = new String[2];
        array[0] = tenantId1;
        array[1] = tenantId2;
        role.setRoleRsId(id);
        role.setClientId(clientId);
        role.setName(roleName);
        role.setTenantIds(array);
        return role;
    }
    
    private ClientRole getTestClientRole() {
        ClientRole role = new ClientRole();
        role.setId(id);
        role.setClientId(clientId);
        role.setName(clientName);
        role.setDescription(description);
        return role;
    }

    private ScopeAccess getScopeAccess() {
        ScopeAccess scopeAccess = new ScopeAccess();
        return scopeAccess;
    }

    public User getUser() {
        User user = new User();
        user.setUniqueId("id");
        return user;
    }
}
