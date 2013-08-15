package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
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
@RunWith(MockitoJUnitRunner.class)
public class LdapScopeAccessRepositoryTestOld extends InMemoryLdapIntegrationTest{

    @Mock
    Configuration config;
    @Mock
    LdapConnectionPools ldapConnectionPools;
    @InjectMocks
    LdapScopeAccessRepository ldapScopeAccessPeristenceRepository = new LdapScopeAccessRepository();
    LdapScopeAccessRepository spy;

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
    public void deleteScopeAccess_callsDeleteEntryAndSubtree() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        scopeAccess.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        scopeAccess.getUniqueId();
        doNothing().when(spy).deleteEntryAndSubtree(eq("uniqueId"),any(Audit.class));
        spy.deleteScopeAccess(scopeAccess);
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"),any(Audit.class));
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
    public void getImpersonatedScopeAccessForParentByClientId_filterUsesImpersonatingUsername() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        ScopeAccess scopeAccess = new ScopeAccess();
        searchEntries.add(searchResult);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=TOKENS,uniqueId"), eq(SearchScope.SUB), argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResult);
        spy.getAllImpersonatedScopeAccessForParentByUser("uniqueId", "username");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute",filters[1].getAttributeName(),equalTo("impersonatingUsername"));
    }

    @Test
    public void getImpersonatedAccessForParentByClientId_searchResultEntryListEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("cn=TOKENS,cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        List<ScopeAccess> scopeAccessList = spy.getAllImpersonatedScopeAccessForParentByUser("cn=uniqueId", "username");
        assertThat("returns scope access list", scopeAccessList.size(), equalTo(0));
    }

    @Test
    public void getImpersonatedScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNoSuchObject_returnsNull() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.NO_SUCH_OBJECT));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=TOKENS,uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);
        List<ScopeAccess> returnedList = spy.getAllImpersonatedScopeAccessForParentByUser("uniqueId", "username");
        assertThat("returns scope access", returnedList.size(), equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getImpersonatedScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNotNoSuchObject_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=TOKENS,uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);
        spy.getAllImpersonatedScopeAccessForParentByUser("uniqueId", "username");
    }

    @Test
    public void getDirectScopeAccessForParentByClientId_setsAttributeNameToClientId() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        ScopeAccess scopeAccess = new ScopeAccess();
        searchEntries.add(searchResult);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=uniqueId"), eq(SearchScope.SUB), argumentCaptor.capture());
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResult);
        spy.getDirectScopeAccessForParentByClientId("cn=uniqueId", "clientId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name",filters[1].getAttributeName(),equalTo("clientId"));
    }

    @Test
    public void getDirectAccessForParentByClientId_searchResultEntryListEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        List<ScopeAccess> scopeAccessList = spy.getDirectScopeAccessForParentByClientId("cn=uniqueId", "clientId");
        assertThat("returns scope access",scopeAccessList.size(), equalTo(0));
    }

    @Test
    public void getDirectScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNoSuchObject_returnsNull() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.NO_SUCH_OBJECT));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);

        List<ScopeAccess> scopeAccessList = spy.getDirectScopeAccessForParentByClientId("cn=uniqueId", "clientId");
        assertThat("returns scope access",scopeAccessList.size(), equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getDirectScopeAccessForParentByClientId_throwsLDAPExceptionWithResultCodeNotNoSuchObject_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResult = new SearchResultEntry("cn=uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResult);
        LDAPPersistException ldapException = new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX));
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("cn=uniqueId"), eq(SearchScope.SUB), any(Filter.class));
        doThrow(ldapException).when(spy).decodeScopeAccess(searchResult);
        spy.getDirectScopeAccessForParentByClientId("cn=uniqueId", "clientId");
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
        spy.getDelegatedScopeAccessByAuthorizationCode("authorizationCode");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name", filters[1].getAttributeName(), equalTo("authCode"));
    }

    @Test
    public void getScopeAccessByAuthorizationCode_searchEntryListPopulated_returnsDelegatedClientScopeAccess() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new DelegatedClientScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        assertThat("returns scope access",spy.getDelegatedScopeAccessByAuthorizationCode("authorizationCode"),equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessByAuthorizationCode_searchEntryListEmpty_returnsNull() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.SUB), any(Filter.class));
        assertThat("returns scope access",spy.getDelegatedScopeAccessByAuthorizationCode("authorizationCode"),nullValue());
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessByAuthorizationCode_throwsLDAPException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getDelegatedScopeAccessByAuthorizationCode("authorizationCode");
    }

    @Test
    public void getDelegatedClientScopeAccessByUsername_setsAttributeNameToUid() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.SUB), argumentCaptor.capture());
        spy.getDelegatedClientScopeAccessByUsername("username");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name", filters[1].getAttributeName(), equalTo("uid"));
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
    public void getScopeAccessesByParentAndClientId_setsFilterAttributeToClientId() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),argumentCaptor.capture());
        spy.getScopeAccessesByParentAndClientId("uniqueId", "clientId");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute name", filters[1].getAttributeName(), equalTo("clientId"));
    }

    @Test
    public void getScopeAccessesByParentAndClientId_searchEntriesExist_returnsPopulatedScopeAccessList() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        doReturn(scopeAccess).when(spy).decodeScopeAccess(searchResultEntry);
        List<ScopeAccess> list = spy.getScopeAccessesByParentAndClientId("uniqueId", "clientId");
        assertThat("list size",list.size(),equalTo(1));
        assertThat("scope access",list.get(0),equalTo(scopeAccess));
    }

    @Test
    public void getScopeAccessesByParentAndClientId_searchEntriesDoNotExist_returnsEmptyScopeAccessList() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        List<ScopeAccess> list = spy.getScopeAccessesByParentAndClientId("uniqueId", "clientId");
        assertThat("list size",list.size(),equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getScopeAccessesByParentAndClientId_throwsLDAPException_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        List<SearchResultEntry> searchEntries = new ArrayList<SearchResultEntry>();
        searchEntries.add(searchResultEntry);
        doReturn(searchEntries).when(spy).getMultipleEntries(eq("uniqueId"),eq(SearchScope.SUB),any(Filter.class));
        doThrow(new LDAPPersistException(new LDAPException(ResultCode.INVALID_DN_SYNTAX))).when(spy).decodeScopeAccess(searchResultEntry);
        spy.getScopeAccessesByParentAndClientId("uniqueId", "clientId");
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
}
