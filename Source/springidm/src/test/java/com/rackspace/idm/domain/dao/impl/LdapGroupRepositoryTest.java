package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/29/12
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapGroupRepositoryTest {
    LdapGroupRepository ldapGroupRepository;
    LdapGroupRepository spy;
    LDAPInterface ldapInterface;

    @Before
    public void setUp() throws Exception {
        ldapGroupRepository = new LdapGroupRepository(mock(LdapConnectionPools.class), mock(Configuration.class));

        ldapInterface = mock(LDAPInterface.class);

        spy = spy(ldapGroupRepository);

        doReturn(ldapInterface).when(spy).getAppInterface();
    }

    @Test (expected = IllegalStateException.class)
    public void getGroups_callsLDAPInterfaceSearch_throwsLDAPSearchException() throws Exception {
        doThrow(new LDAPSearchException(ResultCode.LOCAL_ERROR, "error")).when(ldapInterface).search(anyString(), any(SearchScope.class), any(Filter.class));
        spy.getGroups("marker", 1);
    }

    @Test
    public void getGroups_foundResultsAndAddGroups_returnGroupList() throws Exception {
        Group group = new Group();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 1, 1, new Control[0]);
        doReturn(group).when(spy).getGroup(searchResultEntry);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        List<Group> result = spy.getGroups("marker", 1);
        assertThat("group", result.get(0), equalTo(group));
    }

    @Test
    public void getGroups_searchResultEntryCountIsZero_returnsEmptyGroupList() throws Exception {
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], 0, 1, new Control[0]);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        List<Group> result = spy.getGroups("marker", 1);
        assertThat("group", result.isEmpty(), equalTo(true));
    }

    @Test (expected = IllegalStateException.class)
    public void getGroupById_callsLDAPInterfaceSearch_throwsLDAPSearchException() throws Exception {
        doThrow(new LDAPSearchException(ResultCode.LOCAL_ERROR, "error")).when(ldapInterface).search(anyString(), any(SearchScope.class), any(Filter.class));
        spy.getGroupById(1);
    }

    @Test (expected = IllegalStateException.class)
    public void getGroupById_foundMoreThanOneGroup_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 2, 1, new Control[0]);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        spy.getGroupById(1);
    }

    @Test (expected = NotFoundException.class)
    public void getGroupById_foundZeroGroup_throwsNotFoundException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 0, 1, new Control[0]);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        spy.getGroupById(1);
    }

    @Test
    public void getGroupById_foundOneGroup_returnsGroup() throws Exception {
        Group group = new Group();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 1, 1, new Control[0]);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        doReturn(group).when(spy).getGroup(searchResultEntry);
        Group result = spy.getGroupById(1);
        assertThat("group", result, equalTo(group));
    }
}
