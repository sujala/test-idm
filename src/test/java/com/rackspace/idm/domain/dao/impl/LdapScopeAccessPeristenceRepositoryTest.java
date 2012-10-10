package com.rackspace.idm.domain.dao.impl;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.naming.directory.SearchResult;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 5:20 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapScopeAccessPeristenceRepositoryTest extends InMemoryLdapIntegrationTest{

    @Mock
    Configuration config;
    @Mock
    LdapConnectionPools ldapConnectionPools;
    @InjectMocks
    LdapScopeAccessPeristenceRepository ldapScopeAccessPeristenceRepository = new LdapScopeAccessPeristenceRepository();
    LdapScopeAccessPeristenceRepository spy;

    @Before
    public void setUp() throws Exception {
        spy = spy(ldapScopeAccessPeristenceRepository);
    }

    @Test
    public void getMultipleEntries_throwsLDAPException_returnsEmptyList() throws Exception {

        final Filter filter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(ldapScopeAccessPeristenceRepository.ATTR_OBJECT_CLASS, ldapScopeAccessPeristenceRepository.OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ldapScopeAccessPeristenceRepository.ATTR_CLIENT_ID, "clientId").build();

        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        doThrow(new LDAPSearchException(ResultCode.INVALID_DN_SYNTAX,"error")).when(ldapInterface).search(any(SearchRequest.class));
        List<SearchResultEntry> list = spy.getMultipleEntries("baseDN",SearchScope.SUB,filter);
        assertThat("list size",list.size(),equalTo(0));
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
    public void getDelegateScopeAccessForParentByClientId_filterUsesClientId() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        ScopeAccess scopeAccess = new ScopeAccess();
        searchEntries.add(searchResult);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DELEGATE TOKENS,cn=uniqueId"), eq(SearchScope.SUB), argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResult);
        spy.getDelegateScopeAccessForParentByClientId("cn=uniqueId", "clientId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("returns scope access",filters[1].getAttributeName(),equalTo("clientId"));
    }

    @Test
    public void getDelegateScopeAccessForParentByClientId_searchResultEntryListPopulated_callsDecodeScopeAccessAndReturnsScopeAccess() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        ScopeAccess scopeAccess = new ScopeAccess();
        searchEntries.add(searchResult);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DELEGATE TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResult);
        assertThat("returns scope access",spy.getDelegateScopeAccessForParentByClientId("cn=uniqueId", "clientId"),equalTo(scopeAccess));
    }

    @Test
    public void getDelegateScopeAccessForParentByClientId_searchResultEntryListEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("cn=DELEGATE TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        assertThat("returns scope access",spy.getDelegateScopeAccessForParentByClientId("cn=uniqueId", "clientId"),nullValue());
    }

    @Test
    public void getDelegateScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNoSuchObject_returnsNull() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.NO_SUCH_OBJECT));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DELEGATE TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);
        assertThat("returns scope access", spy.getDelegateScopeAccessForParentByClientId("cn=uniqueId", "clientId"), nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getDelegateScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNotNoSuchObject_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DELEGATE TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);
        spy.getDelegateScopeAccessForParentByClientId("cn=uniqueId", "clientId");
    }

    @Test
    public void getImpersonatedScopeAccessForParentByClientId_filterUsesImpersonatingUsername() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        ScopeAccess scopeAccess = new ScopeAccess();
        searchEntries.add(searchResult);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=IMPERSONATED TOKENS,uniqueId"), eq(SearchScope.SUB), argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResult);
        spy.getImpersonatedScopeAccessForParentByClientId("uniqueId","username");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute",filters[1].getAttributeName(),equalTo("impersonatingUsername"));
    }

    @Test
    public void getImpersonatedScopeAccessForParentByClientId_searchResultEntryListPopulated_callsDecodeScopeAccessAndReturnsScopeAccess() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        ScopeAccess scopeAccess = new ScopeAccess();
        searchEntries.add(searchResult);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=IMPERSONATED TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResult);
        assertThat("returns scope access",spy.getImpersonatedScopeAccessForParentByClientId("cn=uniqueId", "username"),equalTo(scopeAccess));
    }

    @Test
    public void getImpersonatedAccessForParentByClientId_searchResultEntryListEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("cn=IMPERSONATED TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        assertThat("returns scope access",spy.getImpersonatedScopeAccessForParentByClientId("cn=uniqueId", "username"),nullValue());
    }

    @Test
    public void getImpersonatedScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNoSuchObject_returnsNull() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.NO_SUCH_OBJECT));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=IMPERSONATED TOKENS,uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);
        assertThat("returns scope access", spy.getImpersonatedScopeAccessForParentByClientId("uniqueId", "username"), nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getImpersonatedScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNotNoSuchObject_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=IMPERSONATED TOKENS,uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);
        spy.getImpersonatedScopeAccessForParentByClientId("uniqueId", "username");
    }

    @Test
    public void getDirectScopeAccessForParentByClientId_setsAttributeNameToClientId() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        ScopeAccess scopeAccess = new ScopeAccess();
        searchEntries.add(searchResult);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DIRECT TOKENS,cn=uniqueId"), eq(SearchScope.SUB), argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResult);
        spy.getDirectScopeAccessForParentByClientId("cn=uniqueId", "clientId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name",filters[1].getAttributeName(),equalTo("clientId"));
    }

    @Test
    public void getDirectScopeAccessForParentByClientId_searchResultEntryListPopulated_callsDecodeScopeAccessAndReturnsScopeAccess() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        ScopeAccess scopeAccess = new ScopeAccess();
        searchEntries.add(searchResult);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DIRECT TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResult);
        assertThat("returns scope access",spy.getDirectScopeAccessForParentByClientId("cn=uniqueId", "clientId"),equalTo(scopeAccess));
    }

    @Test
    public void getDirectAccessForParentByClientId_searchResultEntryListEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("cn=DIRECT TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        assertThat("returns scope access",spy.getDirectScopeAccessForParentByClientId("cn=uniqueId", "clientId"),nullValue());
    }

    @Test
    public void getDirectScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNoSuchObject_returnsNull() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.NO_SUCH_OBJECT));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DIRECT TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);
        assertThat("returns scope access", spy.getDirectScopeAccessForParentByClientId("cn=uniqueId", "clientId"), nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getDirectScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNotNoSuchObject_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DIRECT TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);
        spy.getDirectScopeAccessForParentByClientId("cn=uniqueId", "clientId");
    }

    @Test
    public void getPermissionByParentAndPermission_listSizeOne_returnsPermission() throws Exception {
        Permission permission = new Permission();
        List<Permission> list = new ArrayList<Permission>();
        list.add(permission);
        doReturn(list).when(spy).getPermissionsByParentAndPermission(null,null);
        assertThat("returns permission", spy.getPermissionByParentAndPermission(null, null), equalTo(permission));
    }

    @Test
    public void getPermissionByParentAndPermission_listSizeTwo_returnsNull() throws Exception {
        List<Permission> list = new ArrayList<Permission>();
        list.add(new Permission());
        list.add(new Permission());
        doReturn(list).when(spy).getPermissionsByParentAndPermission(null,null);
        assertThat("returns permission", spy.getPermissionByParentAndPermission(null, null), nullValue());
    }

    @Test
    public void getPermissionsByParentAndPermission_searchEntriesFound_returnsPopulatedPermissionList() throws Exception {
        Permission permission1 = new Permission();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        Permission permission2 = new Permission();

        doReturn(null).when(spy).getFilterForPermission(permission1);
        doReturn(searchEntries).when(spy).getMultipleEntries("uniqueId",SearchScope.SUB,null);
        doReturn(permission2).when(spy).decodePermission(searchResultEntry);

        List<Permission> list = spy.getPermissionsByParentAndPermission("uniqueId",permission1);
        assertThat("returns list with size one",list.size(),equalTo(1));
        assertThat("returns permission", list.get(0),equalTo(permission2));
    }

    @Test
    public void getPermissionsByParentAndPermission_searchEntriesNotFound_returnsEmptyPermissionList() throws Exception {
        Permission permission1 = new Permission();

        doReturn(null).when(spy).getFilterForPermission(permission1);
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries("uniqueId",SearchScope.SUB,null);

        List<Permission> list = spy.getPermissionsByParentAndPermission("uniqueId",permission1);
        assertThat("returns list with size one",list.size(),equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getPermissionsByParentAndPermission_throwsLdapException_throwsIllegalStateException() throws Exception {
        Permission permission1 = new Permission();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);

        doReturn(null).when(spy).getFilterForPermission(permission1);
        doReturn(searchEntries).when(spy).getMultipleEntries("uniqueId",SearchScope.SUB,null);
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodePermission(searchResultEntry);

        spy.getPermissionsByParentAndPermission("uniqueId",permission1);
    }

    @Test
    public void getPermissionsByParent_callsGetPermissionsByParentAndPermission() throws Exception {
        doReturn(new ArrayList<Permission>()).when(spy).getPermissionsByParentAndPermission("uniqueId",null);
        spy.getPermissionsByParent("uniqueId");
        verify(spy).getPermissionsByParentAndPermission("uniqueId",null);
    }

    @Test
    public void getPermissionsByParent_returnsList() throws Exception {
        doReturn(new ArrayList<Permission>()).when(spy).getPermissionsByParentAndPermission("uniqueId", null);
        assertThat("returns a list", spy.getPermissionsByParent("uniqueId"), instanceOf(List.class));
    }

    @Test
    public void getPermissionsByPermission_callsGetPermissionsByParentAndPermission() throws Exception {
        Permission permission = new Permission();
        doReturn(new ArrayList<Permission>()).when(spy).getPermissionsByParentAndPermission("o=rackspace,dc=rackspace,dc=com", permission);
        spy.getPermissionsByPermission(permission);
        verify(spy).getPermissionsByParentAndPermission("o=rackspace,dc=rackspace,dc=com", permission);
    }

    @Test
    public void getPermissionsByPermission_returnsList() throws Exception {
        Permission permission = new Permission();
        doReturn(new ArrayList<Permission>()).when(spy).getPermissionsByParentAndPermission("o=rackspace,dc=rackspace,dc=com", permission);
        assertThat("returns a list", spy.getPermissionsByPermission(permission), instanceOf(List.class));
    }

    @Test
    public void getScopeAccessByAccessToken_setsFilterAttributeToAccessToken() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchEntry);
        spy.getScopeAccessByAccessToken("accessToken");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("returns scope access", filters[1].getAttributeName(), equalTo("accessToken"));
    }

    @Test
    public void getScopeAccessByAccessToken_nullSearchEntries_returnsNull() throws Exception {
        doReturn(null).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        assertThat("returns null", spy.getScopeAccessByAccessToken("accessToken"), nullValue());
    }

    @Test
    public void getScopeAccessByAccessToken_searchEntriesExists_returnsScopeAccess() throws Exception {
        SearchResultEntry searchEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchEntry);
        assertThat("returns scope access", spy.getScopeAccessByAccessToken("accessToken"), equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessByAccessToken_searchEntriesDoNotExists_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        assertThat("returns scope access", spy.getScopeAccessByAccessToken("accessToken"), nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessByAccessToken_throwsLdapException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchEntry);
        assertThat("returns scope access", spy.getScopeAccessByAccessToken("accessToken"), nullValue());
    }

    @Test
    public void getScopeAccessByUserId_setsFilterAttributeToUserRsId() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessByUserId("userId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("returns scope access", filters[1].getAttributeName(), equalTo("userRsId"));
    }

    @Test
    public void getScopeAccessByUserId_entryListPopulated_returnsScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        assertThat("returns scope access", spy.getScopeAccessByUserId("userId"), equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessByUserId_entryEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        assertThat("returns null",spy.getScopeAccessByUserId("userId"),nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessByUserId_throwsLDAPException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessByUserId("userId");
    }

    @Test
    public void getScopeAccessListByUserId_setsFilterAttributeToUserRsId() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),argumentCaptor.capture());
        spy.getScopeAccessListByUserId("userId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name",filters[1].getAttributeName(),equalTo("userRsId"));
    }

    @Test
    public void getScopeAccessListByUserId_searchEntriesExist_returnsPopulatedScopeAccessList() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB), any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        List<ScopeAccess> list = spy.getScopeAccessListByUserId("userId");
        assertThat("list size",list.size(),equalTo(1));
        assertThat("list has scope access",list.get(0),equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessListByUserId_searchEntriesDoNotExist_returnsEmptyScopeAccessList() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB), any(Filter.class));
        assertThat("list size",spy.getScopeAccessListByUserId("userId").size(),equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessListByUserId_throwsLdapException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB), any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessListByUserId("userId");
    }

    @Test
    public void getScopeAccessByAuthorizationCode_setsFilterAttributeToAuthCode() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new DelegatedClientScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessByAuthorizationCode("authorizationCode");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name",filters[1].getAttributeName(),equalTo("authCode"));
    }

    @Test
    public void getScopeAccessByAuthorizationCode_searchEntryListPopulated_returnsDelegatedClientScopeAccess() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new DelegatedClientScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        assertThat("returns scope access",spy.getScopeAccessByAuthorizationCode("authorizationCode"),equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessByAuthorizationCode_searchEntryListEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        assertThat("returns scope access",spy.getScopeAccessByAuthorizationCode("authorizationCode"),nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessByAuthorizationCode_throwsLDAPException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessByAuthorizationCode("authorizationCode");
    }

    @Test
    public void getScopeAccessByRefreshToken_setsAttributeNameToRefreshToken() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessByRefreshToken("refreshToken");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name",filters[1].getAttributeName(),equalTo("refreshToken"));
    }

    @Test
    public void getScopeAccessByRefreshToken_searchEntryListPopulated_returnsScopeAccess() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        assertThat("returns scope access",spy.getScopeAccessByRefreshToken("refreshToken"),equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessByRefreshToken_searchEntryListEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        assertThat("returns scope access",spy.getScopeAccessByRefreshToken("refreshToken"),nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessByRefreshToken_throwsLDAPException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessByRefreshToken("refreshToken");
    }

    @Test
    public void getScopeAccessByUsernameAndClientId_setsAttributeNameToUid() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessByUsernameAndClientId("username","clientId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name",filters[1].getAttributeName(),equalTo("uid"));
    }

    @Test
    public void getScopeAccessByUsernameAndClientId_setsAttributeNameToClientId() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessByUsernameAndClientId("username","clientId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name",filters[2].getAttributeName(),equalTo("clientId"));
    }

    @Test
    public void getScopeAccessByUsernameAndClientId_searchEntryListPopulated_returnsScopeAccess() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        assertThat("returns scope access",spy.getScopeAccessByUsernameAndClientId("username", "clientId"),equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessByUsernameAndClientId_searchEntryListEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        assertThat("returns scope access",spy.getScopeAccessByUsernameAndClientId("username", "clientId"),nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessByUsernameAndClientId_throwsLDAPException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessByUsernameAndClientId("username", "clientId");
    }

    @Test
    public void getDelegatedClientScopeAccessByUsername_setsAttributeNameToUid() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),argumentCaptor.capture());
        spy.getDelegatedClientScopeAccessByUsername("username");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name",filters[1].getAttributeName(),equalTo("uid"));
    }

    @Test
    public void getDelegatedClientScopeAccessByUsername_searchEntriesExist_returnsDelegatedClientScopeAccessList() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(delegatedClientScopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        List<DelegatedClientScopeAccess> list = spy.getDelegatedClientScopeAccessByUsername("username");
        assertThat("list size", list.size(), equalTo(1));
        assertThat("delegated client scope access",list.get(0),equalTo(delegatedClientScopeAccess));
    }

    @Test
    public void getDelegatedClientScopeAccessByUsername_searchEntriesDoNotExist_returnsEmptyDelegatedClientScopeAccessList() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.SUB), any(Filter.class));
        List<DelegatedClientScopeAccess> list = spy.getDelegatedClientScopeAccessByUsername("username");
        assertThat("list size", list.size(), equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getDelegatedClientScopeAccessByUsername_throwsLDAPExcpetion_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getDelegatedClientScopeAccessByUsername("username");
    }

    @Test
    public void getScopeAccessesByParent_searchEntriesExist_returnsScopeAccessList() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        List<ScopeAccess> list = spy.getScopeAccessesByParent("uniqueId");
        assertThat("list size", list.size(), equalTo(1));
        assertThat("scope access",list.get(0),equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessesByParent_searchEntriesDoNotExist_returnsScopeAccessList() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        List<ScopeAccess> list = spy.getScopeAccessesByParent("uniqueId");
        assertThat("list size", list.size(), equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessesByParent_throwsLDAPExcpetion_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessesByParent("uniqueId");
    }

    @Test
    public void getScopeAccessByParentAndClientId_setsAttributeToClientId() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),argumentCaptor.capture());
        spy.getScopeAccessByParentAndClientId("uniqueId","clientId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("attribute name",filters[1].getAttributeName(),equalTo("clientId"));
    }

    @Test
    public void getScopeAccessByParentAndClientId_searchEntriesExist_returnsScopeAccess() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        assertThat("returns scope access", spy.getScopeAccessByParentAndClientId("uniqueId", "clientId"), equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessByParentAndClientId_searchEntriesDoNotExist_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        assertThat("returns null",spy.getScopeAccessByParentAndClientId("uniqueId","clientId"),nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessByParentAndClientId_throwsLDAPException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        assertThat("returns scope access", spy.getScopeAccessByParentAndClientId("uniqueId", "clientId"), equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessesByParentAndClientId_setsFilterAttributeToClientId() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),argumentCaptor.capture());
        spy.getScopeAccessesByParentAndClientId("uniqueId","clientId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name",filters[1].getAttributeName(),equalTo("clientId"));
    }

    @Test
    public void getScopeAccessesByParentAndClientId_searchEntriesExist_returnsPopulatedScopeAccessList() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        List<ScopeAccess> list = spy.getScopeAccessesByParentAndClientId("uniqueId","clientId");
        assertThat("list size",list.size(),equalTo(1));
        assertThat("scope access",list.get(0),equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessesByParentAndClientId_searchEntriesDoNotExist_returnsEmptyScopeAccessList() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        List<ScopeAccess> list = spy.getScopeAccessesByParentAndClientId("uniqueId","clientId");
        assertThat("list size",list.size(),equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessesByParentAndClientId_throwsLDAPException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessesByParentAndClientId("uniqueId","clientId");
    }

    @Test
    public void getDelegateScopeAccessesByParent_searchEntriesExist_returnsPopulatedScopeAccessList() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DELEGATE TOKENS,uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        List<ScopeAccess> list = spy.getDelegateScopeAccessesByParent("uniqueId");
        assertThat("list size",list.size(),equalTo(1));
        assertThat("returns scope access",list.get(0),equalTo(scopeAccess));
    }

    @Test
    public void getDelegateScopeAccessesByParent_searchEntriesDoNotExist_returnsEmptyScopeAccessList() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("cn=DELEGATE TOKENS,uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        List<ScopeAccess> list = spy.getDelegateScopeAccessesByParent("uniqueId");
        assertThat("list size",list.size(),equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getDelegateScopeAccessesByParent_throwsLDAPExceptionAndResultCodeNotNoSuchObject_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DELEGATE TOKENS,uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getDelegateScopeAccessesByParent("uniqueId");
    }

    @Test
    public void getDelegateScopeAccessesByParent_throwsLDAPExceptionAndResultCodeNoSuchObject_returnsEmptyList() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=DELEGATE TOKENS,uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.NO_SUCH_OBJECT))).when(spy).decodeScopeAccess(searchResultEntry);
        List<ScopeAccess> list = spy.getDelegateScopeAccessesByParent("uniqueId");
        assertThat("list size", list.size(), equalTo(0));
    }

    @Test
    public void grantPermission_runsSuccessfully() throws Exception {
        GrantedPermission grantedPermission = new GrantedPermission();
        grantedPermission.setPermissionId("foo");
        grantedPermission.setClientId("clientId");
        grantedPermission.setCustomerId("customerId");
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        assertThat("runs and returns null", spy.grantPermission("cn=uniqueId", grantedPermission), nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void grantPermission_throwsLDAPException_throwsIllegalStateException() throws Exception {
        GrantedPermission grantedPermission = new GrantedPermission();
        grantedPermission.setPermissionId("foo");
        grantedPermission.setClientId("clientId");
        grantedPermission.setCustomerId("customerId");
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.grantPermission("foo", grantedPermission);
    }


    @Test
    public void removePermissionFromScopeAccess_callsDeleteEntryAndSubtree() throws Exception {
        doNothing().when(spy).deleteEntryAndSubtree((String) eq(null), any(Audit.class));
        spy.removePermissionFromScopeAccess(new Permission());
        verify(spy).deleteEntryAndSubtree((String) eq(null), any(Audit.class));
    }

    @Test
    public void removePermissionFromScopeAccess_returnsTrue() throws Exception {
        doNothing().when(spy).deleteEntryAndSubtree((String) eq(null), any(Audit.class));
        assertThat("returns true", spy.removePermissionFromScopeAccess(new Permission()), equalTo(true));
    }


    @Test (expected = IllegalStateException.class)
    public void updatePermissionForScopeAccess_throwsLdapPersistException_throwsIllegalStateException() throws Exception {
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.updatePermissionForScopeAccess(new Permission());
    }

    @Test (expected = IllegalStateException.class)
    public void updateScopeAccess_throwsLDAPException_throwsIllegalStateException() throws Exception {
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.updateScopeAccess(new ScopeAccess());
    }

    @Test
    public void updateScopeAccess_throwsLDAPSDKRuntimeException_returnsFalse() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        scopeAccess.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        assertThat("returns false", spy.updateScopeAccess(scopeAccess), equalTo(false));
    }

    @Test
    public void addScopeAccess_runsSuccessfully() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        scopeAccess.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        scopeAccess.setClientId("clientId");
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        assertThat("runs and returns null", spy.addScopeAccess("uniqueId", scopeAccess), nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void addScopeAccess_throwsLDAPException_throwsIllegalStateException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.addScopeAccess("foo", scopeAccess);
    }

    @Test
    public void decodePermission_attributeIsDefinedPermission_returnsDefinedPermission() throws Exception {
        Attribute attribute = new Attribute("objectClass","definedPermission");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("permission type",ldapScopeAccessPeristenceRepository.decodePermission(searchResultEntry),instanceOf(DefinedPermission.class));
    }

    @Test
    public void decodePermission_attributeIsGrantedPermission_returnsGrantedPermission() throws Exception {
        Attribute attribute = new Attribute("objectClass","grantedPermission");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("permission type",ldapScopeAccessPeristenceRepository.decodePermission(searchResultEntry),instanceOf(GrantedPermission.class));
    }

    @Test
    public void decodePermission_attributeIsDelegatedPermission_returnsDelegatedPermission() throws Exception {
        Attribute attribute = new Attribute("objectClass","delegatedPermission");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("permission type",ldapScopeAccessPeristenceRepository.decodePermission(searchResultEntry),instanceOf(DelegatedPermission.class));
    }

    @Test
    public void decodePermission_attributeIsPermission_returnsPermission() throws Exception {
        Attribute attribute = new Attribute("objectClass","rsPermission");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("permission type",ldapScopeAccessPeristenceRepository.decodePermission(searchResultEntry),instanceOf(Permission.class));
    }

    @Test
    public void decodePermission_attributeIsNotPermission_returnsNull() throws Exception {
        Attribute attribute = new Attribute("objectClass","scopeAccess");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("null",ldapScopeAccessPeristenceRepository.decodePermission(searchResultEntry),nullValue());
    }

    @Test
    public void decodeScopeAccess_attributeIsUserScopeAccess_returnsUserScopeAccess() throws Exception {
        Attribute attribute = new Attribute("objectClass","userScopeAccess");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("scope access type",ldapScopeAccessPeristenceRepository.decodeScopeAccess(searchResultEntry),instanceOf(UserScopeAccess.class));
    }

    @Test
    public void decodeScopeAccess_attributeIsClientScopeAccess_returnsClientScopeAccess() throws Exception {
        Attribute attribute = new Attribute("objectClass","clientScopeAccess");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("scope access type",ldapScopeAccessPeristenceRepository.decodeScopeAccess(searchResultEntry),instanceOf(ClientScopeAccess.class));
    }

    @Test
    public void decodeScopeAccess_attributeIsPasswordResetScopeAccess_returnsClientScopeAccess() throws Exception {
        Attribute attribute = new Attribute("objectClass","passwordResetScopeAccess");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("scope access type",ldapScopeAccessPeristenceRepository.decodeScopeAccess(searchResultEntry),instanceOf(PasswordResetScopeAccess.class));
    }

    @Test
    public void decodeScopeAccess_attributeIsRackerScopeAccess_returnsRackerScopeAccess() throws Exception {
        Attribute attribute = new Attribute("objectClass","rackerScopeAccess");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("scope access type",ldapScopeAccessPeristenceRepository.decodeScopeAccess(searchResultEntry),instanceOf(RackerScopeAccess.class));
    }

    @Test
    public void decodeScopeAccess_attributeIsDelegatedClientScopeAccess_returnsDelegatedClientScopeAccess() throws Exception {
        Attribute attribute = new Attribute("objectClass","delegatedClientScopeAccess");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("scope access type",ldapScopeAccessPeristenceRepository.decodeScopeAccess(searchResultEntry),instanceOf(DelegatedClientScopeAccess.class));
    }

    @Test
    public void decodeScopeAccess_attributeIsScopeAccess_returnsScopeAccess() throws Exception {
        Attribute attribute = new Attribute("objectClass","scopeAccess");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("scope access type",ldapScopeAccessPeristenceRepository.decodeScopeAccess(searchResultEntry),instanceOf(ScopeAccess.class));
    }

    @Test
    public void decodeScopeAccess_attributeIsNotScopeAccess_returnsNull() throws Exception {
        Attribute attribute = new Attribute("objectClass","rsPermission");
        Attribute[] attributes = {attribute};
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",attributes);
        assertThat("scope access type",ldapScopeAccessPeristenceRepository.decodeScopeAccess(searchResultEntry),nullValue());
    }

    @Test
    public void getFilterForPermission_runsWithNoExceptions_returnsFilterWithNullAssertionValue() throws Exception {
        DelegatedPermission delegatedPermission = new DelegatedPermission();
        delegatedPermission.setClientId("clientId");
        delegatedPermission.setCustomerId("customerId");
        delegatedPermission.setPermissionId("permissionId");
        Filter filter = ldapScopeAccessPeristenceRepository.getFilterForPermission(delegatedPermission);
        assertThat("filter for delegated permission",filter.getAssertionValue(),nullValue());
    }

    @Test
    public void getFilterForPermission_runsWithNoExceptions_returnsFilterWithRsPermissionAssertionValue() throws Exception {
        Filter filter = ldapScopeAccessPeristenceRepository.getFilterForPermission(new DelegatedPermission());
        assertThat("filter for delegated permission",filter.getAssertionValue(),equalTo("rsPermission"));
    }
}
