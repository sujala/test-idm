package com.rackspace.idm.domain.dao.impl;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;

public class LdapTenantRepositoryTest {
    private LdapTenantRepository repo;
    private LdapConnectionPools connPools;
    
    private final String clientId = "YYYY";
    private final String tenantId = "XXXX";
    private final String description = "Description";
    private final boolean enabled = true;
    private final String name = "Tenant";
    private final String roleName = "Role";
    private final String dn = LdapRepository.BASE_DN;
    
    private static LdapTenantRepository getRepo(LdapConnectionPools connPools) {
        Configuration appConfig = new PropertyFileConfiguration()
            .getConfigFromClasspath();
        return new LdapTenantRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        LdapConfiguration config = new LdapConfiguration(
            new PropertyFileConfiguration().getConfigFromClasspath());
        return config.connectionPools();
    }
    
    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
    }
    
    @After
    public void tearDown() {
        connPools.close();
    }
    
    @Test
    public void shouldAddGetDeleteTenant() {
        this.repo.addTenant(getTestTenant());
        Tenant tenant = this.repo.getTenant(tenantId);
        List<Tenant> tenants = this.repo.getTenants();
        this.repo.deleteTenant(tenantId);
        Tenant notThere = this.repo.getTenant(tenantId);
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
        this.repo.deleteTenant(tenantId);
        Assert.assertTrue(duplicate);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddNullTenant() {
        this.repo.addTenant(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotDeleteNullTenantId() {
        this.repo.deleteTenant(null);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteNonExistentTenant() {
        this.repo.deleteTenant(tenantId);
    }
    
    @Test
    public void shouldUdateTenant() {
        Tenant tenant = getTestTenant();
        this.repo.addTenant(tenant);
        tenant.setDescription("Modified Description");
        tenant.setEnabled(false);
        tenant.setName("Modified Name");
        this.repo.updateTenant(tenant);
        Tenant check = this.repo.getTenant(tenantId);
        this.repo.deleteTenant(tenantId);
        Assert.assertEquals(tenant.getTenantId(), check.getTenantId());
        Assert.assertEquals(tenant.getDescription(), check.getDescription());
        Assert.assertEquals(tenant.getName(), check.getName());
        Assert.assertEquals(tenant.isEnabled(), check.isEnabled());
    }
    
    @Test 
    public void shouldAddGetDeleteTenantRole() {
        this.repo.addTenantRoleToParent(dn, getTestTenantRole());
        TenantRole role = this.repo.getTenantRoleForParentByRoleName(dn, roleName);
        TenantRole role2 = this.repo.getTenantRoleForParentByRoleNameAndClientId(dn, roleName, clientId);
        List<TenantRole> roles = this.repo.getTenantRolesByParent(dn);
        this.repo.deleteTenantRole(role);
        TenantRole notThere = this.repo.getTenantRoleForParentByRoleName(dn, roleName);
        Assert.assertNotNull(role);
        Assert.assertEquals(tenantId, role.getTenantIds()[0]);
        Assert.assertEquals(roleName, role.getName());
        Assert.assertEquals(clientId, role.getClientId());
        Assert.assertNotNull(role2);
        Assert.assertEquals(tenantId, role2.getTenantIds()[0]);
        Assert.assertEquals(roleName, role2.getName());
        Assert.assertEquals(clientId, role2.getClientId());
        Assert.assertTrue(roles.size() > 0);
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
    
    private Tenant getTestTenant() {
        Tenant tenant = new Tenant();
        tenant.setDescription(description);
        tenant.setEnabled(enabled);
        tenant.setName(name);
        tenant.setTenantId(tenantId);
        return tenant;
    }
    
    private TenantRole getTestTenantRole() {
        TenantRole role = new TenantRole();
        role.setClientId(clientId);
        role.setName(roleName);
        role.setTenantIds(new String[] {tenantId});
        return role;
    }
}
