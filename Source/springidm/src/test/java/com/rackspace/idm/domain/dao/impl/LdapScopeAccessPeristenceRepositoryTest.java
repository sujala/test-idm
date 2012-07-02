package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.naming.directory.SearchResult;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 5:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapScopeAccessPeristenceRepositoryTest {

    Configuration config = mock(Configuration.class);
    LdapConnectionPools ldapConnectionPools = mock(LdapConnectionPools.class);
    LdapScopeAccessPeristenceRepository ldapScopeAccessPeristenceRepository;
    LdapScopeAccessPeristenceRepository spy;

    @Before
    public void setUp() throws Exception {
        ldapScopeAccessPeristenceRepository = new LdapScopeAccessPeristenceRepository(ldapConnectionPools,config);
        spy = spy(ldapScopeAccessPeristenceRepository);
    }

    @Test
    public void addDelegateScopeAccess_callsGetContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getContainer(null, "DELEGATE TOKENS");
        doNothing().when(spy).addContainer(null,"DELEGATE TOKENS");
        doReturn(new ScopeAccess()).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDelegateScopeAccess(null,scopeAccess);
        verify(spy).getContainer(null, "DELEGATE TOKENS");
    }

    @Test
    public void addDelegateScopeAccess_entryNotNull_doesNotCallAddContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getContainer(null, "DELEGATE TOKENS");
        doNothing().when(spy).addContainer(null,"DELEGATE TOKENS");
        doReturn(new ScopeAccess()).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDelegateScopeAccess(null,scopeAccess);
        verify(spy,never()).addContainer(anyString(), anyString());
    }

    @Test
    public void addDelegateScopeAccess_entryNull_callsAddContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer(null, "DELEGATE TOKENS");
        doNothing().when(spy).addContainer(null,"DELEGATE TOKENS");
        doReturn(new ScopeAccess()).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDelegateScopeAccess(null,scopeAccess);
        verify(spy).addContainer(null, "DELEGATE TOKENS");
    }

    @Test
    public void addDelegateScopeAccess_entryNull_callsGetContainerTwice() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer(null, "DELEGATE TOKENS");
        doNothing().when(spy).addContainer(null,"DELEGATE TOKENS");
        doReturn(new ScopeAccess()).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDelegateScopeAccess(null,scopeAccess);
        verify(spy,times(2)).getContainer(null, "DELEGATE TOKENS");
    }

    @Test
    public void addDelegateScopeAccess_callsAddScopeAccessAndReturnsScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer(null, "DELEGATE TOKENS");
        doNothing().when(spy).addContainer(null,"DELEGATE TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        assertThat("scope access",spy.addDelegateScopeAccess(null, scopeAccess),equalTo(scopeAccess2));
    }

    @Test
    public void addDelegateScopeAccess_getContainerFails_throwsIllegalStateException() throws Exception {

        try{
            ScopeAccess scopeAccess = new ScopeAccess();
            doThrow(new IllegalStateException()).when(spy).getContainer(null, "DELEGATE TOKENS");
            spy.addDelegateScopeAccess(null, scopeAccess);
            assertTrue("should throw exception",false);
        }catch (IllegalStateException ex){
            assertThat("exception message",ex.getMessage(),equalTo("java.lang.IllegalStateException"));
        }
    }

    @Test
    public void addImpersonatedScopeAccess_callsGetContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getContainer("uniqueId","IMPERSONATED TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addImpersonatedScopeAccess("uniqueId",scopeAccess);
        verify(spy).getContainer("uniqueId", "IMPERSONATED TOKENS");
    }

    @Test
    public void addImpersonatedScopeAccess_entryNotNull_doesNotCallAddContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getContainer("uniqueId","IMPERSONATED TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addImpersonatedScopeAccess("uniqueId",scopeAccess);
        verify(spy,never()).addContainer("uniqueId", "IMPERSONATED TOKENS");
    }

    @Test
    public void addImpersonatedScopeAccess_nullEntry_callsAddContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer("uniqueId","IMPERSONATED TOKENS");
        doNothing().when(spy).addContainer("uniqueId", "IMPERSONATED TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addImpersonatedScopeAccess("uniqueId",scopeAccess);
        verify(spy).addContainer("uniqueId","IMPERSONATED TOKENS");
    }

    @Test
    public void addImpersonatedScopeAccess_nullEntry_callsGetContainerTwice() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer("uniqueId","IMPERSONATED TOKENS");
        doNothing().when(spy).addContainer("uniqueId", "IMPERSONATED TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addImpersonatedScopeAccess("uniqueId",scopeAccess);
        verify(spy,times(2)).getContainer("uniqueId", "IMPERSONATED TOKENS");
    }

    @Test
    public void addImpersonatedScopeAccess_callsAddScopeAccessAndReturnsScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer("uniqueId","IMPERSONATED TOKENS");
        doNothing().when(spy).addContainer("uniqueId", "IMPERSONATED TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        assertThat("returns scope access",spy.addImpersonatedScopeAccess("uniqueId",scopeAccess),equalTo(scopeAccess2));
    }

    @Test
    public void addImpersonatedScopeAccess_getContainerFails_throwsIllegalStateException() throws Exception {
        try{
            ScopeAccess scopeAccess = new ScopeAccess();
            doThrow(new IllegalStateException()).when(spy).getContainer("uniqueId","IMPERSONATED TOKENS");
            spy.addImpersonatedScopeAccess("uniqueId",scopeAccess);
            assertTrue("should throw exception",false);
        } catch (IllegalStateException ex){
            assertThat("exception message",ex.getMessage(),equalTo("java.lang.IllegalStateException"));
        }
    }

    @Test
    public void addDirectScopeAccess_callsGetContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getContainer("uniqueId","DIRECT TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDirectScopeAccess("uniqueId",scopeAccess);
        verify(spy).getContainer("uniqueId", "DIRECT TOKENS");
    }

    @Test
    public void addDirectScopeAccess_entryNotNull_doesNotCallAddContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getContainer("uniqueId","DIRECT TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDirectScopeAccess("uniqueId", scopeAccess);
        verify(spy,never()).addContainer("uniqueId", "DIRECT TOKENS");
    }

    @Test
    public void addDirectScopeAccess_nullEntry_callsAddContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer("uniqueId","DIRECT TOKENS");
        doNothing().when(spy).addContainer("uniqueId", "DIRECT TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDirectScopeAccess("uniqueId", scopeAccess);
        verify(spy).addContainer("uniqueId","DIRECT TOKENS");
    }

    @Test
    public void addDirectScopeAccess_nullEntry_callsGetContainerTwice() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer("uniqueId","DIRECT TOKENS");
        doNothing().when(spy).addContainer("uniqueId", "DIRECT TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDirectScopeAccess("uniqueId", scopeAccess);
        verify(spy,times(2)).getContainer("uniqueId", "DIRECT TOKENS");
    }

    @Test
    public void addDirectScopeAccess_callsAddScopeAccessAndReturnsScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer("uniqueId","DIRECT TOKENS");
        doNothing().when(spy).addContainer("uniqueId", "DIRECT TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        assertThat("returns scope access",spy.addDirectScopeAccess("uniqueId", scopeAccess),equalTo(scopeAccess2));
    }

    @Test
    public void addDirectScopeAccess_getContainerFails_throwsIllegalStateException() throws Exception {
        try{
            ScopeAccess scopeAccess = new ScopeAccess();
            doThrow(new IllegalStateException()).when(spy).getContainer("uniqueId","DIRECT TOKENS");
            spy.addDirectScopeAccess("uniqueId", scopeAccess);
            assertTrue("should throw exception",false);
        } catch (IllegalStateException ex){
            assertThat("exception message",ex.getMessage(),equalTo("java.lang.IllegalStateException"));
        }
    }

    @Test
    public void definePermission_runsSuccessfully() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setPermissionId("cn=uniqueId,dc=example");
        definedPermission.setClientId("clientId");
        definedPermission.setCustomerId("customerId");
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        assertThat("runs and returns null", spy.definePermission("cn=uniqueId", definedPermission), nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void definePermission_throwsLDAPException_throwsIllegalStateException() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setPermissionId("cn=uniqueId,dc=example");
        definedPermission.setClientId("clientId");
        definedPermission.setCustomerId("customerId");
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.definePermission("foo", definedPermission);
    }

    @Test
    public void delegatePermission_runsSuccessfully() throws Exception {
        DelegatedPermission delegatedPermission = new DelegatedPermission();
        delegatedPermission.setPermissionId("foo");
        delegatedPermission.setClientId("clientId");
        delegatedPermission.setCustomerId("customerId");
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        assertThat("runs and returns null", spy.delegatePermission("cn=uniqueId", delegatedPermission), nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void delegatePermission_throwsLDAPException_throwsIllegalStateException() throws Exception {
        DelegatedPermission delegatedPermission = new DelegatedPermission();
        delegatedPermission.setPermissionId("foo");
        delegatedPermission.setClientId("clientId");
        delegatedPermission.setCustomerId("customerId");
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.delegatePermission("foo", delegatedPermission);
    }

    @Test
    public void deleteScopeAccess_callsDeleteEntryAndSubtree() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        scopeAccess.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        scopeAccess.getUniqueId();
        doNothing().when(spy).deleteEntryAndSubtree(eq("uniqueId"),any(Audit.class));
        spy.deleteScopeAccess(scopeAccess);
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"),any(Audit.class));
    }

    @Test
    public void doesAccessTokenHavePermission_tokenInstanceOfDelegatedClientScopeAccess_returnsFalse() throws Exception {
        ScopeAccess token = new DelegatedClientScopeAccess();
        token.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        Permission permission = new Permission();
        permission.setCustomerId("customerId");
        permission.setClientId("clientId");
        permission.setPermissionId("permissionId");
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        assertThat("should return false", spy.doesAccessTokenHavePermission(token, permission), equalTo(false));
    }

    @Test
    public void doesAccessTokenHavePermission_tokenNotInstanceOfDelegatedClientScopeAccess_returnsFalse() throws Exception {
        ScopeAccess token = new ScopeAccess();
        token.setLdapEntry(new ReadOnlyEntry("cn=example,dc=uniqueId",new Attribute[0]));
        Permission permission = new Permission();
        permission.setCustomerId("customerId");
        permission.setClientId("clientId");
        permission.setPermissionId("permissionId");
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        assertThat("should return false",spy.doesAccessTokenHavePermission(token,permission),equalTo(false));
    }

    @Test (expected = IllegalStateException.class)
    public void doesAccessTokenHavePermission_throwsLdapException_throwsIllegalStateException() throws Exception {
        ScopeAccess token = new ScopeAccess();
        token.setLdapEntry(new ReadOnlyEntry("foo",new Attribute[0]));
        Permission permission = new Permission();
        permission.setCustomerId("customerId");
        permission.setClientId("clientId");
        permission.setPermissionId("permissionId");
        spy.doesAccessTokenHavePermission(token,permission);
    }

    @Test
    public void doesParentHaveScopeAccess_returnsFalse() throws Exception {
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        ScopeAccess scopeAccess = new ScopeAccess();
        scopeAccess.setClientId("clientId");
        doReturn(ldapInterface).when(spy).getAppInterface();
        assertThat("should return false", spy.doesParentHaveScopeAccess("cn=uniqueId", scopeAccess), equalTo(false));
    }

    @Test (expected = IllegalStateException.class)
    public void doesParentHaveScopeAccess_throwsLdapException_throwsIllegalStateException() throws Exception {
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.doesParentHaveScopeAccess("cn=uniqueId",new ScopeAccess());
    }

    @Test
    public void getDelegateScopeAccessForParentByClientId_searchResultEntryListPopulated_callsDecodeScopeAccessAndReturnsScopeAccess() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        ScopeAccess scopeAccess = new ScopeAccess();
        searchEntries.add(searchResult);
        doReturn(searchEntries).when(spy).getMultipleEntries(anyString(), eq(SearchScope.SUB), any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResult);
        assertThat("returns scope access",spy.getDelegateScopeAccessForParentByClientId("cn=uniqueId", "clientId"),equalTo(scopeAccess));
    }

    @Test
    public void testGetImpersonatedScopeAccessForParentByClientId() throws Exception {

    }

    @Test
    public void testGetDirectScopeAccessForParentByClientId() throws Exception {

    }

    @Test
    public void testGetPermissionByParentAndPermission() throws Exception {

    }

    @Test
    public void testGetPermissionsByParentAndPermission() throws Exception {

    }

    @Test
    public void testGetPermissionsByParent() throws Exception {

    }

    @Test
    public void testGetPermissionsByPermission() throws Exception {

    }

    @Test
    public void testGetScopeAccessByAccessToken() throws Exception {

    }

    @Test
    public void testGetScopeAccessByUserId() throws Exception {

    }

    @Test
    public void testGetScopeAccessByAuthorizationCode() throws Exception {

    }

    @Test
    public void testGetScopeAccessByRefreshToken() throws Exception {

    }

    @Test
    public void testGetScopeAccessByUsernameAndClientId() throws Exception {

    }

    @Test
    public void testGetDelegatedClientScopeAccessByUsername() throws Exception {

    }

    @Test
    public void testGetScopeAccessesByParent() throws Exception {

    }

    @Test
    public void testGetScopeAccessByParentAndClientId() throws Exception {

    }

    @Test
    public void testGetScopeAccessesByParentAndClientId() throws Exception {

    }

    @Test
    public void testGetDelegateScopeAccessesByParent() throws Exception {

    }

    @Test
    public void testGrantPermission() throws Exception {

    }

    @Test
    public void testRemovePermissionFromScopeAccess() throws Exception {

    }

    @Test
    public void testUpdatePermissionForScopeAccess() throws Exception {

    }

    @Test
    public void testUpdateScopeAccess() throws Exception {

    }

    @Test
    public void testAddScopeAccess() throws Exception {

    }
}
