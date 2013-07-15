package com.rackspace.idm.domain.dao.impl;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
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
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapGroupRepositoryTest extends InMemoryLdapIntegrationTest{
    @InjectMocks
    LdapGroupRepository ldapGroupRepository = new LdapGroupRepository();
    @Mock
    LdapConnectionPools ldapConnectionPools;
    @Mock
    Configuration configuration;

    LdapGroupRepository spy;
    LDAPInterface ldapInterface;

    @Before
    public void setUp() throws Exception {
        ldapInterface = mock(LDAPInterface.class);

        spy = spy(ldapGroupRepository);

        doReturn(ldapInterface).when(spy).getAppInterface();
    }

    @Test (expected = IllegalStateException.class)
    public void getGroups_callsLDAPInterfaceSearch_throwsLDAPSearchException() throws Exception {
        doThrow(new LDAPSearchException(ResultCode.LOCAL_ERROR, "error")).when(ldapInterface).search(anyString(), any(SearchScope.class), any(Filter.class));
        spy.getGroups();
    }

    @Test
    public void getGroups_searchResultEntryCountIsZero_returnsEmptyGroupList() throws Exception {
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], 0, 1, new Control[0]);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        List<Group> result = spy.getGroups();
        assertThat("group", result.isEmpty(), equalTo(true));
    }

    @Test (expected = IllegalStateException.class)
    public void getGroupByName_callsLDAPInterfaceSearch_throwsLDAPSearchException() throws Exception {
        doThrow(new LDAPSearchException(ResultCode.LOCAL_ERROR, "error")).when(ldapInterface).search(anyString(), any(SearchScope.class), any(Filter.class));
        spy.getGroupByName("groupName");
    }

    @Test
    public void getGroupByName_foundZeroGroup_returnsNull() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 0, 1, new Control[0]);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        Group result = spy.getGroupByName("groupName");
        assertThat("group", result, equalTo(null));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addGroup_groupIsNull_throwsIllegalArgument() throws Exception {
        ldapGroupRepository.addGroup(null);
    }

    @Test
    public void getNextGroupId_callsGetNextId() throws Exception {
        doReturn("nextId").when(spy).getNextId("nextGroupId");
        spy.getNextGroupId();
        verify(spy).getNextId("nextGroupId");
    }
}
