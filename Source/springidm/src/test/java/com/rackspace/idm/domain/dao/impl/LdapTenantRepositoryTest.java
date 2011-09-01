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
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;

public class LdapTenantRepositoryTest {
    private LdapTenantRepository repo;
    private LdapConnectionPools connPools;
    
    private final String tenantId = "XXXX";
    private final String description = "Description";
    private final boolean enabled = true;
    private final String name = "Tenant";
    
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
    
    private Tenant getTestTenant() {
        Tenant tenant = new Tenant();
        tenant.setDescription(description);
        tenant.setEnabled(enabled);
        tenant.setName(name);
        tenant.setTenantId(tenantId);
        return tenant;
    }
}
