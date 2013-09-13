package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.EndPoints;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
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

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * * These tests should be migrated to LdapRepositoryTest and LdapEndpointRepositoryTest depending on whether the test
 * is testing a method
 * in LdapRepository or LdapEndpointRepository.
 * <p>
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/3/12
 * Time: 12:21 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapEndpointRepositoryDepTest extends InMemoryLdapIntegrationTest {
    @InjectMocks
    LdapEndpointRepository ldapEndpointRepository = new LdapEndpointRepository();
    LdapEndpointRepository spy;
    LDAPInterface ldapInterface;
    EndPoints endPoints;
    @Mock
    LdapConnectionPools connPools = mock(LdapConnectionPools.class);
    @Mock
    Configuration config = mock(Configuration.class);

    @Before
    public void setUp() throws Exception {
        ldapInterface = mock(LDAPInterface.class);

        //setup fields
        endPoints = new EndPoints();
        endPoints.setMossoId(1);
        endPoints.setNastId("nastId");
        endPoints.setUserDN("userDn");
        endPoints.setUsername("username");
        int i = endPoints.hashCode();
        boolean equals = endPoints.equals(endPoints);
        String s = endPoints.toString();
        spy = spy(ldapEndpointRepository);
        doReturn(ldapInterface).when(spy).getAppInterface();
    }

    @Test
    public void getMultipleEntriesThreeParameters_doesNotThrowException_returnsSearchResultEntryList() throws Exception {
        Filter filter = Filter.createEqualityFilter("objectClass", "filter");
        List<SearchResultEntry> searchResultList = new ArrayList<SearchResultEntry>();
        SearchResult searchResult = new SearchResult(123, ResultCode.SUCCESS, "woohoo", "matchedDN", new String[0], searchResultList, null, 0, 0, new Control[0]);
        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(any(SearchRequest.class));
        assertThat("returns search result entry list", spy.getMultipleEntries("dn", SearchScope.SUB, filter), equalTo(searchResultList));
    }

    @Test
    public void getMultipleEntriesFourParameters_doesNotThrowException_returnsSearchResultEntryList() throws Exception {
        Filter filter = Filter.createEqualityFilter("objectClass", "filter");
        List<SearchResultEntry> searchResultList = new ArrayList<SearchResultEntry>();
        SearchResult searchResult = new SearchResult(123, ResultCode.SUCCESS, "woohoo", "matchedDN", new String[0], searchResultList, null, 0, 0, new Control[0]);
        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(any(SearchRequest.class));
        assertThat("returns search result entry list", spy.getMultipleEntries("dn", SearchScope.SUB, "sortAttribute", filter), equalTo(searchResultList));
    }

    @Test
    public void getMultipleEntriesFourParameters_setsRequestControls() throws Exception {
        ArgumentCaptor<SearchRequest> argumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        Filter filter = Filter.createEqualityFilter("objectClass", "filter");
        List<SearchResultEntry> searchResultList = new ArrayList<SearchResultEntry>();
        SearchResult searchResult = new SearchResult(123, ResultCode.SUCCESS, "woohoo", "matchedDN", new String[0], searchResultList, null, 0, 0, new Control[0]);
        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(argumentCaptor.capture());
        spy.getMultipleEntries("dn", SearchScope.SUB, "sortAttribute", filter);
        ServerSideSortRequestControl control = (ServerSideSortRequestControl) argumentCaptor.getValue().getControls()[0];
        assertThat("returns search result entry list", control.getSortKeys()[0].getAttributeName(), equalTo("sortAttribute"));
    }

    @Test
    public void getMultipleEntriesFourParameters_throwsException_returnsEmptyArrayList() throws Exception {
        Filter filter = Filter.createEqualityFilter("objectClass", "filter");
        doReturn(ldapInterface).when(spy).getAppInterface();
        doThrow(new LDAPSearchException(ResultCode.INVALID_DN_SYNTAX, "testing")).when(ldapInterface).search(any(SearchRequest.class));
        List<SearchResultEntry> list = spy.getMultipleEntries("dn", SearchScope.SUB, "sortAttribute", filter);
        assertThat("returns empty list", list.size(), equalTo(0));
    }

    @Test(expected = IllegalStateException.class)
    public void getSingleEntry_throwsLDAPException_throwsIllegalStateException() throws Exception {
        Filter searchFilter = Filter.createEqualityFilter("attributeName", "assertionValue");
        doReturn(ldapInterface).when(spy).getAppInterface();
        String[] attributes = {"test"};
        doThrow(new LDAPSearchException(ResultCode.INVALID_DN_SYNTAX, "testing")).when(ldapInterface).searchForEntry("baseDN", SearchScope.SUB, searchFilter, attributes);
        spy.getSingleEntry("baseDN", SearchScope.SUB, searchFilter, attributes);
    }

    @Test
    public void updateEntry_throwsNoExceptions_callsModify() throws Exception {
        Audit audit = mock(Audit.class);
        List<Modification> list = new ArrayList<Modification>();
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.updateEntry("entryDn", list, audit);
        verify(ldapInterface).modify("entryDn", list);
    }

    @Test
    public void updateEntry_throwsLDAPException_throwsIllegalStateExceptionAndAuditFails() throws Exception {
        Audit audit = mock(Audit.class);
        try {
            List<Modification> list = new ArrayList<Modification>();
            doReturn(ldapInterface).when(spy).getAppInterface();
            doThrow(new LDAPException(ResultCode.INVALID_DN_SYNTAX)).when(ldapInterface).modify("entryDn", list);
            spy.updateEntry("entryDn", list, audit);
            assertTrue("should throw exception", false);
        } catch (IllegalStateException ex) {
            verify(audit).fail();
        }
    }

    @Test
    public void getLdapPagingOffsetDefault_callsConfigMethod() throws Exception {
        ldapEndpointRepository.getLdapPagingOffsetDefault();
        verify(config).getInt("ldap.paging.offset.default");
    }

    @Test
    public void getLdapPagingLimitDefault_callsConfigMethod() throws Exception {
        ldapEndpointRepository.getLdapPagingLimitDefault();
        verify(config).getInt("ldap.paging.limit.default");
    }

    @Test
    public void getRackspaceInumPrefix_callsConfigMethod() throws Exception {
        ldapEndpointRepository.getRackspaceInumPrefix();
        verify(config).getString("rackspace.inum.prefix");
    }

    @Test
    public void getRackspaceCustomerId_callsConfigMethod() throws Exception {
        ldapEndpointRepository.getRackspaceCustomerId();
        verify(config).getString("rackspace.customerId");
    }

    @Test
    public void addContainer_callsAddEntry() throws Exception {
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addContainer("parentUniqueId", "name");
        verify(spy).addEntry(eq("cn=name,parentUniqueId"), any(Attribute[].class), any(Audit.class));
    }

    @Test
    public void addContainer_attributesListPopulated() throws Exception {
        ArgumentCaptor<Attribute[]> argumentCaptor = ArgumentCaptor.forClass(Attribute[].class);
        doNothing().when(spy).addEntry(anyString(), argumentCaptor.capture(), any(Audit.class));
        spy.addContainer("parentUniqueId", "name");
        assertThat("list size", argumentCaptor.getValue().length, equalTo(2));
        assertThat("first attribute is Rackspace container", argumentCaptor.getValue()[0].getValue(), equalTo("rsContainer"));
        assertThat("second attribute is name", argumentCaptor.getValue()[1].getValue(), equalTo("name"));
    }

    @Test
    public void getContainer_callsGetSingleEntry() throws Exception {
        doReturn(new SearchResultEntry("uniqueId", new Attribute[0])).when(spy).getSingleEntry(eq("parentUniqueId"), eq(SearchScope.ONE), any(Filter.class));
        spy.getContainer("parentUniqueId", "name");
        verify(spy).getSingleEntry(eq("parentUniqueId"), eq(SearchScope.ONE), any(Filter.class));
    }

    @Test
    public void getContainer_filterContainsCorrectAttributes() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        doReturn(new SearchResultEntry("uniqueId", new Attribute[0])).when(spy).getSingleEntry(eq("parentUniqueId"), eq(SearchScope.ONE), argumentCaptor.capture());
        spy.getContainer("parentUniqueId", "name");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute is rackspace container", filters[0].getAssertionValue(), equalTo("rsContainer"));
        assertThat("filter attribute is name", filters[1].getAssertionValue(), equalTo("name"));
    }

    @Test
    public void getContainer_returnsSearchResultEntry() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("parentUniqueId"), eq(SearchScope.ONE), any(Filter.class));
        assertThat("returns a search result entry", spy.getContainer("parentUniqueId", "name"), equalTo(searchResultEntry));
    }

    @Test
    public void getNextId_callsGetSingleEntry() throws Exception {
        Attribute[] attributes = {new Attribute("rsId", "123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn", attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.getNextId("type");
        verify(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.ONE), any(Filter.class));
    }

    @Test
    public void getNextId_filterHasCorrectAttributes() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        Attribute[] attributes = {new Attribute("rsId", "123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn", attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.ONE), argumentCaptor.capture());
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.getNextId("type");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("first attribute vale", filters[0].getAssertionValue(), equalTo("rsNextId"));
        assertThat("second attribute value", filters[1].getAssertionValue(), equalTo("type"));
    }

    @Test
    public void getNextId_modificationListHasCorrectDeleteModification() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Attribute[] attributes = {new Attribute("rsId", "123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn", attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), argumentCaptor.capture())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.getNextId("type");
        List<Modification> mods = argumentCaptor.getValue();
        assertThat("first modification deletes", mods.get(0).getModificationType(), equalTo(ModificationType.DELETE));
        assertThat("first modification acts on id", mods.get(0).getAttributeName(), equalTo("rsId"));
        assertThat("first modification acts on correct id value", mods.get(0).getAttribute().getValue(), equalTo("123"));
    }

    @Test
    public void getNextId_modificationListHasCorrectAddModification() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Attribute[] attributes = {new Attribute("rsId", "123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn", attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), argumentCaptor.capture())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.getNextId("type");
        List<Modification> mods = argumentCaptor.getValue();
        assertThat("second modification deletes", mods.get(1).getModificationType(), equalTo(ModificationType.ADD));
        assertThat("second modification acts on id", mods.get(1).getAttributeName(), equalTo("rsId"));
        assertThat("second modification acts on correct id value", mods.get(1).getAttribute().getValue(), equalTo("124"));
    }

    @Test
    public void getNextId_callsModify() throws Exception {
        Attribute[] attributes = {new Attribute("rsId", "123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn", attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.getNextId("type");
        verify(ldapInterface).modify(eq("testDn"), any(List.class));
    }

    @Test
    public void getNextId_throwsNoExceptions_returnsStringValue() throws Exception {
        Attribute[] attributes = {new Attribute("rsId", "123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn", attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        assertThat("returns a string of the id", spy.getNextId("type"), equalTo("123"));
    }

    @Test(expected = IllegalStateException.class)
    public void getNextId_throwsLdapExceptionResultCodeNotNoSuchAttribute_throwsIllegalStateException() throws Exception {
        Attribute[] attributes = {new Attribute("rsId", "123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn", attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"), eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        doThrow(new LDAPException(ResultCode.INVALID_DN_SYNTAX)).when(ldapInterface).modify(eq("testDn"), any(List.class));
        spy.getNextId("type");
    }

    @Test
    public void queryPairConstructor_attributeIsBlank_throwsIllegalArgumentException() throws Exception {
        try {
            new LdapRepository.QueryPair("", "comparer", "value");
            assertTrue("should throw exception", false);
        } catch (IllegalArgumentException ex) {
            assertThat("has correct message", ex.getMessage(), equalTo("attribute cannot be empty"));
        }
    }

    @Test
    public void queryPairConstructor_comparerIsBlank_throwsIllegalArgumentException() throws Exception {
        try {
            new LdapRepository.QueryPair("attribute", "", "value");
            assertTrue("should throw exception", false);
        } catch (IllegalArgumentException ex) {
            assertThat("has correct message", ex.getMessage(), equalTo("comparer cannot be empty"));
        }
    }

    @Test
    public void queryPairConstructor_valueIsBlank_throwsIllegalArgumentException() throws Exception {
        try {
            new LdapRepository.QueryPair("attribute", "comparer", "");
            assertTrue("should throw exception", false);
        } catch (IllegalArgumentException ex) {
            assertThat("has correct message", ex.getMessage(), equalTo("value cannot be empty"));
        }
    }

    @Test
    public void addEqualAttribute_addsFilterToList() throws Exception {
        byte[] bytes = "attributeValue".getBytes();
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        LdapRepository.LdapSearchBuilder returnedValue = searchBuilder.addEqualAttribute("attributeName", bytes);
        assertThat("search builder has filter with correct attribute name", returnedValue.build().getAttributeName(), equalTo("attributeName"));
        assertThat("search builder has filter with correct attribute value", returnedValue.build().getAssertionValue(), equalTo("attributeValue"));
    }

    @Test
    public void addGreaterOrEqualAttribute_addsFilterToList() throws Exception {
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        LdapRepository.LdapSearchBuilder returnedValue = searchBuilder.addGreaterOrEqualAttribute("attributeName", "attributeValue");
        assertThat("search builder has filter with correct attribute name", returnedValue.build().getAttributeName(), equalTo("attributeName"));
        assertThat("search builder has filter with correct attribute value", returnedValue.build().getAssertionValue(), equalTo("attributeValue"));
    }

    @Test
    public void build_filterListEmpty_returnsNewFilter() throws Exception {
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        Filter filter = searchBuilder.build();
        assertThat("returned filter attribute name", filter.getAttributeName(), equalTo("objectClass"));
        assertThat("returned filter attribute value", filter.getAssertionValue(), equalTo("*"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addBaseUrl_baseUrlIsNull_throwsIllegalArgumentException() throws Exception {
        ldapEndpointRepository.addBaseUrl(null);
    }

    @Test(expected = IllegalStateException.class)
    public void addBaseUrl_resultCodeNotSuccess_throwsIllegalStateException() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        when(ldapInterface.add(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.addBaseUrl(cloudBaseUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateCloudBaseUrl_cloudBaseUrlIsNull_throwsIllegalArgumentException() throws Exception {
        spy.updateCloudBaseUrl(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateCloudBaseUrl_cloudBaseUrlUniqueIdIsBlank_throwsIllegalArgumentException() throws Exception {
        spy.updateCloudBaseUrl(new CloudBaseUrl());
    }
}
