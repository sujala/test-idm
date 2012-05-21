package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 5/21/12
 * Time: 4:10 PM
 */
public class DefaultTenantServiceTest {

    DefaultTenantService defaultTenantService;
    private TenantDao tenantDao = mock(TenantDao.class);
    private ApplicationDao clientDao = mock(ApplicationDao.class);
    private ScopeAccessDao scopeAccessDao = mock(ScopeAccessDao.class);
    private UserDao userDao = mock(UserDao.class);
    DefaultTenantService spy;

    @Before
    public void setUp() throws Exception {
        defaultTenantService = new DefaultTenantService(tenantDao, clientDao, userDao, scopeAccessDao);
        spy = spy(defaultTenantService);


    }

    @Test
    public void hasTenantAccess_tenantIdBlank_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        boolean  userHasTenant = spy.hasTenantAccess(scopeAccess, "");
        assertThat("has tenant", userHasTenant, equalTo(false));
    }

    @Test
    public void hasTenantAccess_tenantIdNull_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        boolean  userHasTenant = spy.hasTenantAccess(scopeAccess, null);
        assertThat("has tenant", userHasTenant, equalTo(false));
    }

    @Test
    public void hasTenantAccess_callsGetTenantsForScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(new ArrayList<Tenant>()).when(spy).getTenantsForScopeAccessByTenantRoles(scopeAccess);
        spy.hasTenantAccess(scopeAccess, "tenantId");
        verify(spy).getTenantsForScopeAccessByTenantRoles(scopeAccess);
    }

    @Test
    public void hasTenantAccess_tenantExistsForUser_returnsTrue() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ArrayList<Tenant> tenants = new ArrayList<Tenant>();
        Tenant tenant = new Tenant();
        tenant.setName("tenantId");
        tenants.add(tenant);
        doReturn(tenants).when(spy).getTenantsForScopeAccessByTenantRoles(scopeAccess);
        boolean userHasTenant = spy.hasTenantAccess(scopeAccess, "tenantId");
        assertThat("has tenant", userHasTenant, equalTo(true));
    }

    @Test
    public void hasTenantAccess_tenantDoesNotExistForUser_returnsTrue() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ArrayList<Tenant> tenants = new ArrayList<Tenant>();
        doReturn(tenants).when(spy).getTenantsForScopeAccessByTenantRoles(scopeAccess);
        boolean userHasTenant = spy.hasTenantAccess(scopeAccess, "tenantId");
        assertThat("has tenant", userHasTenant, equalTo(false));
    }
}
