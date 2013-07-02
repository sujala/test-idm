package com.rackspace.idm.domain.dao.impl;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.*;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class LdapTenantRepositoryIntegrationTestOld extends InMemoryLdapIntegrationTest{
    @Autowired
    private LdapTenantRepository tenantRepository;
    @Autowired
    private LdapTenantRoleRepository tenantRoleRepository;
    @Autowired
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
    
    @Before
    public void preTestSetUp() throws Exception {
        //cleanup before test
        try{
            tenantRepository.deleteTenant(tenantId);
        }catch (Exception e){
            System.out.println("failed to delete tenant");
        }
    }

    @Test
    public void shouldAddGetDeleteTenant() {
        this.tenantRepository.addTenant(getTestTenant());
        Tenant tenant = this.tenantRepository.getTenant(tenantId);
        List<Tenant> tenants = this.tenantRepository.getTenants();
        this.tenantRepository.deleteTenant(tenantId);
        Tenant notThere = this.tenantRepository.getTenant(tenantId);
        Assert.assertNotNull(tenant);
        Assert.assertEquals(tenantId, tenant.getTenantId());
        Assert.assertEquals(description, tenant.getDescription());
        Assert.assertEquals(name, tenant.getName());
        Assert.assertEquals(enabled, tenant.getEnabled());
        Assert.assertTrue(tenants.size() > 0);
        Assert.assertNull(notThere);
    }
    
    @Test
    public void shouldNotAddDuplicateTenant() {
        this.tenantRepository.addTenant(getTestTenant());
        boolean duplicate = false;
        try {
            this.tenantRepository.addTenant(getTestTenant());
        } catch (DuplicateException ex) {
            duplicate = true;
        }
        this.tenantRepository.deleteTenant(tenantId);
        Assert.assertTrue(duplicate);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAddNullTenant() {
        this.tenantRepository.addTenant(null);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteNonExistentTenant() {
        this.tenantRepository.deleteTenant(tenantId);
    }
    
    @Test
    public void shouldUdateTenant() {
        Tenant tenant = getTestTenant();
        this.tenantRepository.addTenant(tenant);
        tenant.setDescription("Modified Description");
        tenant.setEnabled(false);
        tenant.setName("Modified Name");
        this.tenantRepository.updateTenant(tenant);
        Tenant check = this.tenantRepository.getTenant(tenantId);
        this.tenantRepository.deleteTenant(tenantId);
        Assert.assertEquals(tenant.getTenantId(), check.getTenantId());
        Assert.assertEquals(tenant.getDescription(), check.getDescription());
        Assert.assertEquals(tenant.getName(), check.getName());
        Assert.assertEquals(tenant.getEnabled(), check.getEnabled());
    }
    
    private Tenant getTestTenant() {
        Tenant tenant = new Tenant();
        tenant.setDescription(description);
        tenant.setEnabled(enabled);
        tenant.setName(name);
        tenant.setDisplayName(displayName);
        tenant.setTenantId(tenantId);
        return tenant;
    }
    
    private TenantRole getTestTenantRole() {
        TenantRole role = new TenantRole();
        role.setRoleRsId(id);
        role.setClientId(clientId);
        role.setUserId(userId);
        role.setName(roleName);
        role.getTenantIds().add(tenantId);
        return role;
    }
}
