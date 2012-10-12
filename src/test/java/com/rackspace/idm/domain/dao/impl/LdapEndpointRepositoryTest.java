package com.rackspace.idm.domain.dao.impl;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/3/12
 * Time: 12:21 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapEndpointRepositoryTest extends InMemoryLdapIntegrationTest{
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
    public void getAppConnPool_callsConnectionPoolMethod() throws Exception {
        ldapEndpointRepository.getAppConnPool();
        verify(connPools).getAppConnPool();
    }

    @Test
    public void getAppInterface_callsConnectionPoolMethod() throws Exception {
        ldapEndpointRepository.getAppInterface();
        verify(connPools).getAppConnPool();
    }

    @Test
    public void getBindConnPool_callsConnectionPool() throws Exception {
        ldapEndpointRepository.getBindConnPool();
        verify(connPools).getBindConnPool();
    }

    @Test
    public void addEntry_throwsNoExceptions_callsAddMethod() throws Exception {
        Audit audit = mock(Audit.class);
        Attribute[] attributes = new Attribute[0];
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.addEntry("entryDn",attributes,audit);
        verify(ldapInterface).add("entryDn",attributes);
    }

    @Test
    public void addEntry_throwsLDAPException_throwsIllegalStateExceptionAndAuditFails() throws Exception {
        Audit audit = mock(Audit.class);
        try{
            Attribute[] attributes = new Attribute[0];
            doReturn(ldapInterface).when(spy).getAppInterface();
            doThrow(new LDAPException(ResultCode.INVALID_DN_SYNTAX)).when(ldapInterface).add("entryDn",attributes);
            spy.addEntry("entryDn",attributes,audit);
            assertTrue("should throw exception",false);
        }catch (IllegalStateException ex){
            verify(audit).fail();
        }
    }

    @Test
    public void deleteEntryAndSubtree_setsFilterAttributeToWildCard() throws Exception {
        Audit audit = mock(Audit.class);
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        SearchResult searchResult = new SearchResult(123,ResultCode.SUCCESS,"woohoo","matchedDN",new String[0],new ArrayList<SearchResultEntry>(),null,0,0,new Control[0]);
        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(eq("dn"),eq(SearchScope.ONE),argumentCaptor.capture(),eq(LdapEndpointRepository.ATTR_NO_ATTRIBUTES));
        spy.deleteEntryAndSubtree("dn",audit);
        assertThat("sets filter attribute",argumentCaptor.getValue(),equalTo("(objectClass=*)"));
    }

    @Test
    public void deleteEntryAndSubtree_SearchResultsExist_callsDeleteEntryAndSubtree() throws Exception {
        Audit audit = mock(Audit.class);
        ArrayList<SearchResultEntry> arrayList = new ArrayList<SearchResultEntry>();
        arrayList.add(new SearchResultEntry("dn",new Attribute[0]));
        SearchResult searchResult = new SearchResult(123,ResultCode.SUCCESS,"woohoo","matchedDN",new String[0],arrayList,null,0,0,new Control[0]);

        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(anyString(), eq(SearchScope.ONE), anyString(), eq(LdapEndpointRepository.ATTR_NO_ATTRIBUTES));
        doCallRealMethod().doNothing().when(spy).deleteEntryAndSubtree("dn", audit);

        spy.deleteEntryAndSubtree("dn",audit);
        verify(spy,times(2)).deleteEntryAndSubtree("dn",audit);
    }

    @Test
    public void deleteEntryAndSubtree_callsDelete() throws Exception {
        Audit audit = mock(Audit.class);
        SearchResult searchResult = new SearchResult(123,ResultCode.SUCCESS,"woohoo","matchedDN",new String[0],new ArrayList<SearchResultEntry>(),null,0,0,new Control[0]);

        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(eq("dn"), eq(SearchScope.ONE), anyString(), eq(LdapEndpointRepository.ATTR_NO_ATTRIBUTES));

        spy.deleteEntryAndSubtree("dn",audit);
        verify(ldapInterface).delete("dn");
    }

    @Test (expected = IllegalStateException.class)
    public void deleteEntryAndSubtree_throwsLDAPException_auditFailsAndThrowsIllegalStateException() throws Exception {
        Audit audit = mock(Audit.class);
        SearchResult searchResult = new SearchResult(123,ResultCode.SUCCESS,"woohoo","matchedDN",new String[0],new ArrayList<SearchResultEntry>(),null,0,0,new Control[0]);

        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(eq("dn"), eq(SearchScope.ONE), anyString(), eq(LdapEndpointRepository.ATTR_NO_ATTRIBUTES));
        doThrow(new LDAPException(ResultCode.INVALID_DN_SYNTAX)).when(ldapInterface).delete("dn");

        spy.deleteEntryAndSubtree("dn",audit);
        verify(audit).fail();
    }

    @Test
    public void getMultipleEntriesThreeParameters_doesNotThrowException_returnsSearchResultEntryList() throws Exception {
        Filter filter = Filter.createEqualityFilter("objectClass","filter");
        List<SearchResultEntry> searchResultList = new ArrayList<SearchResultEntry>();
        SearchResult searchResult = new SearchResult(123,ResultCode.SUCCESS,"woohoo","matchedDN",new String[0],searchResultList,null,0,0,new Control[0]);
        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(any(SearchRequest.class));
        assertThat("returns search result entry list", spy.getMultipleEntries("dn", SearchScope.SUB, filter), equalTo(searchResultList));
    }

    @Test
    public void getMultipleEntriesFourParameters_doesNotThrowException_returnsSearchResultEntryList() throws Exception {
        Filter filter = Filter.createEqualityFilter("objectClass","filter");
        List<SearchResultEntry> searchResultList = new ArrayList<SearchResultEntry>();
        SearchResult searchResult = new SearchResult(123,ResultCode.SUCCESS,"woohoo","matchedDN",new String[0],searchResultList,null,0,0,new Control[0]);
        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(any(SearchRequest.class));
        assertThat("returns search result entry list", spy.getMultipleEntries("dn", SearchScope.SUB, "sortAttribute", filter), equalTo(searchResultList));
    }

    @Test
    public void getMultipleEntriesFourParameters_setsRequestControls() throws Exception {
        ArgumentCaptor<SearchRequest> argumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        Filter filter = Filter.createEqualityFilter("objectClass","filter");
        List<SearchResultEntry> searchResultList = new ArrayList<SearchResultEntry>();
        SearchResult searchResult = new SearchResult(123,ResultCode.SUCCESS,"woohoo","matchedDN",new String[0],searchResultList,null,0,0,new Control[0]);
        doReturn(ldapInterface).when(spy).getAppInterface();
        doReturn(searchResult).when(ldapInterface).search(argumentCaptor.capture());
        spy.getMultipleEntries("dn", SearchScope.SUB, "sortAttribute", filter);
        ServerSideSortRequestControl control = (ServerSideSortRequestControl) argumentCaptor.getValue().getControls()[0];
        assertThat("returns search result entry list",control.getSortKeys()[0].getAttributeName(), equalTo("sortAttribute"));
    }

    @Test
    public void getMultipleEntriesFourParameters_throwsException_returnsEmptyArrayList() throws Exception {
        Filter filter = Filter.createEqualityFilter("objectClass","filter");
        doReturn(ldapInterface).when(spy).getAppInterface();
        doThrow(new LDAPSearchException(ResultCode.INVALID_DN_SYNTAX,"testing")).when(ldapInterface).search(any(SearchRequest.class));
        List<SearchResultEntry> list = spy.getMultipleEntries("dn", SearchScope.SUB, "sortAttribute", filter);
        assertThat("returns empty list", list.size(), equalTo(0));
    }

    @Test (expected = IllegalStateException.class)
    public void getSingleEntry_throwsLDAPException_throwsIllegalStateException() throws Exception {
        Filter searchFilter = Filter.createEqualityFilter("attributeName","assertionValue");
        doReturn(ldapInterface).when(spy).getAppInterface();
        String[] attributes = {"test"};
        doThrow(new LDAPSearchException(ResultCode.INVALID_DN_SYNTAX,"testing")).when(ldapInterface).searchForEntry("baseDN", SearchScope.SUB, searchFilter,attributes);
        spy.getSingleEntry("baseDN", SearchScope.SUB, searchFilter, attributes);
    }

    @Test
    public void updateEntry_throwsNoExceptions_callsModify() throws Exception {
        Audit audit = mock(Audit.class);
        List<Modification> list = new ArrayList<Modification>();
        doReturn(ldapInterface).when(spy).getAppInterface();
        spy.updateEntry("entryDn", list, audit);
        verify(ldapInterface).modify("entryDn",list);
    }

    @Test
    public void updateEntry_throwsLDAPException_throwsIllegalStateExceptionAndAuditFails() throws Exception {
        Audit audit = mock(Audit.class);
        try{
            List<Modification> list = new ArrayList<Modification>();
            doReturn(ldapInterface).when(spy).getAppInterface();
            doThrow(new LDAPException(ResultCode.INVALID_DN_SYNTAX)).when(ldapInterface).modify("entryDn",list);
            spy.updateEntry("entryDn", list, audit);
            assertTrue("should throw exception",false);
        } catch (IllegalStateException ex){
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
        doNothing().when(spy).addEntry(anyString(),any(Attribute[].class),any(Audit.class));
        spy.addContainer("parentUniqueId","name");
        verify(spy).addEntry(eq("cn=name,parentUniqueId"),any(Attribute[].class),any(Audit.class));
    }

    @Test
    public void addContainer_attributesListPopulated() throws Exception {
        ArgumentCaptor<Attribute[]> argumentCaptor = ArgumentCaptor.forClass(Attribute[].class);
        doNothing().when(spy).addEntry(anyString(),argumentCaptor.capture(),any(Audit.class));
        spy.addContainer("parentUniqueId","name");
        assertThat("list size",argumentCaptor.getValue().length,equalTo(2));
        assertThat("first attribute is Rackspace container",argumentCaptor.getValue()[0].getValue(),equalTo("rsContainer"));
        assertThat("second attribute is name",argumentCaptor.getValue()[1].getValue(),equalTo("name"));
    }

    @Test
    public void getContainer_callsGetSingleEntry() throws Exception {
        doReturn(new SearchResultEntry("uniqueId",new Attribute[0])).when(spy).getSingleEntry(eq("parentUniqueId"),eq(SearchScope.ONE), any(Filter.class));
        spy.getContainer("parentUniqueId","name");
        verify(spy).getSingleEntry(eq("parentUniqueId"),eq(SearchScope.ONE), any(Filter.class));
    }

    @Test
    public void getContainer_filterContainsCorrectAttributes() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        doReturn(new SearchResultEntry("uniqueId",new Attribute[0])).when(spy).getSingleEntry(eq("parentUniqueId"),eq(SearchScope.ONE), argumentCaptor.capture());
        spy.getContainer("parentUniqueId","name");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("filter attribute is rackspace container",filters[0].getAssertionValue(),equalTo("rsContainer"));
        assertThat("filter attribute is name",filters[1].getAssertionValue(),equalTo("name"));
    }

    @Test
    public void getContainer_returnsSearchResultEntry() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId",new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("parentUniqueId"),eq(SearchScope.ONE), any(Filter.class));
        assertThat("returns a search result entry",spy.getContainer("parentUniqueId","name"),equalTo(searchResultEntry));
    }

    @Test
    public void getNextId_callsGetSingleEntry() throws Exception {
        Attribute[] attributes = {new Attribute("rsId","123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn",attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.getNextId("type");
        verify(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.ONE), any(Filter.class));
    }

    @Test
    public void getNextId_filterHasCorrectAttributes() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        Attribute[] attributes = {new Attribute("rsId","123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn",attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.ONE), argumentCaptor.capture());
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), any(List.class))).thenReturn(new LDAPResult(1,ResultCode.SUCCESS));
        spy.getNextId("type");
        Filter[] filters = argumentCaptor.getValue().getComponents();
        assertThat("first attribute vale",filters[0].getAssertionValue(),equalTo("rsNextId"));
        assertThat("second attribute value",filters[1].getAssertionValue(),equalTo("type"));
    }

    @Test
    public void getNextId_modificationListHasCorrectDeleteModification() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Attribute[] attributes = {new Attribute("rsId","123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn",attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), argumentCaptor.capture())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.getNextId("type");
        List<Modification> mods = argumentCaptor.getValue();
        assertThat("first modification deletes",mods.get(0).getModificationType(),equalTo(ModificationType.DELETE));
        assertThat("first modification acts on id",mods.get(0).getAttributeName(),equalTo("rsId"));
        assertThat("first modification acts on correct id value",mods.get(0).getAttribute().getValue(),equalTo("123"));
    }

    @Test
    public void getNextId_modificationListHasCorrectAddModification() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Attribute[] attributes = {new Attribute("rsId","123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn",attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), argumentCaptor.capture())).thenReturn(new LDAPResult(1,ResultCode.SUCCESS));
        spy.getNextId("type");
        List<Modification> mods = argumentCaptor.getValue();
        assertThat("second modification deletes",mods.get(1).getModificationType(),equalTo(ModificationType.ADD));
        assertThat("second modification acts on id",mods.get(1).getAttributeName(),equalTo("rsId"));
        assertThat("second modification acts on correct id value",mods.get(1).getAttribute().getValue(),equalTo("124"));
    }

    @Test
    public void getNextId_callsModify() throws Exception {
        Attribute[] attributes = {new Attribute("rsId","123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn",attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), any(List.class))).thenReturn(new LDAPResult(1,ResultCode.SUCCESS));
        spy.getNextId("type");
        verify(ldapInterface).modify(eq("testDn"), any(List.class));
    }

    @Test
    public void getNextId_throwsNoExceptions_returnsStringValue() throws Exception {
        Attribute[] attributes = {new Attribute("rsId","123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn",attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        when(ldapInterface.modify(eq("testDn"), any(List.class))).thenReturn(new LDAPResult(1,ResultCode.SUCCESS));
        assertThat("returns a string of the id", spy.getNextId("type"), equalTo("123"));
    }

    @Test (expected = IllegalStateException.class)
    public void getNextId_throwsLdapExceptionResultCodeNotNoSuchAttribute_throwsIllegalStateException() throws Exception {
        Attribute[] attributes = {new Attribute("rsId","123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn",attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        doThrow(new LDAPException(ResultCode.INVALID_DN_SYNTAX)).when(ldapInterface).modify(eq("testDn"), any(List.class));
        spy.getNextId("type");
    }

    @Test
    public void getNextId_throwsLdapExceptionResultCodeNoSuchAttribute_makesRecursiveCall() throws Exception {
        Attribute[] attributes = {new Attribute("rsId","123")};
        SearchResultEntry searchResultEntry = new SearchResultEntry("testDn",attributes);
        doReturn(searchResultEntry).when(spy).getSingleEntry(eq("ou=nextIds,o=rackspace,dc=rackspace,dc=com"),eq(SearchScope.ONE), any(Filter.class));
        doReturn(ldapInterface).when(spy).getAppInterface();
        doThrow(new LDAPException(ResultCode.NO_SUCH_ATTRIBUTE)).when(ldapInterface).modify(eq("testDn"), any(List.class));
        doCallRealMethod().doReturn("success").when(spy).getNextId("type");
        spy.getNextId("type");
        verify(spy,times(2)).getNextId("type");
    }

    @Test
    public void queryPairConstructor_attributeIsBlank_throwsIllegalArgumentException() throws Exception {
        try{
            new LdapRepository.QueryPair("","comparer","value");
            assertTrue("should throw exception",false);
        } catch (IllegalArgumentException ex){
            assertThat("has correct message",ex.getMessage(),equalTo("attribute cannot be empty"));
        }
    }

    @Test
    public void queryPairConstructor_comparerIsBlank_throwsIllegalArgumentException() throws Exception {
        try{
            new LdapRepository.QueryPair("attribute","","value");
            assertTrue("should throw exception",false);
        } catch (IllegalArgumentException ex){
            assertThat("has correct message",ex.getMessage(),equalTo("comparer cannot be empty"));
        }
    }

    @Test
    public void queryPairConstructor_valueIsBlank_throwsIllegalArgumentException() throws Exception {
        try{
            new LdapRepository.QueryPair("attribute","comparer","");
            assertTrue("should throw exception",false);
        } catch (IllegalArgumentException ex){
            assertThat("has correct message",ex.getMessage(),equalTo("value cannot be empty"));
        }
    }

    @Test
    public void addEqualAttribute_addsFilterToList() throws Exception {
        byte[] bytes = "attributeValue".getBytes();
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        LdapRepository.LdapSearchBuilder returnedValue = searchBuilder.addEqualAttribute("attributeName",bytes);
        assertThat("search builder has filter with correct attribute name", returnedValue.build().getAttributeName(),equalTo("attributeName"));
        assertThat("search builder has filter with correct attribute value", returnedValue.build().getAssertionValue(),equalTo("attributeValue"));
    }

    @Test
    public void addGreaterOrEqualAttribute_addsFilterToList() throws Exception {
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        LdapRepository.LdapSearchBuilder returnedValue = searchBuilder.addGreaterOrEqualAttribute("attributeName","attributeValue");
        assertThat("search builder has filter with correct attribute name", returnedValue.build().getAttributeName(),equalTo("attributeName"));
        assertThat("search builder has filter with correct attribute value", returnedValue.build().getAssertionValue(),equalTo("attributeValue"));
    }

    @Test
    public void build_filterListEmpty_returnsNewFilter() throws Exception {
        LdapRepository.LdapSearchBuilder searchBuilder = new LdapRepository.LdapSearchBuilder();
        Filter filter = searchBuilder.build();
        assertThat("returned filter attribute name",filter.getAttributeName(),equalTo("objectClass"));
        assertThat("returned filter attribute value",filter.getAssertionValue(),equalTo("*"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addBaseUrl_baseUrlIsNull_throwsIllegalArgumentException() throws Exception {
        ldapEndpointRepository.addBaseUrl(null);
    }

    @Test
    public void addBaseUrl_addsAllCloudBaseUrlAttributes_succeed() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setOpenstackType("openStackType");
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setGlobal(true);
        when(ldapInterface.add(anyString(), argumentCaptor.capture())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.addBaseUrl(cloudBaseUrl);
        List<Attribute> result = argumentCaptor.getValue();
        assertThat("admin url", result.get(1).getValue(), equalTo("adminUrl"));
        assertThat("base url type", result.get(2).getValue(), equalTo("baseUrlType"));
        assertThat("base url id", result.get(3).getValue(), equalTo("1"));
        assertThat("internal url", result.get(4).getValue(), equalTo("internalUrl"));
        assertThat("publie url", result.get(5).getValue(), equalTo("publicUrl"));
        assertThat("region", result.get(6).getValue(), equalTo("region"));
        assertThat("service name", result.get(7).getValue(), equalTo("serviceName"));
        assertThat("def", result.get(8).getValue(), equalTo("true"));
        assertThat("enabled", result.get(9).getValue(), equalTo("true"));
        assertThat("open stack type", result.get(10).getValue(), equalTo("openStackType"));
        assertThat("version id", result.get(11).getValue(), equalTo("versionId"));
        assertThat("version info", result.get(12).getValue(), equalTo("versionInfo"));
        assertThat("version list", result.get(13).getValue(), equalTo("versionList"));
        assertThat("global", result.get(14).getValue(), equalTo("true"));
    }

    @Test (expected = IllegalStateException.class)
    public void addBaseUrl_callsLDAPInterfaceAdd_throwsIllegalStateException() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setBaseUrlId(0);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setOpenstackType("openStackType");
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.toString();
        cloudBaseUrl.getAuditContext();
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).add(anyString(), any(List.class));
        spy.addBaseUrl(cloudBaseUrl);
    }

    @Test (expected = IllegalStateException.class)
    public void addBaseUrl_resultCodeNotSuccess_throwsIllegalStateException() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        when(ldapInterface.add(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.addBaseUrl(cloudBaseUrl);
    }

    @Test (expected = NotFoundException.class)
    public void addBaseUrlToUser_baseUrlIsNull_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getBaseUrlById(1);
        spy.addBaseUrlToUser(1, false, "username");
    }

    @Test (expected = BaseUrlConflictException.class)
    public void addBaseUrlToUser_endpointStringContainsDashSign_throwsBaseUrlConflictException() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("-1");
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        spy.addBaseUrlToUser(1, true, "username");
    }

    @Test (expected = BaseUrlConflictException.class)
    public void addBaseUrlToUser_endpointStringContainsPlusSign_throwsBaseUrlConflictException() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("+1");
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        spy.addBaseUrlToUser(1, false, "username");
    }

    @Test (expected = IllegalStateException.class)
    public void addBaseUrlToUser_callsLDAPInterfaceModify_throwsLDAPException() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("notExisting");
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(List.class));
        spy.addBaseUrlToUser(1, false, "username");
    }

    @Test (expected = IllegalArgumentException.class)
    public void addBaseUrlToUser_resultCodeNotSuccess_throwsIllegalArgumentException() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.addBaseUrlToUser(1, false, "username");
    }

    @Test
    public void addBaseUrlToUser_resultCodeSuccess_urlAddedSuccessfully() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("notExisting");
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.addBaseUrlToUser(1, false, "username");
        verify(ldapInterface).modify(anyString(), any(List.class));
    }

    @Test (expected = IllegalStateException.class)
    public void deleteBaseUrl_callsLDAPInterfaceDelete_throwsLDAPException() throws Exception {
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).delete(anyString());
        spy.deleteBaseUrl(1);
    }

    @Test (expected = IllegalStateException.class)
    public void deleteBaseUrl_resultCodeNotSuccess_throwsIllegalStateException() throws Exception {
        when(ldapInterface.delete(anyString())).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.deleteBaseUrl(1);
    }

    @Test
    public void deleteBaseUrl_resultCodeSuccess_deletesSuccessfully() throws Exception {
        when(ldapInterface.delete(anyString())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.deleteBaseUrl(1);
    }

    @Test
    public void getBaseUrlById_returnsBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(cloudBaseUrl).when(spy).getBaseUrl(searchResultEntry);
        CloudBaseUrl result = spy.getBaseUrlById(1);
        assertThat("cloud base url", result, equalTo(cloudBaseUrl));
    }



    @Test
    public void getBaseUrlsByService_foundBaseUrl_returnsList() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(cloudBaseUrl).when(spy).getBaseUrl(searchResultEntry);
        List<CloudBaseUrl> result = spy.getBaseUrlsByService("service");
        assertThat("cloud base url", result.get(0), equalTo(cloudBaseUrl));
    }

    @Test
    public void getBaseUrlsByService_noEntryFound_returnsEmptyList() throws Exception {
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class));
        List<CloudBaseUrl> result = spy.getBaseUrlsByService("service");
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getBaseUrls_foundBaseUrl_returnsList() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.hashCode();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(cloudBaseUrl).when(spy).getBaseUrl(searchResultEntry);
        List<CloudBaseUrl> result = spy.getBaseUrls();
        assertThat("cloud base url", result.get(0), equalTo(cloudBaseUrl));
    }

    @Test
    public void getBaseUrls_noEntryFound_returnsEmptyList() throws Exception {
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class));
        List<CloudBaseUrl> result = spy.getBaseUrls();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getEndpointsForUser_endPointIsNull_returnsNull() throws Exception {
        doReturn(null).when(spy).getRawEndpointsForUser("username");
        List<CloudEndpoint> result = spy.getEndpointsForUser("username");
        assertThat("list", result, equalTo(null));
    }

    @Test
    public void getEndpointsForUser_endPointsSizeIsZero_returnsNull() throws Exception {
        endPoints.setEndpoints(new ArrayList<String>());
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        List<CloudEndpoint> result = spy.getEndpointsForUser("username");
        assertThat("list", result, equalTo(null));
    }

    @Test
    public void getEndpointsForUser_baseUrlNotNull_setsUpCloudEndpoint() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        List<String> endPointList = new ArrayList<String>();
        endPointList.add("+1");
        endPoints.setEndpoints(endPointList);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(1);
        List<CloudEndpoint> result = spy.getEndpointsForUser("username");
        assertThat("hash code", result.get(0).hashCode(), equalTo(1998524901));
        assertThat("cloud endpoint", result.get(0).toString(), equalTo("CloudEndpoint [v1preferred=true, baseUrl=baseUrl=null, username=username, nastId=nastId, mossoId=1]"));
        assertThat("cloud base url", result.get(0).getBaseUrl(), equalTo(cloudBaseUrl));
        assertThat("mosso id", result.get(0).getMossoId(), equalTo(1));
        assertThat("username", result.get(0).getUsername(), equalTo("username"));
        assertThat("nast id", result.get(0).getNastId(), equalTo("nastId"));
        assertThat("v1 preferred", result.get(0).isV1preferred(), equalTo(true));
    }

    @Test
    public void getEndpoitnsForUser_baseUrlIsNull_returnsEmptyList() throws Exception {
        List<String> endPointList = new ArrayList<String>();
        endPointList.add("+1");
        endPoints.setEndpoints(endPointList);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doReturn(null).when(spy).getBaseUrlById(1);
        List<CloudEndpoint> result = spy.getEndpointsForUser("username");
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getOpenstackEndpointsForTenant_baseUrlIdIsNull_setsEmptyBaseUrlList() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName("name");
        tenant.setTenantId("tenantId");
        OpenstackEndpoint result = spy.getOpenstackEndpointsForTenant(tenant);
        assertThat("baseurl list", result.getBaseUrls().isEmpty(), equalTo(true));
    }

    @Test
    public void getOpenstackEndpointsForTenant_addsBaseUrlToList_setsBaseUrlList() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        String[] baseUrlIds = new String[2];
        baseUrlIds[0] = "1";
        baseUrlIds[1] = "2";
        Tenant tenant = new Tenant();
        tenant.setName("name");
        tenant.setTenantId("tenantId");
        tenant.setBaseUrlIds(baseUrlIds);
        doReturn(null).when(spy).getBaseUrlById(1);
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(2);
        OpenstackEndpoint result = spy.getOpenstackEndpointsForTenant(tenant);
        assertThat("tenant name", result.getTenantName(), equalTo("name"));
        assertThat("tenant id", result.getTenantId(), equalTo("tenantId"));
        assertThat("base url", result.getBaseUrls().get(0), equalTo(cloudBaseUrl));
        assertThat("list", result.getBaseUrls().size(), equalTo(1));
    }

    @Test
    public void appendTenantToBaseUrl_setsAllUrlEndWithSlashWithTenantId() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setPublicUrl("publicUrl/");
        cloudBaseUrl.setAdminUrl("adminUrl/");
        cloudBaseUrl.setInternalUrl("internalUrl/");
        spy.appendTenantToBaseUrl("1", cloudBaseUrl);
        assertThat("public url", cloudBaseUrl.getPublicUrl(), equalTo("publicUrl/1"));
        assertThat("admin url", cloudBaseUrl.getAdminUrl(), equalTo("adminUrl/1"));
        assertThat("internal url", cloudBaseUrl.getInternalUrl(), equalTo("internalUrl/1"));
    }

    @Test
    public void appendTenantToBaseUrl_setsAllUrlWithTenantIdAlsoAddsSlashAtEnd() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setInternalUrl("internalUrl");
        spy.appendTenantToBaseUrl("1", cloudBaseUrl);
        assertThat("public url", cloudBaseUrl.getPublicUrl(), equalTo("publicUrl/1"));
        assertThat("admin url", cloudBaseUrl.getAdminUrl(), equalTo("adminUrl/1"));
        assertThat("internal url", cloudBaseUrl.getInternalUrl(), equalTo("internalUrl/1"));
    }

    @Test (expected = NotFoundException.class)
    public void removeBaseUrlFromUser_baseUrlIsNull_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getBaseUrlById(1);
        spy.removeBaseUrlFromUser(1, "username");
    }

    @Test
    public void removeBaseUrlFromUser_endpointSizeIsZero_returns() throws Exception {
        endPoints.setEndpoints(new ArrayList<String>());
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        spy.removeBaseUrlFromUser(1, "username");
        verify(spy).getRawEndpointsForUser("username");
    }

    @Test
    public void removeBaseUrlFromUser_addsDeleteMod() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        ArrayList<String> endpointsList = new ArrayList<String>();
        endpointsList.add("+1");
        endPoints.setEndpoints(endpointsList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doNothing().when(spy).updateEntry(eq("userDn"), argumentCaptor.capture(), any(Audit.class));
        spy.removeBaseUrlFromUser(1, "username");
        List<Modification> result = argumentCaptor.getValue();
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void removeBaseUrlFromUser_addsReplaceMod() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        ArrayList<String> endpointsList = new ArrayList<String>();
        endpointsList.add("+2");
        endPoints.setEndpoints(endpointsList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doNothing().when(spy).updateEntry(eq("userDn"), argumentCaptor.capture(), any(Audit.class));
        spy.removeBaseUrlFromUser(1, "username");
        List<Modification> result = argumentCaptor.getValue();
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test (expected = NotFoundException.class)
    public void setBaseUrlEnabled_baseUrlIsNull_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getBaseUrlById(1);
        spy.setBaseUrlEnabled(1, true);
    }

    @Test
    public void setBaseUrlEnabled_callsUpdateEntry() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setUniqueId("uniqueId");
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(1);
        doNothing().when(spy).updateEntry(eq("uniqueId"), any(List.class), any(Audit.class));
        spy.setBaseUrlEnabled(1, true);
        verify(spy).updateEntry(eq("uniqueId"), any(List.class), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateCloudBaseUrl_cloudBaseUrlIsNull_throwsIllegalArgumentException() throws Exception {
        spy.updateCloudBaseUrl(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateCloudBaseUrl_cloudBaseUrlUniqueIdIsBlank_throwsIllegalArgumentException() throws Exception {
        spy.updateCloudBaseUrl(new CloudBaseUrl());
    }

    @Test
    public void updateCloudBaseUrl_modificationSizeIsZero_doesNotUpdate() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setUniqueId("uniqueId");
        cloudBaseUrl.setBaseUrlId(1);
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(1);
        doReturn(new ArrayList<Modification>()).when(spy).getModifications(cloudBaseUrl, cloudBaseUrl);
        spy.updateCloudBaseUrl(cloudBaseUrl);
        verify(spy, never()).updateEntry(anyString(), any(List.class), any(Audit.class));
    }

    @Test
    public void updateCloudBaseUrl_modificationSizeMoreThanZero_callsUpdateEntry() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setUniqueId("uniqueId");
        cloudBaseUrl.setBaseUrlId(1);
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(1);
        Modification modification = new Modification(ModificationType.ADD, "add");
        List<Modification> modifications = new ArrayList<Modification>();
        modifications.add(modification);
        doReturn(modifications).when(spy).getModifications(cloudBaseUrl, cloudBaseUrl);
        doNothing().when(spy).updateEntry(anyString(), eq(modifications), any(Audit.class));
        spy.updateCloudBaseUrl(cloudBaseUrl);
        verify(spy).updateEntry(anyString(), eq(modifications), any(Audit.class));
    }

    @Test
    public void getBaseUrl_setsAttributeForBaseUrl_returnsCloudBaseUrl() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        CloudBaseUrl result = spy.getBaseUrl(searchResultEntry);
        assertThat("cloud base url", result.toString(), equalTo("baseUrl=null"));
    }

    @Test
    public void getBaseUrl_WithNullSearchResult_returnsNull() throws Exception {
        CloudBaseUrl result = spy.getBaseUrl(null);
        assertThat("cloud base url", result, nullValue());
    }

    @Test (expected = NotFoundException.class)
    public void getRawEndpointsForUser_searchResultIsNull_throwsNotFoundException() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_UID, "username")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
                .build();
        doReturn(null).when(spy).getSingleEntry(LdapRepository.BASE_DN, SearchScope.SUB, searchFilter, new String[]{LdapRepository.ATTR_ENDPOINT, LdapRepository.ATTR_UID, LdapRepository.ATTR_NAST_ID, LdapRepository.ATTR_MOSSO_ID});
        spy.getRawEndpointsForUser("username");
    }

    @Test
    public void getRawEndpointsForUser_listIsNull_setsEmptyListToEndpoints() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_UID, "username")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
                .build();
        doReturn(searchResultEntry).when(spy).getSingleEntry(LdapRepository.BASE_DN, SearchScope.SUB, searchFilter, new String[]{LdapRepository.ATTR_ENDPOINT, LdapRepository.ATTR_UID, LdapRepository.ATTR_NAST_ID, LdapRepository.ATTR_MOSSO_ID});
        EndPoints result = spy.getRawEndpointsForUser("username");
        assertThat("list", result.getEndpoints().isEmpty(), equalTo(true));
    }

    @Test
    public void getRawEndpointsForUser_listLengthIsMoreThanZero_setsListToEndpoints() throws Exception {
        Attribute[] attributes = new Attribute[1];
        attributes[0] = new Attribute(LdapRepository.ATTR_ENDPOINT, "endpoint");
        SearchResultEntry searchResultEntry = new SearchResultEntry("", attributes);
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_UID, "username")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
                .build();
        doReturn(searchResultEntry).when(spy).getSingleEntry(LdapRepository.BASE_DN, SearchScope.SUB, searchFilter, new String[]{LdapRepository.ATTR_ENDPOINT, LdapRepository.ATTR_UID, LdapRepository.ATTR_NAST_ID, LdapRepository.ATTR_MOSSO_ID});
        EndPoints result = spy.getRawEndpointsForUser("username");
        assertThat("list", result.getEndpoints().get(0), equalTo("endpoint"));
    }

    @Test
    public void getModifications_addsReplaceModOfDef() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setDef(true);
        oCloud.setDef(false);
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_DEF));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfAdminUrl() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setDef(true);
        oCloud.setDef(true);
        nCloud.setAdminUrl("new");
        oCloud.setAdminUrl("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_ADMIN_URL));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfBaseUrlType() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setAdminUrl("new");
        oCloud.setAdminUrl("new");
        nCloud.setBaseUrlType("new");
        oCloud.setBaseUrlType("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_BASEURL_TYPE));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfEnabled() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setBaseUrlType("new");
        oCloud.setBaseUrlType("new");
        nCloud.setEnabled(true);
        oCloud.setEnabled(false);
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_ENABLED));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfGlobal() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setEnabled(true);
        oCloud.setEnabled(true);
        nCloud.setGlobal(true);
        oCloud.setGlobal(false);
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_GLOBAL));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfInternalUrl() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setGlobal(true);
        oCloud.setGlobal(true);
        nCloud.setInternalUrl("new");
        oCloud.setInternalUrl("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_INTERNAL_URL));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfPublicUrl() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setInternalUrl("new");
        oCloud.setInternalUrl("new");
        nCloud.setPublicUrl("new");
        oCloud.setPublicUrl("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_PUBLIC_URL));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfRegion() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setPublicUrl("new");
        oCloud.setPublicUrl("new");
        nCloud.setRegion("new");
        oCloud.setRegion("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_REGION));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfServiceName() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setRegion("new");
        oCloud.setRegion("new");
        nCloud.setServiceName("new");
        oCloud.setServiceName("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_SERVICE));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfOpenStackType() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setServiceName("new");
        oCloud.setServiceName("new");
        nCloud.setOpenstackType("new");
        oCloud.setOpenstackType("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_OPENSTACK_TYPE));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_openStackTypeMatch_setsNothing() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setOpenstackType("new");
        oCloud.setOpenstackType("new");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getModifications_notModsAddedToList_returnsEmptyList() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("list", result.isEmpty(), equalTo(true));
    }
}
