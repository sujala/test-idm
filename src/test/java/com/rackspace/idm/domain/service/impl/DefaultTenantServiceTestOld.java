package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 5/21/12
 * Time: 4:10 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTenantServiceTestOld {

    @InjectMocks
    DefaultTenantService defaultTenantService = new DefaultTenantService();
    @Mock
    private TenantDao tenantDao;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private ScopeAccessService scopeAccessService;
    @Mock
    private UserService userService;
    @Mock
    private EndpointService endpointService;
    @Mock
    private Configuration config;
    @Mock
    ScopeAccess scopeAccess;
    @Mock
    TenantRoleDao tenantRoleDao;

    DefaultTenantService spy;

    @Before
    public void setUp() throws Exception {
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
    public void checkAndGetTenant_TenantIsNull_throwsNotFoundException() throws Exception {
        try{
            doReturn(null).when(spy).getTenant("tenantId");
            spy.checkAndGetTenant("tenantId");
            assertTrue("should throw exception",false);
        } catch (NotFoundException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Tenant with id/name: 'tenantId' was not found."));
        }
    }

    @Test
    public void checkAndGetTenant_TenantIsNotNull_returnsTenant() throws Exception {
        Tenant tenant = new Tenant();
        doReturn(tenant).when(spy).getTenant("tenantId");
        assertThat("tenant", spy.checkAndGetTenant("tenantId"),equalTo(tenant));
    }

    @Test
    public void getTenantByName_callsTenantDaoMethod() throws Exception {
        defaultTenantService.getTenantByName(null);
        verify(tenantDao).getTenantByName(null);
    }

    @Test (expected = IllegalStateException.class)
    public void getTenantsForScopeAccessByTenantRoles_actionFails_throwsIllegalStateException() throws Exception {
        defaultTenantService.getTenantsForScopeAccessByTenantRoles(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRole_roleIsNull_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.addTenantRoleToClient(null, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteTenantRole_nullRole_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.deleteTenantRoleForUser(getUser(), null);
    }

    @Test
    public void deleteGlobalRole_callsTenantDaoMethod() throws Exception {
        defaultTenantService.deleteGlobalRole(null);
        verify(tenantRoleDao).deleteTenantRole(null);
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
        verify(tenantRoleDao).getTenantRolesForScopeAccess(scopeAccess);

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
        defaultTenantService.addTenantRoleToUser(user,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_userIdIsBlankAndRoleExists_throwsIllegalArgumentException() throws Exception {
        User user = new User();
        TenantRole tenantRole = new TenantRole();
        defaultTenantService.addTenantRoleToUser(user,tenantRole);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToUser_nullRole_throwsIllegalArgumentException() throws Exception {
        User user = new User();
        defaultTenantService.addTenantRoleToUser(user,null);
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
        defaultTenantService.addTenantRoleToClient(application,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToClient_clientIdIsBlankAndRoleExists_throwsIllegalArgumentException() throws Exception {
        TenantRole tenantRole = new TenantRole();
        Application application = new Application();
        defaultTenantService.addTenantRoleToClient(application,tenantRole);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToClient_clientIdIsNotBlankAndRoleIsNull_throwsIllegalArgumentException() throws Exception {
        Application application = new Application();
        defaultTenantService.addTenantRoleToClient(application,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getGlobalRolesForUser_nullUser_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.getGlobalRolesForUser(null);
    }

    @Test
    public void getGlobalRolesForUser_userParameter_returnsList() throws Exception {
        User user = new User();
        doReturn(new ArrayList<TenantRole>()).when(spy).getGlobalRoles(null);
        assertThat("tenant role list", spy.getGlobalRolesForUser(user), instanceOf(ArrayList.class));

    }

    @Test
    public void getGlobalRolesForUser_userAndFilterParameters_returnsList() throws Exception {
        doReturn(new ArrayList<TenantRole>()).when(spy).getGlobalRoles(null);
        assertThat("tenant role list", spy.getGlobalRolesForUser(null, null), instanceOf(ArrayList.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getGlobalRolesForApplication_nullApplication_throwsIllegalArgumentException() throws Exception {
        defaultTenantService.getGlobalRolesForApplication(null, null);
    }

    @Test
    public void getGlobalRolesForApplication_returnsList() throws Exception {
        doReturn(new ArrayList<TenantRole>()).when(spy).getGlobalRoles(null);
        assertThat("tenant role list", spy.getGlobalRolesForApplication(new Application(), null), instanceOf(ArrayList.class));
    }

    @Test
    public void getGlobalRoles_roleTenantIdsIsNull_returnsCorrectRole() throws Exception {
        TenantRole role = new TenantRole();
        role.setRoleRsId("123");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        ClientRole cRole = new ClientRole();
        cRole.setName("John Smith");
        cRole.setDescription("this is a description");
        when(applicationService.getClientRoleById("123")).thenReturn(cRole);
        List<TenantRole> list = defaultTenantService.getGlobalRoles(roles);
        assertThat("role name", list.get(0).getName(), equalTo("John Smith"));
        assertThat("role description",list.get(0).getDescription(),equalTo("this is a description"));
    }

    @Test
    public void getGlobalRoles_roleNull_returnsEmptyList() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        List<TenantRole> list = defaultTenantService.getGlobalRoles(roles);
        assertThat("list size",list.size(),equalTo(0));
    }

    @Test
    public void getTenantOnlyRoles_tenantIdsNull_returnsEmptyList() throws Exception {
        TenantRole role = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        List<TenantRole> tenantRoles = defaultTenantService.getTenantOnlyRoles(roles);
        assertThat("number of tenant roles",tenantRoles.size(),equalTo(0));
    }

    @Test
    public void getTenantOnlyRoles_roleListEmpty_returnsEmptyList() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        List<TenantRole> tenantRoles = defaultTenantService.getTenantOnlyRoles(roles);
        assertThat("number of tenant roles",tenantRoles.size(),equalTo(0));
    }

    @Test
    public void getTenantRolesByParent_roleIsNull_doesNotCallClientDao() throws Exception {
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRoleList.add(null);
        when(tenantRoleDao.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoleList);
        defaultTenantService.getTenantRolesForUser(getUser());
        verify(applicationService, times(0)).getClientRoleById(anyString());
    }

    @Test
    public void isTenantIdContainedInTenantRoles_rolesIsNull_returnsFalse() throws Exception {
        assertThat("boolean",defaultTenantService.isTenantIdContainedInTenantRoles("tenantId",null),equalTo(false));
    }

    @Test
    public void isTenantIdContainedInTenantRoles_rolesSizeIsZero_returnsFalse() throws Exception {
        assertThat("boolean",defaultTenantService.isTenantIdContainedInTenantRoles("tenantId",new ArrayList<TenantRole>()),equalTo(false));
    }

    public ScopeAccess getScopeAccess() {
        Entry entry = new Entry("id=1234,ou=here,o=path,dc=blah");
        ReadOnlyEntry readOnlyEntry = new ReadOnlyEntry(entry);
        when(scopeAccess.getLDAPEntry()).thenReturn(readOnlyEntry);
        return scopeAccess;
    }

    public User getUser() {
        User user = new User();
        return user;
    }
}
