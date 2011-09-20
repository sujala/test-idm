package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.impl.DefaultTenantService;
import com.rackspace.idm.exception.NotFoundException;

public class TenantServiceTests {

    private TenantDao mockTenantDao;
    private ClientDao mockClientDao;
    private ScopeAccessDao mockScopeAccessDao;
    private TenantService tenantService;

    private final String tenantName = "tenantName";
    private final String tenantId1 = "tenantId1";
    private final String tenantId2 = "tenantId2";
    private final String description = "description";
    private final String clientId = "clientId";
    private final String roleName = "Role";

    @Before
    public void setUp() throws Exception {

        mockTenantDao = EasyMock.createMock(TenantDao.class);
        mockClientDao = EasyMock.createMock(ClientDao.class);
        mockScopeAccessDao = EasyMock.createMock(ScopeAccessDao.class);
        tenantService = new DefaultTenantService(mockTenantDao, mockClientDao, mockScopeAccessDao);
    }

    @Test
    public void shouldAddTenant() {
        Tenant tenant = getTestTenant();
        mockTenantDao.addTenant(tenant);
        EasyMock.replay(mockTenantDao);
        tenantService.addTenant(tenant);
        EasyMock.verify(mockTenantDao);
    }

    @Test
    public void shouldDeleteTenant() {
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

    @Test
    public void shouldAddTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(
            mockTenantDao.getTenantRoleForParentByRoleNameAndClientId(null,
                roleName, clientId)).andReturn(null);
        mockTenantDao.addTenantRoleToParent(null, role);
        EasyMock.replay(mockTenantDao);
        tenantService.addTenantRole(null, role);
        EasyMock.verify(mockTenantDao);
    }

    @Test
    public void shouldAddTenantToExistingRole() {
        TenantRole role = getTestSingleTenantRole();
        TenantRole role2 = getTestSingleTenantRole();
        TenantRole combined = getTestMultipleTenantRole();
        role2.setTenantIds(new String[]{tenantId2});
        EasyMock.expect(
            mockTenantDao.getTenantRoleForParentByRoleNameAndClientId(null,
                roleName, clientId)).andReturn(role2);
        mockTenantDao.updateTenantRole(EasyMock.eq(combined));
        EasyMock.replay(mockTenantDao);
        tenantService.addTenantRole(null, role);
        EasyMock.verify(mockTenantDao);
    }

    @Test
    public void shouldNotAddTenantRoleIfGlobalExists() {
        TenantRole role = getTestSingleTenantRole();
        TenantRole global = getTestGlobalTenantRole();
        EasyMock.expect(
            mockTenantDao.getTenantRoleForParentByRoleNameAndClientId(null,
                roleName, clientId)).andReturn(global);
        EasyMock.replay(mockTenantDao);
        tenantService.addTenantRole(null, role);
        EasyMock.verify(mockTenantDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteNonExistentTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(
            mockTenantDao.getTenantRoleForParentByRoleNameAndClientId(null,
                roleName, clientId)).andReturn(null);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRole(null, role);
    }
    
    @Test
    public void shouldDeleteGlobalTenantRole() {
        TenantRole global = getTestGlobalTenantRole();
        EasyMock.expect(
            mockTenantDao.getTenantRoleForParentByRoleNameAndClientId(null,
                roleName, clientId)).andReturn(global);
        mockTenantDao.deleteTenantRole(global);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRole(null, global);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldDeleteSingleTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(
            mockTenantDao.getTenantRoleForParentByRoleNameAndClientId(null,
                roleName, clientId)).andReturn(role);
        mockTenantDao.deleteTenantRole(role);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRole(null, role);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldDeleteSingleTenantRoleFromMultipleTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        TenantRole role2 = getTestSingleTenantRole();
        role2.setTenantIds(new String[] {tenantId2});
        TenantRole combined = getTestMultipleTenantRole();
        EasyMock.expect(
            mockTenantDao.getTenantRoleForParentByRoleNameAndClientId(null,
                roleName, clientId)).andReturn(combined);
        mockTenantDao.updateTenantRole(role2);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRole(null, role);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldGetTenantRoleByRoleName() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentByRoleName(null, roleName)).andReturn(role);
        EasyMock.replay(mockTenantDao);
        TenantRole returned = tenantService.getTenantRoleForParentByRoleName(null, roleName);
        Assert.assertNotNull(returned);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldGetTenantRoleByClientIdAndRoleName() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentByRoleNameAndClientId(null, roleName, clientId)).andReturn(role);
        EasyMock.replay(mockTenantDao);
        TenantRole returned = tenantService.getTenantRoleForParentByRoleNameAndClientId(null, roleName, clientId);
        Assert.assertNotNull(returned);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldGetTenantRolesByParent() {
        TenantRole role = getTestSingleTenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        EasyMock.expect(mockTenantDao.getTenantRolesByParent(null)).andReturn(roles);
        EasyMock.replay(mockTenantDao);
        List<TenantRole> returned = tenantService.getTenantRolesByParent(null);
        Assert.assertTrue(returned.size() == 1);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldGetTenantRolesByParentAndClientId() {
        TenantRole role = getTestSingleTenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        EasyMock.expect(mockTenantDao.getTenantRolesByParentAndClientId(null, clientId)).andReturn(roles);
        EasyMock.replay(mockTenantDao);
        List<TenantRole> returned = tenantService.getTenantRolesByParentAndClientId(null, clientId);
        Assert.assertTrue(returned.size() == 1);
        EasyMock.verify(mockTenantDao);
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
        List<Tenant> tenants = tenantService.getTenantsForParentByTenantRoles(null);
        Assert.assertTrue(tenants.size() == 1);
        EasyMock.verify(mockTenantDao);
    }

    private Tenant getTestTenant() {
        Tenant tenant = new Tenant();
        tenant.setDescription(description);
        tenant.setEnabled(true);
        tenant.setName(tenantName);
        tenant.setTenantId(tenantId1);
        return tenant;
    }

    private TenantRole getTestGlobalTenantRole() {
        TenantRole role = new TenantRole();
        role.setClientId(clientId);
        role.setName(roleName);
        role.setTenantIds(null);
        return role;
    }

    private TenantRole getTestSingleTenantRole() {
        TenantRole role = new TenantRole();
        role.setClientId(clientId);
        role.setName(roleName);
        role.setTenantIds(new String[]{tenantId1});
        return role;
    }

    private TenantRole getTestMultipleTenantRole() {
        TenantRole role = new TenantRole();
        role.setClientId(clientId);
        role.setName(roleName);
        role.setTenantIds(new String[]{tenantId1, tenantId2});
        return role;
    }
}
