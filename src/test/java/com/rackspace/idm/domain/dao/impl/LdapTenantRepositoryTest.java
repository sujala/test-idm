package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.api.resource.pagination.DefaultPaginator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import org.apache.commons.lang.StringUtils;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

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
@RunWith(MockitoJUnitRunner.class)
public class LdapTenantRepositoryTest extends InMemoryLdapIntegrationTest{
    @InjectMocks
    LdapTenantRepository ldapTenantRepository = new LdapTenantRepository();
    @Mock
    LdapConnectionPools ldapConnectionPools;
    @Mock
    Configuration configuration;
    @Mock
    DefaultPaginator<String> stringPaginator;

    LdapTenantRepository spy;
    LDAPInterface ldapInterface;

    @Before
    public void setUp() throws Exception {
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

    @Test
    public void getTenant_tenantIdIsBlank_returnsNull() throws Exception {
        Tenant result = ldapTenantRepository.getTenant("");
        assertThat("tenant", result, equalTo(null));
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

    @Test
    public void getTenantByName_nameIsBlank_returnsNull() throws Exception {
        Tenant result = ldapTenantRepository.getTenantByName("  ");
        assertThat("tenant", result, equalTo(null));
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

    private User getUser() {
        User user = new User();
        user.setUniqueId("id");
        return user;
    }

    private Application getApplication() {
        Application application = new Application();
        return application;
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

    @Test
    public void getAllTenantRolesForTenant_tenantIdIsBlank_returnsEmptyList() throws Exception {
        List<TenantRole> result = ldapTenantRepository.getAllTenantRolesForTenant(" ");
        assertThat("list", result.isEmpty(), equalTo(true));
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

    @Test
    public void getAllTenantRolesForClientRole_clientRoleIsNull_returnsEmptyList() throws Exception {
        List<TenantRole> result = ldapTenantRepository.getAllTenantRolesForClientRole(null);
        assertThat("list", result.isEmpty(), equalTo(true));
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

    @Test
    public void getAllTenantRolesForTenantAndRoles_tenantIdIsBlank_returnsEmptyList() throws Exception {
        List<TenantRole> result = ldapTenantRepository.getAllTenantRolesForTenantAndRole(" ", "roleId");
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getAllTenantRolesForTenantAndRoles_roleIdIsBlank_returnsEmptyList() throws Exception {
        List<TenantRole> result = ldapTenantRepository.getAllTenantRolesForTenantAndRole("tenantId", "   ");
        assertThat("list", result.isEmpty(), equalTo(true));
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
    public void getMultipleTenantRoles_callsPaginator_createSearchRequest() throws Exception {
        PaginatorContext<String> context = new PaginatorContext<String>();
        String[] referalURLs = {"123"};
        Attribute attribute = new Attribute("o", "rackspace");
        Attribute[] attributes = {attribute};
        Control control = new Control("oid");
        Control[] controls = {control};
        List<SearchResultEntry> searchResultEntries = new ArrayList<SearchResultEntry>();
        List<SearchResultReference> searchResultReferences = new ArrayList<SearchResultReference>();
        SearchResultEntry entry = new SearchResultEntry("", attributes, control);
        SearchResultReference reference = new SearchResultReference(referalURLs, controls);
        searchResultEntries.add(entry);
        searchResultReferences.add(reference);

        SearchResult searchResult = new SearchResult(0, ResultCode.SUCCESS, "", "", referalURLs,
                searchResultEntries, searchResultReferences, 1, 1, null);
        doReturn(context).when(stringPaginator).createSearchRequest(anyString(), any(SearchRequest.class), anyInt(), anyInt());
        doReturn(searchResult).when(spy).getMultipleEntries(any(SearchRequest.class));

        spy.getIdsForUsersWithTenantRole("1", 0, 10);
        verify(stringPaginator).createSearchRequest(anyString(), any(SearchRequest.class), anyInt(), anyInt());
    }

    @Test
    public void getMultipleTenantRoles_callsGetMultipleEntries() throws Exception {
        PaginatorContext<String> context = new PaginatorContext<String>();
        String[] referalURLs = {"123"};
        Attribute attribute = new Attribute("attribute");
        Attribute[] attributes = {attribute};
        Control control = new Control("oid");
        Control[] controls = {control};
        List<SearchResultEntry> searchResultEntries = new ArrayList<SearchResultEntry>();
        List<SearchResultReference> searchResultReferences = new ArrayList<SearchResultReference>();
        SearchResultEntry entry = new SearchResultEntry("", attributes, control);
        SearchResultReference reference = new SearchResultReference(referalURLs, controls);
        searchResultEntries.add(entry);
        searchResultReferences.add(reference);

        SearchResult searchResult = new SearchResult(0, ResultCode.SUCCESS, "", "", referalURLs,
                searchResultEntries, searchResultReferences, 1, 1, null);
        doReturn(searchResult).when(spy).getMultipleEntries(any(SearchRequest.class));
        doReturn(context).when(stringPaginator).createSearchRequest(anyString(), any(SearchRequest.class), anyInt(), anyInt());

        spy.getIdsForUsersWithTenantRole("1", 0, 10);
        verify(spy).getMultipleEntries(any(SearchRequest.class));
    }

    @Test
    public void getMultipleTenantRoles_returnsContext() throws Exception {
        PaginatorContext<String> context = new PaginatorContext<String>();
        String[] referalURLs = {"123"};
        Attribute attribute = new Attribute("attribute");
        Attribute[] attributes = {attribute};
        Control control = new Control("oid");
        Control[] controls = {control};
        List<SearchResultEntry> searchResultEntries = new ArrayList<SearchResultEntry>();
        List<SearchResultReference> searchResultReferences = new ArrayList<SearchResultReference>();
        SearchResultEntry entry = new SearchResultEntry("", attributes, control);
        SearchResultReference reference = new SearchResultReference(referalURLs, controls);
        searchResultEntries.add(entry);
        searchResultReferences.add(reference);

        SearchResult searchResult = new SearchResult(0, ResultCode.SUCCESS, "", "", referalURLs,
                searchResultEntries, searchResultReferences, 1, 1, null);

        doReturn(searchResult).when(spy).getMultipleEntries(any(SearchRequest.class));
        doReturn(context).when(stringPaginator).createSearchRequest(anyString(), any(SearchRequest.class), anyInt(), anyInt());
        doReturn("123456").when(spy).getUserIdFromDN(any(DN.class));

        PaginatorContext<String> userIdPaginatorContext = spy.getIdsForUsersWithTenantRole("1", 0, 10);

        assertThat("valueList", userIdPaginatorContext.getValueList().size(), equalTo(1));
    }

    @Test
    public void getUserIdFromRDN_returnsUserId() throws Exception {
        DN rootDN = createDN(true);

        String userId = spy.getUserIdFromDN(rootDN);
        assertThat("userId match", userId.equalsIgnoreCase("123456789"));
    }

    @Test
    public void getUserIdFromRDN_rsIdNotInDN_returnsEmptyString() throws Exception {
        DN rootDN = createDN(false);

        String userId = spy.getUserIdFromDN(rootDN);
        assertThat("userId is blank", StringUtils.isBlank(userId));
    }

    @Test
    public void getUserDnFromScopeAccess_returnsUserDn() throws Exception {
        DN dn = createDN(true);
        DN compareToDn = new DN(new RDN("rsId", "123456789"), new RDN("ou", "users"), new RDN("o", "rackspace"), new RDN("dc", "rackspace"));

        DN userDn = spy.getBaseDnForSearch(dn);

        assertThat("userDn is correct", userDn.toString().equalsIgnoreCase(compareToDn.toString()));
    }

    protected DN createDN(boolean withRsId) {
        RDN rdn = new RDN("accessToken", "abcd12345");
        RDN rdn1 = new RDN("cn", "TOKENS");
        RDN rdn2 = new RDN("rsId", "123456789");
        RDN rdn3 = new RDN("ou", "users");
        RDN rdn4 = new RDN("o", "rackspace");
        RDN rdn5 = new RDN("dc", "rackspace");

        if (withRsId) {
            return new DN(rdn, rdn1, rdn2, rdn3, rdn4, rdn5);
        } else {
            return new DN(rdn, rdn1, rdn3, rdn4, rdn5);
        }
    }
}
