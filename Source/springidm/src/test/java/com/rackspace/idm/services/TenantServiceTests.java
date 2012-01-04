package com.rackspace.idm.services;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.impl.DefaultTenantService;
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
        mockScopeAccessDao = EasyMock.createMock(ScopeAccessDao.class);
        tenantService = new DefaultTenantService(mockTenantDao, mockClientDao, mockUserDao, mockScopeAccessDao);
    }

    @Test
    public void shouldAddTenant() {
        Tenant tenant = getTestTenant();
        EasyMock.expect(mockTenantDao.getTenantByName(tenantName)).andReturn(null);
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

    @Test
    public void shouldAddTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(
            mockTenantDao.getTenantRoleForParentById(null,role.getRoleRsId())).andReturn(null);
        mockTenantDao.addTenantRoleToParent(null, role);
        EasyMock.replay(mockTenantDao);
        tenantService.addTenantRole(null, role);
        EasyMock.verify(mockTenantDao);
    }

    @Test
    public void shouldAddTenantToExistingRole() {
    	//setup
        TenantRole role = getTestSingleTenantRole();
        TenantRole role2 = getTestSingleTenantRole();
        role2.setTenantIds(new String[]{tenantId2});
        
        TenantRole combined = getTestMultipleTenantRole();
        
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById(null, role.getRoleRsId())).andReturn(role2);
        mockTenantDao.updateTenantRole(EasyMock.eq(combined));
        EasyMock.replay(mockTenantDao);
        
        //execution
        tenantService.addTenantRole(null, role);
        
        //verification
        EasyMock.verify(mockTenantDao);
    }

    @Test
    public void shouldNotAddTenantRoleIfGlobalExists() {
        TenantRole role = getTestSingleTenantRole();
        TenantRole global = getTestGlobalTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById(null,role.getRoleRsId())).andReturn(global);
        EasyMock.replay(mockTenantDao);
        tenantService.addTenantRole(null, role);
        EasyMock.verify(mockTenantDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteNonExistentTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById(null,role.getRoleRsId())).andReturn(null);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRole(null, role);
    }
    
    @Test
    public void shouldDeleteGlobalTenantRole() {
        TenantRole global = getTestGlobalTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById(null,global.getRoleRsId())).andReturn(global);
        mockTenantDao.deleteTenantRole(global);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRole(null, global);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldDeleteSingleTenantRole() {
        TenantRole role = getTestSingleTenantRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById(null,role.getRoleRsId())).andReturn(role);
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
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById(null, id)).andReturn(combined);
        mockTenantDao.updateTenantRole(role2);
        EasyMock.replay(mockTenantDao);
        tenantService.deleteTenantRole(null, role);
        EasyMock.verify(mockTenantDao);
    }
    
    @Test
    public void shouldGetTenantRoleByRoleName() {
        TenantRole role = getTestSingleTenantRole();
        ClientRole cRole = getTestClientRole();
        EasyMock.expect(mockTenantDao.getTenantRoleForParentById(null, id)).andReturn(role);
        EasyMock.replay(mockTenantDao);
        EasyMock.expect(mockClientDao.getClientRoleById(id)).andReturn(cRole);
        EasyMock.replay(mockClientDao);
        TenantRole returned = tenantService.getTenantRoleForParentById(null, id);
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
        EasyMock.expect(mockTenantDao.getTenantRolesByParent(null)).andReturn(roles);
        EasyMock.replay(mockTenantDao);
        EasyMock.expect(mockClientDao.getClientRoleById(id)).andReturn(cRole);
        EasyMock.replay(mockClientDao);
        List<TenantRole> returned = tenantService.getTenantRolesByParent(null);
        Assert.assertTrue(returned.size() == 1);
        EasyMock.verify(mockTenantDao);
        EasyMock.verify(mockClientDao);
    }
    
    @Test
    public void shouldGetTenantRolesByParentAndClientId() {
        TenantRole role = getTestSingleTenantRole();
        ClientRole cRole = getTestClientRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        EasyMock.expect(mockTenantDao.getTenantRolesByParentAndClientId(null, clientId)).andReturn(roles);
        EasyMock.replay(mockTenantDao);
        EasyMock.expect(mockClientDao.getClientRoleById(id)).andReturn(cRole);
        EasyMock.replay(mockClientDao);
        List<TenantRole> returned = tenantService.getTenantRolesByParentAndClientId(null, clientId);
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
        List<Tenant> tenants = tenantService.getTenantsForParentByTenantRoles(null);
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
}
