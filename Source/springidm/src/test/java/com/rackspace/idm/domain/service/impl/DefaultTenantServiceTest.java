package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
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

    @Test (expected = IllegalArgumentException.class)
    public void addTenant_nullTenant_throwIllegalArgumentException() throws Exception {
        when(tenantDao.getTenant(null)).thenReturn(new Tenant());
        defaultTenantService.addTenant(null);
    }

    @Test (expected = DuplicateException.class)
    public void addTenant_tenantExists_throwDuplicateException() throws Exception {
        when(tenantDao.getTenant(null)).thenReturn(new Tenant());
        defaultTenantService.addTenant(new Tenant());
    }

    @Test
    public void getTenantByName_callsTenantDaoMethod() throws Exception {
        defaultTenantService.getTenantByName(null);
        verify(tenantDao).getTenantByName(null);
    }

    @Test
    public void getTenantsForParentsByTenantRoles_tenantIdFound_returnsTenantListWithNoDuplicates() throws Exception {
        String[] tenantIds = {"123","123","123"};
        TenantRole tenantRole = new TenantRole();
        tenantRole.setTenantIds(tenantIds);
        ArrayList<TenantRole> list = new ArrayList<TenantRole>();
        list.add(tenantRole);
        Tenant tenant = new Tenant();
        tenant.setEnabled(true);
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(list);
        doReturn(tenant).when(spy).getTenant("123");
        assertThat("tenant list size", spy.getTenantsForParentByTenantRoles(null).size(), equalTo(1));
    }

    @Test
    public void getTenantsForParentsByTenantRoles_tenantNotEnabled_returnsEmptyTenantList() throws Exception {
        String[] tenantIds = {"123"};
        TenantRole tenantRole = new TenantRole();
        tenantRole.setTenantIds(tenantIds);
        ArrayList<TenantRole> list = new ArrayList<TenantRole>();
        list.add(tenantRole);
        Tenant tenant = new Tenant();
        tenant.setEnabled(false);
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(list);
        doReturn(tenant).when(spy).getTenant("123");
        assertThat("tenant list size",spy.getTenantsForParentByTenantRoles(null).size(), equalTo(0));
    }

    @Test
    public void getTenantsForParentsByTenantRoles_tenantNull_returnsEmptyTenantList() throws Exception {
        String[] tenantIds = {"123"};
        TenantRole tenantRole = new TenantRole();
        tenantRole.setTenantIds(tenantIds);
        ArrayList<TenantRole> list = new ArrayList<TenantRole>();
        list.add(tenantRole);
        Tenant tenant = new Tenant();
        tenant.setEnabled(false);
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(list);
        doReturn(null).when(spy).getTenant("123");
        assertThat("tenant list size",spy.getTenantsForParentByTenantRoles(null).size(), equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getTenantsForScopeAccessByTenantRoles_actionFails_throwsIllegalStateException() throws Exception {
        defaultTenantService.getTenantsForScopeAccessByTenantRoles(null);
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
    public void hasTenantAccess_tenantDoesNotExistForUser_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ArrayList<Tenant> tenants = new ArrayList<Tenant>();
        doReturn(tenants).when(spy).getTenantsForScopeAccessByTenantRoles(scopeAccess);
        boolean userHasTenant = spy.hasTenantAccess(scopeAccess, "tenantId");
        assertThat("has tenant", userHasTenant, equalTo(false));
    }

    @Test
    public void hasTenantAccess_scopeAccessIsNull_returnsFalse() throws Exception {
        boolean  userHasTenant = spy.hasTenantAccess(null, "hello");
        assertThat("does not have tenant", userHasTenant, equalTo(false));
    }

    @Test
    public void hasTenantAccess_tenantIdsMatch_returnsTrue() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTenantId("123");
        ScopeAccess scopeAccess = new ScopeAccess();
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        list.add(tenant);
        doReturn(list).when(spy).getTenantsForScopeAccessByTenantRoles(scopeAccess);
        boolean  userHasTenant = spy.hasTenantAccess(scopeAccess, "123");
        assertThat("has tenant", userHasTenant, equalTo(true));
    }

    @Test
    public void hasTenantAccess_tenantIdsDoNotMatchAndNameNull_returnsFalse() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTenantId("321");
        ScopeAccess scopeAccess = new ScopeAccess();
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        list.add(tenant);
        doReturn(list).when(spy).getTenantsForScopeAccessByTenantRoles(scopeAccess);
        boolean  userHasTenant = spy.hasTenantAccess(scopeAccess, "123");
        assertThat("does not have tenant", userHasTenant, equalTo(false));
    }

    @Test
    public void hasTenantAccess_tenantNamesDoNotMatch_returnsFalse() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        ScopeAccess scopeAccess = new ScopeAccess();
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        list.add(tenant);
        doReturn(list).when(spy).getTenantsForScopeAccessByTenantRoles(scopeAccess);
        boolean  userHasTenant = spy.hasTenantAccess(scopeAccess, "notTenant");
        assertThat("has tenant", userHasTenant, equalTo(false));
    }

    @Test
    public void hasTenantAccess_TenantNameAndIdNull_returnsFalse() throws Exception {
        Tenant tenant = new Tenant();
        ScopeAccess scopeAccess = new ScopeAccess();
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        list.add(tenant);
        doReturn(list).when(spy).getTenantsForScopeAccessByTenantRoles(scopeAccess);
        boolean  userHasTenant = spy.hasTenantAccess(scopeAccess, "123");
        assertThat("has tenant", userHasTenant, equalTo(false));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRole_roleIsNull_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.addTenantRole(null, null);
    }

    @Test
    public void addTenantRole_givenRoleTenantIdsEmpty_setsExistingTenantIdsToNull() throws Exception {
        String[] tenantIds = {"123"};
        String[] tenantIds2 = {};
        TenantRole tenantRole1 = new TenantRole();
        tenantRole1.setTenantIds(tenantIds2);
        TenantRole tenantRole2 = new TenantRole();
        tenantRole2.setTenantIds(tenantIds);
        when(tenantDao.getTenantRoleForParentById(null, null)).thenReturn(tenantRole2);
        doNothing().when(tenantDao).updateTenantRole(tenantRole2);
        defaultTenantService.addTenantRole(null, tenantRole1);
        assertThat("tenant Ids",tenantRole2.getTenantIds(),nullValue());
    }

    @Test
    public void addTenantRole_givenRoleTenantIdsNull_setsExistingTenantIdsToNull() throws Exception {
        String[] tenantIds = {"123"};
        TenantRole tenantRole1 = new TenantRole();
        TenantRole tenantRole2 = new TenantRole();
        tenantRole2.setTenantIds(tenantIds);
        when(tenantDao.getTenantRoleForParentById(null, null)).thenReturn(tenantRole2);
        doNothing().when(tenantDao).updateTenantRole(tenantRole2);
        defaultTenantService.addTenantRole(null, tenantRole1);
        assertThat("tenant Ids",tenantRole2.getTenantIds(),nullValue());
    }

    @Test (expected = ClientConflictException.class)
    public void addTenantRole_givenRoleTenantIdsSameAsExisting_throwsClientConflictException() throws Exception {
        String[] tenantIds = {"123"};
        String[] tenantIds2 = {"123"};
        TenantRole tenantRole1 = new TenantRole();
        tenantRole1.setTenantIds(tenantIds2);
        TenantRole tenantRole2 = new TenantRole();
        tenantRole2.setTenantIds(tenantIds);
        when(tenantDao.getTenantRoleForParentById(null,null)).thenReturn(tenantRole2);
        defaultTenantService.addTenantRole(null,tenantRole1);
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteTenantRole_nullRole_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.deleteTenantRole(null, null);
    }

    @Test (expected = AssertionError.class)
    public void deleteTenantRole_roleExistsAndGivenTenantIdNotInExistingRole_doesNothing() throws Exception {
        String[] tenantIds = {"123"};
        String[] tenantIds2 = {"456"};
        TenantRole tenantRole = new TenantRole();
        tenantRole.setTenantIds(tenantIds);
        TenantRole tenantRole2 = new TenantRole();
        tenantRole2.setTenantIds(tenantIds2);
        when(tenantDao.getTenantRoleForParentById(null, null)).thenReturn(tenantRole2);
        defaultTenantService.deleteTenantRole(null,tenantRole);
        verify(tenantDao).deleteTenantRole(tenantRole2);
    }

    @Test
    public void deleteGlobalRole_callsTenantDaoMethod() throws Exception {
        defaultTenantService.deleteGlobalRole(null);
        verify(tenantDao).deleteTenantRole(null);
    }

    @Test(expected = NotFoundException.class)
    public void getTenantRoleForParentById_roleIsNull_throwsNotFoundException() throws Exception {
        when(tenantDao.getTenantRoleForParentById(null,null)).thenReturn(null);
        defaultTenantService.getTenantRoleForParentById(null,null);
    }

    @Test
    public void getTenantRolesForScopeAccess_scopeAccessInstanceOfDelegatedClientScopeAccess_parentDnIsAssignedUniqueId() throws Exception {

        DelegatedClientScopeAccess delegatedClientScopeAccess = mock(DelegatedClientScopeAccess.class);
        when(delegatedClientScopeAccess.getUniqueId()).thenReturn("123");

        defaultTenantService.getTenantRolesForScopeAccess(delegatedClientScopeAccess);
        verify(tenantDao).getTenantRolesByParent("123");

    }

    @Test
    public void getTenantRolesForScopeAccess_scopeAccessNotInstanceOfDelegatedClientScopeAccess_parentDnIsAssignedParentDNString() throws Exception {
        DN parent = new DN("dn=hello");
        DN dn = new DN(new RDN("rdn=foo"),parent);
        Entry entry = new Entry(dn);
        Entry readOnlyEntry = new ReadOnlyEntry(entry);
        ScopeAccess scopeAccess = mock(ScopeAccess.class);
        when(scopeAccess.getLDAPEntry()).thenReturn((ReadOnlyEntry) readOnlyEntry);

        defaultTenantService.getTenantRolesForScopeAccess(scopeAccess);
        verify(tenantDao).getTenantRolesByParent("dn=hello");

    }

    @Test (expected = IllegalStateException.class)
    public void getTenantRolesForScopeAccess_gettingParentDNFails_throwsIllegalStateException() throws Exception {
        ScopeAccess scopeAccess = mock(ScopeAccess.class);

        defaultTenantService.getTenantRolesForScopeAccess(scopeAccess);

    }

    @Test
    public void getTenantRolesForScopeAccess_rolesListNotEmpty_returnsPopulatedList() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setName("John Smith");
        clientRole.setDescription("this is a description");
        List<TenantRole> list = new ArrayList<TenantRole>();
        list.add(new TenantRole());
        list.add(new TenantRole());
        DelegatedClientScopeAccess delegatedClientScopeAccess = mock(DelegatedClientScopeAccess.class);
        when(delegatedClientScopeAccess.getUniqueId()).thenReturn("123");
        when(tenantDao.getTenantRolesByParent("123")).thenReturn(list);
        when(clientDao.getClientRoleById(null)).thenReturn(clientRole);

        List <TenantRole> roles = defaultTenantService.getTenantRolesForScopeAccess(delegatedClientScopeAccess);
        assertThat("list size",roles.size(),equalTo(2));
        assertThat("role name", roles.get(0).getName(),equalTo("John Smith"));
        assertThat("role description",roles.get(0).getDescription(),equalTo("this is a description"));

    }

    @Test
    public void getTenantRolesForScopeAccess_rolesListEmpty_returnsEmptyList() throws Exception {
        List<TenantRole> list = new ArrayList<TenantRole>();
        DelegatedClientScopeAccess delegatedClientScopeAccess = mock(DelegatedClientScopeAccess.class);
        when(delegatedClientScopeAccess.getUniqueId()).thenReturn("123");
        when(tenantDao.getTenantRolesByParent("123")).thenReturn(list);

        List <TenantRole> roles = defaultTenantService.getTenantRolesForScopeAccess(delegatedClientScopeAccess);
        assertThat("list size",roles.size(),equalTo(0));
     }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_userIsNullAndRoleIsNull_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.addTenantRoleToUser(null,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_userIsNullAndRoleExists_throwsIllegalArgumentException() throws Exception {
        TenantRole role = new TenantRole();
        defaultTenantService.addTenantRoleToUser(null,role);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_userIdIsBlankAndRoleIsNull_throwsIllegalArgumentException() throws Exception {
        User user = new User();
        user.setUniqueId("");
        defaultTenantService.addTenantRoleToUser(user,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_userIdIsBlankAndRoleExists_throwsIllegalArgumentException() throws Exception {
        User user = new User();
        user.setUniqueId("");
        TenantRole tenantRole = new TenantRole();
        defaultTenantService.addTenantRoleToUser(user,tenantRole);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_nullRole_throwsIllegalArgumentException() throws Exception {
        User user = new User();
        user.setUniqueId("123");
        defaultTenantService.addTenantRoleToUser(user,null);
    }

    @Test (expected = NotFoundException.class)
    public void addTenantRoleToUser_clientIsNull_throwsNotFoundException() throws Exception {
        User user = new User();
        user.setUniqueId("123");
        TenantRole role = new TenantRole();
        when(clientDao.getClientByClientId(null)).thenReturn(null);
        defaultTenantService.addTenantRoleToUser(user,role);

    }

    @Test (expected = NotFoundException.class)
    public void addTenantRoleToUser_clientRoleIsNull_throwsNotFoundException() throws Exception {
        User user = new User();
        user.setUniqueId("123");
        TenantRole role = new TenantRole();
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName(null,null)).thenReturn(null);
        defaultTenantService.addTenantRoleToUser(user,role);

    }

    @Test
    public void addTenantRoleToUser_ScopeAccessIsNullAndUserIsRacker_makesRackerScopeAccess() throws Exception {
        User user = new Racker();
        user.setUniqueId("123");
        TenantRole role = new TenantRole();
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName(null, null)).thenReturn(new ClientRole());
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId("123", null)).thenReturn(null);
        when(scopeAccessDao.addDirectScopeAccess(eq("123"), any(RackerScopeAccess.class))).thenReturn(new ScopeAccess());
        doNothing().when(spy).addTenantRole(null, role);
        spy.addTenantRoleToUser(user, role);
        verify(scopeAccessDao).addDirectScopeAccess(eq("123"),any(RackerScopeAccess.class));

    }

    @Test
    public void addTenantRoleToUser_ScopeAccessIsNullAndUserIsNotRacker_makesUserScopeAccess() throws Exception {
        User user = new User();
        user.setUniqueId("123");
        TenantRole role = new TenantRole();
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName(null, null)).thenReturn(new ClientRole());
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId("123", null)).thenReturn(null);
        when(scopeAccessDao.addDirectScopeAccess(eq("123"),any(UserScopeAccess.class))).thenReturn(new ScopeAccess());
        doNothing().when(spy).addTenantRole(null,role);
        spy.addTenantRoleToUser(user, role);
        verify(scopeAccessDao).addDirectScopeAccess(eq("123"),any(UserScopeAccess.class));

    }

    @Test (expected = AssertionError.class)
    public void addTenantRoleToUser_scopeAccessNotNull_doesNotCallScopeAccessDaoMethodAdd() throws Exception {
        User user = new User();
        user.setUniqueId("123");
        TenantRole role = new TenantRole();
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName(null, null)).thenReturn(new ClientRole());
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId("123", null)).thenReturn(new ScopeAccess());
        when(scopeAccessDao.addDirectScopeAccess(eq("123"),any(UserScopeAccess.class))).thenReturn(new ScopeAccess());
        doNothing().when(spy).addTenantRole(null,role);
        spy.addTenantRoleToUser(user, role);
        verify(scopeAccessDao).addDirectScopeAccess(eq("123"),any(ScopeAccess.class));

    }

    @Test
    public void addTenantRoleToUser_callsAddTenantRole() throws Exception {
        User user = new User();
        user.setUniqueId("123");
        TenantRole role = new TenantRole();
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName(null, null)).thenReturn(new ClientRole());
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId("123", null)).thenReturn(null);
        when(scopeAccessDao.addDirectScopeAccess(eq("123"),any(UserScopeAccess.class))).thenReturn(new ScopeAccess());
        doNothing().when(spy).addTenantRole(null,role);
        spy.addTenantRoleToUser(user, role);
        verify(spy).addTenantRole(null,role);

    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToClient_clientIsNullAndRoleIsNull_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.addTenantRoleToClient(null,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToClient_clientIsNullAndRoleExists_throwsIllegalArgumentException() throws Exception {
        TenantRole tenantRole = new TenantRole();
        defaultTenantService.addTenantRoleToClient(null,tenantRole);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToClient_clientIdIsBlankAndRoleIsNull_throwsIllegalArgumentException() throws Exception {
        Application application = new Application();
        application.setUniqueId("");
        defaultTenantService.addTenantRoleToClient(application,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToClient_clientIdIsBlankAndRoleExists_throwsIllegalArgumentException() throws Exception {
        TenantRole tenantRole = new TenantRole();
        Application application = new Application();
        application.setUniqueId("");
        defaultTenantService.addTenantRoleToClient(application,tenantRole);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToClient_clientIdIsNotBlankAndRoleIsNull_throwsIllegalArgumentException() throws Exception {
        Application application = new Application();
        application.setUniqueId("123");
        defaultTenantService.addTenantRoleToClient(application,null);
    }

    @Test (expected = NotFoundException.class)
    public void addTenantRoleToClient_ownerIsNull_throwsNotFoundException() throws Exception {
        TenantRole tenantRole = new TenantRole();
        Application application = new Application();
        application.setUniqueId("123");
        when(clientDao.getClientByClientId(null)).thenReturn(null);
        defaultTenantService.addTenantRoleToClient(application,tenantRole);
    }

    @Test (expected = NotFoundException.class)
    public void addTenantRoleToClient_clientRoleIsNull_throwsNotFoundException() throws Exception {
        TenantRole tenantRole = new TenantRole();
        Application application = new Application();
        application.setUniqueId("123");
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName(null,null)).thenReturn(null);
        defaultTenantService.addTenantRoleToClient(application, tenantRole);
    }

    @Test
    public void addTenantRoleToClient_scopeAccessIsNull_callsScopeAccessDaoMethod() throws Exception {
        TenantRole tenantRole = new TenantRole();
        Application application = new Application();
        application.setUniqueId("123");
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName(null, null)).thenReturn(new ClientRole());
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId("123", null)).thenReturn(null);
        when(scopeAccessDao.addDirectScopeAccess(eq("123"),any(ScopeAccess.class))).thenReturn(new ScopeAccess());
        doNothing().when(spy).addTenantRole(null,tenantRole);
        spy.addTenantRoleToClient(application,tenantRole);
        verify(scopeAccessDao).addDirectScopeAccess(eq("123"), any(ScopeAccess.class));
    }

    @Test (expected = AssertionError.class)
    public void addTenantRoleToClient_scopeAccessIsNotNull_doesNotCallScopeAccessDaoMethod() throws Exception {
        TenantRole tenantRole = new TenantRole();
        Application application = new Application();
        application.setUniqueId("123");
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName(null, null)).thenReturn(new ClientRole());
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId("123", null)).thenReturn(new ScopeAccess());
        doNothing().when(spy).addTenantRole(null, tenantRole);
        spy.addTenantRoleToClient(application,tenantRole);
        verify(scopeAccessDao).addDirectScopeAccess(eq("123"),any(ScopeAccess.class));
    }

    @Test
    public void addTenantRoleToClient_callsAddTenantRoles() throws Exception {
        TenantRole tenantRole = new TenantRole();
        Application application = new Application();
        application.setUniqueId("123");
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(clientDao.getClientRoleByClientIdAndRoleName(null, null)).thenReturn(new ClientRole());
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId("123", null)).thenReturn(new ScopeAccess());
        doNothing().when(spy).addTenantRole(null,tenantRole);
        spy.addTenantRoleToClient(application,tenantRole);
        verify(spy).addTenantRole(null,tenantRole);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getGlobalRolesForUser_nullUser_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.getGlobalRolesForUser(null);
    }

    @Test
    public void getGlobalRolesForUser_userParameter_callsTenantDaoMethod() throws Exception {
        User user = new User();
        defaultTenantService.getGlobalRolesForUser(user);
        verify(tenantDao).getTenantRolesForUser(user);
    }

    @Test
    public void getGlobalRolesForUser_userParameter_returnsList() throws Exception {
        User user = new User();
        assertThat("tenant role list",defaultTenantService.getGlobalRolesForUser(user),instanceOf(ArrayList.class));

    }

    @Test
    public void getGlobalRolesForUser_userAndFilterParameters_callsTenantDaoMethod() throws Exception {
        defaultTenantService.getGlobalRolesForUser(null,null);
        verify(tenantDao).getTenantRolesForUser(null,null);
    }

    @Test
    public void getGlobalRolesForUser_userAndFilterParameters_returnsList() throws Exception {
        assertThat("tenant role list", defaultTenantService.getGlobalRolesForUser(null, null), instanceOf(ArrayList.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getGlobalRolesForApplication_nullApplication_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.getGlobalRolesForApplication(null,null);
    }

    @Test
    public void getGlobalRolesForApplication_callsTenantRoleDaoMethod() throws Exception {
        Application application = new Application();
        defaultTenantService.getGlobalRolesForApplication(application,null);
        verify(tenantDao).getTenantRolesForApplication(application,null);
    }

    @Test
    public void getGlobalRolesForApplication_returnsList() throws Exception {
        assertThat("tenant role list",defaultTenantService.getGlobalRolesForApplication(new Application(),null),instanceOf(ArrayList.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getTenantRolesForUserOnTenant_nullTenant_throwsIllegalArgumentException() throws Exception {
        String[] tenantIds = {"123"};
        TenantRole tenantRole = new TenantRole();
        tenantRole.setTenantIds(tenantIds);
        ArrayList<TenantRole> list = new ArrayList<TenantRole>();
        list.add(tenantRole);
        when(tenantDao.getTenantRolesForUser(null)).thenReturn(list);
        defaultTenantService.getTenantRolesForUserOnTenant(null,null);
    }

    @Test
    public void getTenantRolesForUserOnTenant_roleTenantIdMatches_returnsCorrectListOfTenantRoles() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTenantId("123");
        String[] tenantIds = {"123"};
        TenantRole tenantRole = new TenantRole();
        tenantRole.setTenantIds(tenantIds);
        tenantRole.setClientId("456");
        tenantRole.setRoleRsId("789");
        tenantRole.setName("John Smith");
        ArrayList<TenantRole> list = new ArrayList<TenantRole>();
        list.add(tenantRole);
        when(tenantDao.getTenantRolesForUser(null)).thenReturn(list);
        List<TenantRole> roles = defaultTenantService.getTenantRolesForUserOnTenant(null,tenant);
        assertThat("list size",roles.size(),equalTo(1));
        assertThat("client id",roles.get(0).getClientId(),equalTo("456"));
        assertThat("role rs id",roles.get(0).getRoleRsId(),equalTo("789"));
        assertThat("name",roles.get(0).getName(),equalTo("John Smith"));
        assertThat("tenantId",roles.get(0).getTenantIds()[0],equalTo("123"));
    }

    @Test
    public void getTenantRolesForUserOnTenant_roleTenantIdDoesNotMatch_returnsEmptyTenantRolesList() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTenantId("456");
        String[] tenantIds = {"123"};
        TenantRole tenantRole = new TenantRole();
        tenantRole.setTenantIds(tenantIds);
        ArrayList<TenantRole> list = new ArrayList<TenantRole>();
        list.add(tenantRole);
        when(tenantDao.getTenantRolesForUser(null)).thenReturn(list);
        List<TenantRole> roles = defaultTenantService.getTenantRolesForUserOnTenant(null,tenant);
        assertThat("list size",roles.size(),equalTo(0));
    }

    @Test
    public void getTenantRolesForUserOnTenant_userHasNoRoles_returnsEmptyTenantRolesList() throws Exception {
        Tenant tenant = new Tenant();
        ArrayList<TenantRole> list = new ArrayList<TenantRole>();
        when(tenantDao.getTenantRolesForUser(null)).thenReturn(list);
        List<TenantRole> roles = defaultTenantService.getTenantRolesForUserOnTenant(null,tenant);
        assertThat("list size",roles.size(),equalTo(0));
    }

    @Test
    public void getTenantRolesForUser_userHasRoles_returnsPopulatedList() throws Exception {
        TenantRole role = new TenantRole();
        ArrayList<TenantRole> list = new ArrayList<TenantRole>();
        list.add(role);
        ClientRole clientRole = new ClientRole();
        clientRole.setName("John Smith");
        clientRole.setDescription("this is a description");
        when(tenantDao.getTenantRolesForUser(null,null)).thenReturn(list);
        when(clientDao.getClientRoleById(null)).thenReturn(clientRole);
        doReturn(list).when(spy).getTenantOnlyRoles(list);
        List<TenantRole> roles = spy.getTenantRolesForUser(null,null);

        assertThat("list size",roles.size(),equalTo(1));
        assertThat("role name",roles.get(0).getName(),equalTo("John Smith"));
        assertThat("role description", roles.get(0).getDescription(),equalTo("this is a description"));
    }

    @Test
    public void getTenantRolesForUser_userHasNoRoles_returnsEmptyList() throws Exception {
        ArrayList<TenantRole> list = new ArrayList<TenantRole>();
        when(tenantDao.getTenantRolesForUser(null,null)).thenReturn(list);
        doReturn(list).when(spy).getTenantOnlyRoles(list);
        List<TenantRole> roles = spy.getTenantRolesForUser(null,null);

        assertThat("list size",roles.size(),equalTo(0));
    }

    @Test
    public void getTenantRolesForApplication_callsTenantDaoMethod() throws Exception {
        when(tenantDao.getTenantRolesForApplication(null,null)).thenReturn(null);
        doReturn(new ArrayList<TenantRole>()).when(spy).getTenantOnlyRoles(null);
        spy.getTenantRolesForApplication(null,null);
        verify(tenantDao).getTenantRolesForApplication(null,null);
    }

    @Test
    public void getTenantRolesForApplication_callsGetTenantOnlyRoles() throws Exception {
        when(tenantDao.getTenantRolesForApplication(null,null)).thenReturn(null);
        doReturn(new ArrayList<TenantRole>()).when(spy).getTenantOnlyRoles(null);
        spy.getTenantRolesForApplication(null,null);
        verify(spy).getTenantOnlyRoles(null);
    }

    @Test
    public void getTenantRolesForApplication_returnsList() throws Exception {
        when(tenantDao.getTenantRolesForApplication(null,null)).thenReturn(null);
        doReturn(new ArrayList<TenantRole>()).when(spy).getTenantOnlyRoles(null);
        assertThat("tenant roles", spy.getTenantRolesForApplication(null, null), instanceOf(List.class));
    }

    @Test
    public void getTenantRolesForTenant_clientRoleExists_returnsPopulatedTenantRoleList() throws Exception {
        TenantRole role = new TenantRole();
        role.setRoleRsId("123");
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        ClientRole cRole = new ClientRole();
        cRole.setClientId("456");
        cRole.setDescription("this is a description");
        cRole.setName("John Smith");
        cRole.setId("789");
        when(tenantDao.getAllTenantRolesForTenant("id")).thenReturn(roles);
        when(clientDao.getClientRoleById("123")).thenReturn(cRole);
        List<TenantRole> returnedRoles = defaultTenantService.getTenantRolesForTenant("id");
        assertThat("list size",returnedRoles.size(),equalTo(1));
        assertThat("clientId",returnedRoles.get(0).getClientId(),equalTo("456"));
        assertThat("description",returnedRoles.get(0).getDescription(),equalTo("this is a description"));
        assertThat("name", returnedRoles.get(0).getName(),equalTo("John Smith"));
        assertThat("role rs id", returnedRoles.get(0).getRoleRsId(),equalTo("789"));
        assertThat("tenant ids",returnedRoles.get(0).getTenantIds()[0],equalTo("id"));
    }

    @Test
    public void getUsersForTenant_rolesExistAndNoDuplicates_populatesUserIdList() throws Exception {
        TenantRole role = new TenantRole();
        role.setUserId("123");
        List<TenantRole> list = new ArrayList<TenantRole>();
        list.add(role);
        when(tenantDao.getAllTenantRolesForTenant("123")).thenReturn(list);
        defaultTenantService.getUsersForTenant("123");
        verify(userDao).getUserById("123");
    }

    @Test
    public void getUsersForTenant_rolesExistWithDuplicatesAndUserExistsAndEnabled_returnsListOfOneUser() throws Exception {
        TenantRole role1 = new TenantRole();
        role1.setUserId("123");
        TenantRole role2 = new TenantRole();
        role2.setUserId("123");
        TenantRole role3 = new TenantRole();
        role3.setUserId("123");
        List<TenantRole> list = new ArrayList<TenantRole>();
        list.add(role1);
        list.add(role2);
        list.add(role3);
        User user = new User();
        user.setEnabled(true);
        when(tenantDao.getAllTenantRolesForTenant("123")).thenReturn(list);
        when(userDao.getUserById("123")).thenReturn(user);
        assertThat("list size",defaultTenantService.getUsersForTenant("123").size(),equalTo(1));
    }

    @Test (expected = AssertionError.class)
    public void getUsersForTenant_noRolesAndUserExistsAndEnabled_doesNotCallUserDaoMethod() throws Exception {
        List<TenantRole> list = new ArrayList<TenantRole>();
        User user = new User();
        user.setEnabled(true);
        when(tenantDao.getAllTenantRolesForTenant("123")).thenReturn(list);
        defaultTenantService.getUsersForTenant("123");
        verify(userDao).getUserById("123");
    }

    @Test
    public void getUsersForTenant_userExistsAndNotEnabled_returnsEmptyList() throws Exception {
        TenantRole role1 = new TenantRole();
        role1.setUserId("123");
        TenantRole role2 = new TenantRole();
        role2.setUserId("123");
        TenantRole role3 = new TenantRole();
        role3.setUserId("123");
        List<TenantRole> list = new ArrayList<TenantRole>();
        list.add(role1);
        list.add(role2);
        list.add(role3);
        User user = new User();
        user.setEnabled(false);
        when(tenantDao.getAllTenantRolesForTenant("123")).thenReturn(list);
        when(userDao.getUserById("123")).thenReturn(user);
        assertThat("list size",defaultTenantService.getUsersForTenant("123").size(),equalTo(0));
    }

    @Test
    public void getUsersForTenant_userIsNull_returnsEmptyList() throws Exception {
        TenantRole role1 = new TenantRole();
        role1.setUserId("123");
        TenantRole role2 = new TenantRole();
        role2.setUserId("123");
        TenantRole role3 = new TenantRole();
        role3.setUserId("123");
        List<TenantRole> list = new ArrayList<TenantRole>();
        list.add(role1);
        list.add(role2);
        list.add(role3);
        when(tenantDao.getAllTenantRolesForTenant("123")).thenReturn(list);
        when(userDao.getUserById("123")).thenReturn(null);
        assertThat("list size",defaultTenantService.getUsersForTenant("123").size(),equalTo(0));
    }
}
