package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
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
public class LdapTenantRepositoryTest {
    LdapTenantRepository ldapTenantRepository;
    LdapTenantRepository spy;

    @Before
    public void setUp() throws Exception {
        ldapTenantRepository = new LdapTenantRepository(mock(LdapConnectionPools.class), mock(Configuration.class));

        spy = spy(ldapTenantRepository);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addTenant_tenantIsNull_throwsIllegalArgument() throws Exception {
        ldapTenantRepository.addTenant(null);
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

    @Test
    public void getMultipleTenants_foundTenants_returnsTenantList() throws Exception {
        Tenant tenant = new Tenant();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        entries.add(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(LdapRepository.TENANT_BASE_DN, SearchScope.ONE, null, LdapRepository.ATTR_ID, LdapRepository.ATTR_TENANT_SEARCH_ATTRIBUTES);
        doReturn(tenant).when(spy).getTenant(searchResultEntry);
        List<Tenant> result = spy.getMultipleTenants(null);
        assertThat("tenant", result.get(0), equalTo(tenant));
    }

    @Test
    public void getMultipleTenants_tenantsNotFound_returnsEmptyList() throws Exception {
        List<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        doReturn(entries).when(spy).getMultipleEntries(LdapRepository.TENANT_BASE_DN, SearchScope.ONE, null, LdapRepository.ATTR_ID, LdapRepository.ATTR_TENANT_SEARCH_ATTRIBUTES);
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
}
