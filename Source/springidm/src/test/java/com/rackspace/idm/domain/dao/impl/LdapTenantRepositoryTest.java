package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import com.rackspace.idm.GlobalConstants;

public class LdapTenantRepositoryTest {
    private LdapTenantRepository repo;
    private LdapConnectionPools connPools;
    
    private final String clientId = "YYYY";
    private final String tenantId = "XXXX";
    private final String description = "Description";
    private final boolean enabled = true;
    private final String name = "XXXX";
    private final String roleName = "Role";
    private final String dn = LdapRepository.BASE_DN;
    private final String displayName = "Display Name";
    private final String id = "XXX";
    private final String userId = "1";
    
    private static LdapTenantRepository getRepo(LdapConnectionPools connPools) {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapTenantRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        LdapConfiguration config = new LdapConfiguration(appConfig);
        return config.connectionPools();
    }
    
    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
        //cleanup before test
        try{
            repo.deleteTenant(tenantId, GlobalConstants.DEFAULT_TENANT_SCOPEID);
        }catch (Exception e){
            System.out.println("failed to delete tenant");
        }
    }
    
    @After
    public void tearDown() {
        connPools.close();
    }
    
    @Test
    public void shouldAddGetDeleteTenant() {
        this.repo.addTenant(getTestTenant());
        Tenant tenant = this.repo.getTenant(tenantId, GlobalConstants.DEFAULT_TENANT_SCOPEID);
        List<Tenant> tenants = this.repo.getTenants();
        this.repo.deleteTenant(tenantId, GlobalConstants.DEFAULT_TENANT_SCOPEID);
        Tenant notThere = this.repo.getTenant(tenantId, GlobalConstants.DEFAULT_TENANT_SCOPEID);
        Assert.assertNotNull(tenant);
        Assert.assertEquals(tenantId, tenant.getTenantId());
        Assert.assertEquals(description, tenant.getDescription());
        Assert.assertEquals(name, tenant.getName());
        Assert.assertEquals(enabled, tenant.isEnabled());
        Assert.assertTrue(tenants.size() > 0);
        Assert.assertNull(notThere);
    }
    
    @Test
    public void shouldNotAddDuplicateTenant() {
        this.repo.addTenant(getTestTenant());
        boolean duplicate = false;
        try {
            this.repo.addTenant(getTestTenant());
        } catch (DuplicateException ex) {
            duplicate = true;
        }
        this.repo.deleteTenant(tenantId, GlobalConstants.DEFAULT_TENANT_SCOPEID);
        Assert.assertTrue(duplicate);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddNullTenant() {
        this.repo.addTenant(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotDeleteNullTenantId() {
        this.repo.deleteTenant(null, GlobalConstants.DEFAULT_TENANT_SCOPEID);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteNonExistentTenant() {
        this.repo.deleteTenant(tenantId, GlobalConstants.DEFAULT_TENANT_SCOPEID);
    }
    
    @Test
    public void shouldUdateTenant() {
        Tenant tenant = getTestTenant();
        this.repo.addTenant(tenant);
        tenant.setDescription("Modified Description");
        tenant.setEnabled(false);
        tenant.setName("Modified Name");
        this.repo.updateTenant(tenant);
        Tenant check = this.repo.getTenant(tenantId, GlobalConstants.DEFAULT_TENANT_SCOPEID);
        this.repo.deleteTenant(tenantId, GlobalConstants.DEFAULT_TENANT_SCOPEID);
        Assert.assertEquals(tenant.getTenantId(), check.getTenantId());
        Assert.assertEquals(tenant.getDescription(), check.getDescription());
        Assert.assertEquals(tenant.getName(), check.getName());
        Assert.assertEquals(tenant.isEnabled(), check.isEnabled());
    }
    
    @Test 
    public void shouldAddGetDeleteTenantRole() {
        this.repo.addTenantRoleToParent(dn, getTestTenantRole());
        TenantRole role = this.repo.getTenantRoleForParentById(dn, id);
        List<TenantRole> roles = this.repo.getTenantRolesByParent(dn);
        List<TenantRole> roles2 = this.repo.getTenantRolesByParentAndClientId(dn, clientId);
        this.repo.deleteTenantRole(role);
        TenantRole notThere = this.repo.getTenantRoleForParentById(dn, id);
        Assert.assertNotNull(role);
        Assert.assertEquals(tenantId, role.getTenantIds()[0]);
        Assert.assertEquals(id, role.getRoleRsId());
        Assert.assertEquals(clientId, role.getClientId());
        Assert.assertTrue(roles.size() > 0);
        Assert.assertTrue(roles2.size() > 0);
        Assert.assertNull(notThere);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddNullTenantRole() {
        this.repo.addTenantRoleToParent(dn, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddTenantRoleWithBlankParent() {
        this.repo.addTenantRoleToParent(null, getTestTenantRole());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotDeleteNullTenantRole() {
        this.repo.deleteTenantRole(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetTenantRoleWithBlankParent() {
        this.repo.getTenantRoleForParentById(null, roleName);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetTenantRoleWithBlankId() {
        this.repo.getTenantRoleForParentById(dn, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetTenantRolesWithBlankParent() {
        this.repo.getTenantRolesByParent(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetTenantRolesByClientIdWithBlankParent() {
        this.repo.getTenantRolesByParentAndClientId(null, clientId);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetTenantRolesByClientIdWithBlankClientId() {
        this.repo.getTenantRolesByParentAndClientId(dn, null);
    }
    
    private Tenant getTestTenant() {
        Tenant tenant = new Tenant();
        tenant.setDescription(description);
        tenant.setEnabled(enabled);
        tenant.setName(name);
        tenant.setDisplayName(displayName);
        tenant.setScopeId(GlobalConstants.DEFAULT_TENANT_SCOPEID);
        tenant.setTenantId(tenantId);
        return tenant;
    }
    
    private TenantRole getTestTenantRole() {
        TenantRole role = new TenantRole();
        role.setRoleRsId(id);
        role.setClientId(clientId);
        role.setUserId(userId);
        role.setName(roleName);
        role.setTenantIds(new String[] {tenantId});
        return role;
    }
}
