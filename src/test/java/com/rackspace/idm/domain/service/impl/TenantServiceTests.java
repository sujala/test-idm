package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.NotFoundException;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TenantServiceTests {

    private TenantDao mockTenantDao;
    private ApplicationDao mockClientDao;
    private UserDao mockUserDao;
    private ScopeAccessDao mockScopeAccessDao;
    private EndpointDao mockEndpointDao;
    private TenantService tenantService;

    private final String tenantName = "tenantName";
    private final String tenantId1 = "tenantId1";
    private final String tenantId2 = "tenantId2";
    private final String description = "description";
    private final String clientId = "clientId";
    private final String roleName = "Role";
    
    private final String clientName = "Client";
    
    private final String id = "XXX";
    private final String id2 = "YYY";

    @Before
    public void setUp() throws Exception {

        mockTenantDao = EasyMock.createMock(TenantDao.class);
        mockClientDao = EasyMock.createMock(ApplicationDao.class);
        mockUserDao = EasyMock.createMock(UserDao.class);
        mockEndpointDao = EasyMock.createMock(EndpointDao.class);
        mockScopeAccessDao = EasyMock.createMock(ScopeAccessDao.class);
        tenantService = new DefaultTenantService();
        tenantService.setTenantDao(mockTenantDao);
        tenantService.setClientDao(mockClientDao);
        tenantService.setUserDao(mockUserDao);
        tenantService.setEndpointDao(mockEndpointDao);
        tenantService.setScopeAccessDao(mockScopeAccessDao);
    }

    @Test
    public void shouldAddTenant() {
        Tenant tenant = getTestTenant();
        EasyMock.expect(mockTenantDao.getTenant(tenantName)).andReturn(null);
        mockTenantDao.addTenant(tenant);
        EasyMock.replay(mockTenantDao);
        tenantService.addTenant(tenant);
        EasyMock.verify(mockTenantDao);
    }

    @Test
    public void shouldDeleteTenant() {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(getTestSingleTenantRole());
        EasyMock.expect(mockTenantDao.getAllTenantRolesForTenant(tenantId1)).andReturn(roles);
        mockTenantDao.deleteTenantRole(getTestSingleTenantRole());
        mockTenantDao.deleteTenant(tenantId1);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenant(tenantId1);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldDeleteTenanWithMulitpleTenantRole() {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(getTestMultipleTenantRole());
        EasyMock.expect(mockTenantDao.getAllTenantRolesForTenant(tenantId1)).andReturn(roles);
        mockTenantDao.updateTenantRole(EasyMock.anyObject(TenantRole.class));
        mockTenantDao.deleteTenant(tenantId1);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenant(tenantId1);
        EasyMock.verify(mockTenantDao);
    }

    @Test
    public void shouldGetTenant() {
        Tenant tenant = getTestTenant();
        EasyMock.expect(mockTenantDao.getTenant(tenantId1)).andReturn(tenant);
        EasyMock.replay(mockTenantDao);
        Tenant returned = tenantService.getTenant(tenantId1);
        Assert.assertNotNull(returned);
        EasyMock.verify(mockTenantDao);
    }

    @Test
    public void shouldGetTenants() {
        Tenant tenant = getTestTenant();
        List<Tenant> tenants = new ArrayList<Tenant>();
        tenants.add(tenant);
        EasyMock.expect(mockTenantDao.getTenants()).andReturn(tenants);
        EasyMock.replay(mockTenantDao);
        List<Tenant> returned = tenantService.getTenants();
        Assert.assertNotNull(returned);
        Assert.assertTrue(returned.size() == 1);
        EasyMock.verify(mockTenantDao);
    }

    @Test
    public void shouldUpdateTenant() {
        Tenant tenant = getTestTenant();
        mockTenantDao.updateTenant(tenant);
        EasyMock.replay(mockTenantDao);
        tenantService.updateTenant(tenant);
        EasyMock.verify(mockTenantDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteNonExistentTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById("id",role.getRoleRsId())).andReturn(null);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRoleForUser(getUser(), role);
    }
    
    @Test
    public void shouldDeleteGlobalTenantRole() {
        TenantRole global = getTestGlobalTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById("id",global.getRoleRsId())).andReturn(global);
        mockTenantDao.deleteTenantRole(global);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRoleForUser(getUser(), global);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldDeleteSingleTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById("id",role.getRoleRsId())).andReturn(role);
        mockTenantDao.deleteTenantRole(role);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRoleForUser(getUser(), role);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldDeleteSingleTenantRoleFromMultipleTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        TenantRole role2 = getTestSingleTenantRole();
        role2.setTenantIds(new String[] {tenantId2});
        TenantRole combined = getTestMultipleTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById("id", id)).andReturn(combined);
        mockTenantDao.updateTenantRole(role2);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRoleForUser(getUser(), role);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldGetTenantRoleByRoleName() {
        TenantRole role = getTestSingleTenantRole();
        ClientRole cRole = getTestClientRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById("id", id)).andReturn(role);
        EasyMock.replay(mockTenantDao);
        EasyMock.expect(mockClientDao.getClientRoleById(id)).andReturn(cRole);
        EasyMock.replay(mockClientDao);
        TenantRole returned = tenantService.getTenantRoleForUserById(getUser(), id);
        Assert.assertNotNull(returned);
        EasyMock.verify(mockTenantDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldGetTenantRolesByParent() {
        TenantRole role = getTestSingleTenantRole();
        ClientRole cRole = getTestClientRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        User user = getUser();
        EasyMock.expect(mockTenantDao.getTenantRolesForUser(user)).andReturn(roles);
        EasyMock.replay(mockTenantDao);
        EasyMock.expect(mockClientDao.getClientRoleById(id)).andReturn(cRole);
        EasyMock.replay(mockClientDao);
        List<TenantRole> returned = tenantService.getTenantRolesForUser(user);
        Assert.assertTrue(returned.size() == 1);
        EasyMock.verify(mockTenantDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldGetTenantsForParentByTenantRoles() {
        Tenant tenant = getTestTenant();
        TenantRole role = getTestSingleTenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        EasyMock.expect(mockTenantDao.getTenantRolesByParent(null)).andReturn(roles);
        EasyMock.expect(mockTenantDao.getTenant(tenantId1)).andReturn(tenant);
        EasyMock.replay(mockTenantDao);
        List<Tenant> tenants = tenantService.getTenantsForScopeAccessByTenantRoles(getScopeAccess());
        Assert.assertTrue(tenants.size() == 1);
        EasyMock.verify(mockTenantDao);
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
        role.setRoleRsId(id);
        role.setClientId(clientId);
        role.setName(roleName);
        role.setTenantIds(new String[]{tenantId1});
        return role;
    }

    private TenantRole getTestMultipleTenantRole() {
        TenantRole role = new TenantRole();
        role.setRoleRsId(id);
        role.setClientId(clientId);
        role.setName(roleName);
        role.setTenantIds(new String[]{tenantId2, tenantId1});
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
