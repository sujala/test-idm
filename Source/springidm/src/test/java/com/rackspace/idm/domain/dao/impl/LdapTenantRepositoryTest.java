package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/29/12
 * Time: 8:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class LdapTenantRepositoryTest extends InMemoryLdapIntegrationTest{
    LdapTenantRepository ldapTenantRepository;
    LdapTenantRepository spy;
    LDAPInterface ldapInterface;

    @Before
    public void setUp() throws Exception {
        ldapTenantRepository = new LdapTenantRepository(mock(LdapConnectionPools.class), mock(Configuration.class));
        ldapInterface = mock(LDAPInterface.class);
        spy = spy(ldapTenantRepository);

        doReturn(ldapInterface).when(spy).getAppInterface();
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenant_tenantIsNull_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.addTenant(null);
    }

    @Test (expected = IllegalStateException.class)
    public void addTenant_callsLDAPPersister_throwsIllegalStateException() throws Exception {
        spy.addTenant(new Tenant());
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteTenant_tenantIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.deleteTenant("");
    }

    @Test (expected = NotFoundException.class)
    public void deleteTenant_tenantNotFound_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getTenant("tenantId");
        spy.deleteTenant("tenantId");
    }

    @Test
    public void deleteTenant_callsDeleteEntryAndSubtree() throws Exception {
        Tenant tenant = new Tenant();
        doReturn(tenant).when(spy).getTenant("tenantId");
        doNothing().when(spy).deleteEntryAndSubtree(anyString(), any(Audit.class));
        spy.deleteTenant("tenantId");
        verify(spy).deleteEntryAndSubtree(anyString(), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getTenant_tenantIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getTenant("");
    }

    @Test (expected = IllegalStateException.class)
    public void getTenant_getSingleTenant_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getSingleTenant(any(Filter.class));
        spy.getTenant("tenantId");
    }

    @Test
    public void getTenant_foundTenant_returnsTenant() throws Exception {
        Tenant tenant = new Tenant();
        doReturn(tenant).when(spy).getSingleTenant(any(Filter.class));
        Tenant result = spy.getTenant("tenantId");
        assertThat("tenant", result, equalTo(tenant));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getTenantByName_nameIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getTenantByName("  ");
    }

    @Test (expected = IllegalStateException.class)
    public void getTenantByName_callsGetSingleTenant_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getSingleTenant(any(Filter.class));
        spy.getTenantByName("name");
    }

    @Test
    public void getTenantByName_foundTenant_returnsTenant() throws Exception {
        Tenant tenant = new Tenant();
        doReturn(tenant).when(spy).getSingleTenant(any(Filter.class));
        Tenant result = spy.getTenantByName("name");
        assertThat("tenant", result, equalTo(tenant));
    }

    @Test (expected = IllegalStateException.class)
    public void getTenants_callsGetMultipleTenants_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleTenants(any(Filter.class));
        spy.getTenants();
    }

    @Test
    public void getTenants_foundTenants_returnsTenantList() throws Exception {
        List<Tenant> tenantList = new ArrayList<Tenant>();
        doReturn(tenantList).when(spy).getMultipleTenants(any(Filter.class));
        List<Tenant> result = spy.getTenants();
        assertThat("tenant list", result, equalTo(tenantList));
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateTenant_tenantIsNull_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.updateTenant(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateTenant_tenantUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.updateTenant(new Tenant());
    }

    @Test (expected = IllegalStateException.class)
    public void updateTenant_callsLDAPPersister_throwsIllegalStateException() throws Exception {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getUniqueId()).thenReturn("uniqueId");
        spy.updateTenant(tenant);
    }

    @Test
    public void getMultipleTenants_foundTenants_returnsTenantList() throws Exception {
        Tenant tenant = new Tenant();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(LdapRepository.TENANT_BASE_DN, SearchScope.ONE, LdapRepository.ATTR_ID, null, LdapRepository.ATTR_TENANT_SEARCH_ATTRIBUTES);
        doReturn(tenant).when(spy).getTenant(searchResultEntry);
        List<Tenant> result = spy.getMultipleTenants(null);
        assertThat("tenant", result.get(0), equalTo(tenant));
    }

    @Test
    public void getMultipleTenants_tenantsNotFound_returnsEmptyList() throws Exception {
        List<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        doReturn(entries).when(spy).getMultipleEntries(LdapRepository.TENANT_BASE_DN, SearchScope.ONE, LdapRepository.ATTR_ID, null, LdapRepository.ATTR_TENANT_SEARCH_ATTRIBUTES);
        List<Tenant> result = spy.getMultipleTenants(null);
        assertThat("tenant", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getSingleTenant_foundTenant_returnsTenant() throws Exception {
        Tenant tenant = new Tenant();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(LdapRepository.TENANT_BASE_DN, SearchScope.ONE, null, LdapRepository.ATTR_TENANT_SEARCH_ATTRIBUTES);
        doReturn(tenant).when(spy).getTenant(searchResultEntry);
        Tenant result = spy.getSingleTenant(null);
        assertThat("tenant", result, equalTo(tenant));
    }

    @Test
    public void getTenant_entryIsNull_returnsNull() throws Exception {
        Tenant result = ldapTenantRepository.getTenant((SearchResultEntry) null);
        assertThat("tenant", result, equalTo(null));
    }

    @Test
    public void getTenant_decodeEntry_returnsTenant() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        Tenant result = ldapTenantRepository.getTenant(searchResultEntry);
        assertThat("tenant unique id", result.getUniqueId(), equalTo("uniqueId"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToParent_parentUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.addTenantRoleToParent("   ", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenantRoleToParent_tenantRoleIsNull_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.addTenantRoleToParent("uniqueId", null);
    }

    @Test (expected = IllegalStateException.class)
    public void addTenantRoleToParent_callsLDAPPersister_throwsIllegalStateException() throws Exception {
        spy.addTenantRoleToParent("uniqueId", new TenantRole());
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteTenantRole_roleIsNull_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.deleteTenantRole(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteTenantRole_roleUniqueIdIsNull_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.deleteTenantRole(new TenantRole());
    }

    @Test
    public void deleteTenantRole_callsDeleteEntrySubtree() throws Exception {
        TenantRole role = mock(TenantRole.class);
        when(role.getUniqueId()).thenReturn("uniqueId");
        doNothing().when(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
        spy.deleteTenantRole(role);
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getTenantRoleForParentById_parentUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getTenantRoleForParentById("   ", "id");
    }

    @Test (expected = IllegalArgumentException.class)
    public void getTenantRoleForParentById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getTenantRoleForParentById("uniqueId", "");
    }

    @Test (expected = IllegalStateException.class)
    public void getTenantRoleForParentById_callsGetSingleTenantRole_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getSingleTenantRole(anyString(), any(Filter.class));
        spy.getTenantRoleForParentById("uniqueId", "id");
    }

    @Test
    public void getTenantRoleForParentById_foundTenantRole_returnsRole() throws Exception {
        TenantRole tenantRole = new TenantRole();
        doReturn(tenantRole).when(spy).getSingleTenantRole(anyString(), any(Filter.class));
        TenantRole result = spy.getTenantRoleForParentById("uniqueId", "id");
        assertThat("tenant role", result, equalTo(tenantRole));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getTenantRolesByParent_parentUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getTenantRolesByParent("  ");
    }

    @Test (expected = IllegalStateException.class)
    public void getTenantRolesByParent_callsGetMultipleTenantRoles_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        spy.getTenantRolesByParent("uniqueId");
    }

    @Test
    public void getTenantRolesByParent_foundTenantRoles_returnsRoleList() throws Exception {
        TenantRole tenantRole = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(tenantRole);
        doReturn(roles).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        List<TenantRole> result = spy.getTenantRolesByParent("uniqueId");
        assertThat("tenant role", result.get(0), equalTo(tenantRole));
    }

    @Test (expected = IllegalStateException.class)
    public void getTenantRolesForUser_callsGetMultipleTenantRoles_throwsLDAPPersistException() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        spy.getTenantRolesForUser(user);
    }

    @Test
    public void getTenantRolesForUser_foundRoles_returnRoleList() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        TenantRole tenatnRole = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(tenatnRole);
        doReturn(roles).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        List<TenantRole> result = spy.getTenantRolesForUser(user);
        assertThat("tenant role", result.get(0), equalTo(tenatnRole));
    }

    @Test
    public void getTenantRolesForUser_callsGetTenantRolesForClient() throws Exception {
        User user = new User();
        user.setUniqueId("uniqueId");
        doReturn(null).when(spy).getTenantRolesForClient("uniqueId", null);
        spy.getTenantRolesForUser(user, null);
        verify(spy).getTenantRolesForClient("uniqueId", null);
    }

    @Test
    public void getTenantRolesForApplication_callsGetTenantRolesForClient() throws Exception {
        Application application = new Application();
        application.setUniqueId("uniqueId");
        doReturn(null).when(spy).getTenantRolesForClient("uniqueId", null);
        spy.getTenantRolesForApplication(application, null);
        verify(spy).getTenantRolesForClient("uniqueId", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getTenantRolesByParentAndClientId_parentUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getTenantRolesByParentAndClientId(" ", "clientId");
    }

    @Test (expected = IllegalArgumentException.class)
    public void getTenantRolesByParentAndClientId_clientIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getTenantRolesByParentAndClientId("uniqueId", null);
    }

    @Test (expected = IllegalStateException.class)
    public void getTenantRolesByParentAndClientId_callsGetMultipleTenantRoles_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleTenantRoles(eq("uniqueId"), any(Filter.class));
        spy.getTenantRolesByParentAndClientId("uniqueId", "clientId");
    }

    @Test
    public void getTenantRolesByParentAndClientId_foundRoles_returnsRoleList() throws Exception {
        TenantRole tenantRole = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(tenantRole);
        doReturn(roles).when(spy).getMultipleTenantRoles(eq("uniqueId"), any(Filter.class));
        List<TenantRole> result = spy.getTenantRolesByParentAndClientId("uniqueId", "clientId");
        assertThat("tenant role", result.get(0), equalTo(tenantRole));
    }

    @Test
    public void getTenantRolesForClient_filterParamNotNull_addsAttributes() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        FilterParam filterParam = new FilterParam(FilterParam.FilterParamName.APPLICATION_ID, "applicationId");
        FilterParam filterParam1 = new FilterParam(FilterParam.FilterParamName.TENANT_ID, "tenantId");
        FilterParam filterParam2 = new FilterParam(FilterParam.FilterParamName.APPLICATION_NAME, "applicationName");
        FilterParam[] filterParams = new FilterParam[3];
        filterParams[0] = filterParam;
        filterParams[1] = filterParam1;
        filterParams[2] = filterParam2;
        doReturn(new ArrayList<TenantRole>()).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        spy.getTenantRolesForClient("uniqueClientId", filterParams);
        verify(spy).getMultipleTenantRoles(anyString(), argumentCaptor.capture());
        Filter[] result = argumentCaptor.getValue().getComponents();
        assertThat("application id", result[1].getAssertionValue(), equalTo("applicationId"));
        assertThat("tenant id", result[2].getAssertionValue(), equalTo("tenantId"));
        assertThat("filter size", result.length, equalTo(3));
    }

    @Test (expected = IllegalStateException.class)
    public void getTenantRolesForClient_callsGetMultipleTenantRoles_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        spy.getTenantRolesForClient("uniqueId", new FilterParam[0]);
    }

    @Test
    public void getTenantRolesForClient_gotRoles_returnRoleList() throws Exception {
        TenantRole tenantRole = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(tenantRole);
        doReturn(roles).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        List<TenantRole> result = spy.getTenantRolesForClient("uniqueId", null);
        assertThat("tenant role", result.get(0), equalTo(tenantRole));
    }

    @Test
    public void getMultipleTenantRoles_foundTenantRoles_returnsRoleList() throws Exception {
        TenantRole tenantRole = new TenantRole();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        doReturn(tenantRole).when(spy).getTenantRole(searchResultEntry);
        List<TenantRole> result = spy.getMultipleTenantRoles("uniqueId", null);
        assertThat("tenant role", result.get(0), equalTo(tenantRole));
    }

    @Test
    public void getMultipleTenantRoles_tenantRoleNotFound_returnsEmptyList() throws Exception {
        List<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        List<TenantRole> result = spy.getMultipleTenantRoles("uniqueId", null);
        assertThat("tenant role", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getSingleTenantRole_callsGetTenantRole_returnsRole() throws Exception {
        TenantRole role = new TenantRole();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(role).when(spy).getTenantRole(searchResultEntry);
        TenantRole result = spy.getSingleTenantRole("uniqueId", null);
        assertThat("tenant role", result, equalTo(role));
    }

    @Test
    public void getTenantRole_entryIsNull_returnsNull() throws Exception {
        TenantRole result = ldapTenantRepository.getTenantRole(null);
        assertThat("tenant role", result, equalTo(null));
    }

    @Test
    public void getTenantRole_entryNotNull_returnsTenantRole() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        TenantRole result = spy.getTenantRole(searchResultEntry);
        assertThat("tenant role uniqueId", result.getUniqueId(), equalTo("uniqueId"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateTenantRole_roleIsNull_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.updateTenantRole(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateTenantRole_roleUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.updateTenantRole(new TenantRole());
    }

    @Test (expected = IllegalStateException.class)
    public void updateTenantRole_callsLDAPPersisterModify_throwsIllegalStateException() throws Exception {
        TenantRole role = mock(TenantRole.class);
        when(role.getUniqueId()).thenReturn("uniqueId");
        spy.updateTenantRole(role);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getAllTenantRolesForTenant_tenantIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getAllTenantRolesForTenant(" ");
    }

    @Test (expected = IllegalStateException.class)
    public void getAllTenantRolesForTenant_callsGetMultipleTenantRoles_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        spy.getAllTenantRolesForTenant("tenantId");
    }

    @Test
    public void getAllTenantRolesForTenant_foundRoles_returnsTenantRoleList() throws Exception {
        TenantRole tenantRole = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(tenantRole);
        doReturn(roles).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        List<TenantRole> result = spy.getAllTenantRolesForTenant("tenantId");
        assertThat("tenant role", result.get(0), equalTo(tenantRole));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getAllTenantRolesForClientRole_clientRoleIsNull_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getAllTenantRolesForClientRole(null);
    }

    @Test (expected = IllegalStateException.class)
    public void getAllTenantRolesForClientRole_callsGetMultipleTenantRoles_throwsLDAPPersistException() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setId("id");
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        spy.getAllTenantRolesForClientRole(clientRole);
    }

    @Test
    public void getAllTenantRolesForClientRole_foundRole_returnsTenantRoleList() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setId("id");
        TenantRole tenantRole = new TenantRole();
        List<TenantRole> roleList = new ArrayList<TenantRole>();
        roleList.add(tenantRole);
        doReturn(roleList).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        List<TenantRole> result = spy.getAllTenantRolesForClientRole(clientRole);
        assertThat("tenant role", result.get(0), equalTo(tenantRole));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getAllTenantRolesForTenantAndRoles_tenantIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getAllTenantRolesForTenantAndRole(" ", "roleId");
    }

    @Test (expected = IllegalArgumentException.class)
    public void getAllTenantRolesForTenantAndRoles_roleIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.getAllTenantRolesForTenantAndRole("tenantId", "   ");
    }

    @Test (expected = IllegalStateException.class)
    public void getAllTenantRolesForTenantAndRoles_callsGetMultipleTenantRoles_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        spy.getAllTenantRolesForTenantAndRole("tenantId", "roleId");
    }

    @Test
    public void getAllTenantRolesForTenantAndRoles_foundTenantRoles_returnTenantRoleList() throws Exception {
        TenantRole tenantRole = new TenantRole();
        List<TenantRole> roleList = new ArrayList<TenantRole>();
        roleList.add(tenantRole);
        doReturn(roleList).when(spy).getMultipleTenantRoles(anyString(), any(Filter.class));
        List<TenantRole> result = spy.getAllTenantRolesForTenantAndRole("tenantId", "roleId");
        assertThat("tenant role", result.get(0), equalTo(tenantRole));
    }

    @Test (expected = IllegalStateException.class)
    public void doesScopeAccessHaveTenantRole_callsScopeAccessGetLdapEntry_throwsIllegalStateException() throws Exception {
        RackerScopeAccess scopeAccess = mock(RackerScopeAccess.class);
        doThrow(new IllegalStateException()).when(scopeAccess).getLDAPEntry();
        spy.doesScopeAccessHaveTenantRole(scopeAccess, null);
    }

    @Test
    public void doesScopeAccessHaveTenantRole_tenantRoleExists_returnsTrue() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setId("roleId");
        DelegatedClientScopeAccess scopeAccess = mock(DelegatedClientScopeAccess.class);
        when(scopeAccess.getUniqueId()).thenReturn("uniqueId");
        doReturn(new TenantRole()).when(spy).getTenantRoleForParentById("uniqueId", "roleId");
        boolean result = spy.doesScopeAccessHaveTenantRole(scopeAccess, clientRole);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void doesScopeAccessHaveTenantRole_tenantRoleDoesNotExist_returnsFalse() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setId("roleId");
        DelegatedClientScopeAccess scopeAccess = mock(DelegatedClientScopeAccess.class);
        when(scopeAccess.getUniqueId()).thenReturn("uniqueId");
        doReturn(null).when(spy).getTenantRoleForParentById("uniqueId", "roleId");
        boolean result = spy.doesScopeAccessHaveTenantRole(scopeAccess, clientRole);
        assertThat("boolean", result, equalTo(false));
    }
}
