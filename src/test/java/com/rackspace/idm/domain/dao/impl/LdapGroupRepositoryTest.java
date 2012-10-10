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
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.security.GeneralSecurityException;
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

    @Test (expected = IllegalStateException.class)
    public void getGroupByName_callsLDAPInterfaceSearch_throwsLDAPSearchException() throws Exception {
        doThrow(new LDAPSearchException(ResultCode.LOCAL_ERROR, "error")).when(ldapInterface).search(anyString(), any(SearchScope.class), any(Filter.class));
        spy.getGroupByName("groupName");
    }

    @Test (expected = IllegalStateException.class)
    public void getGroupByName_foundMoreThanOneGroup_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 2, 1, new Control[0]);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
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

    @Test
    public void getGroupByName_foundOneGroup_returnsGroup() throws Exception {
        Group group = new Group();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 1, 1, new Control[0]);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        doReturn(group).when(spy).getGroup(searchResultEntry);
        Group result = spy.getGroupByName("groupName");
        assertThat("group", result, equalTo(group));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addGroup_groupIsNull_throwsIllegalArgument() throws Exception {
        ldapGroupRepository.addGroup(null);
    }

    @Test
    public void addGroup_callsAddEntry() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        doReturn(new Attribute[0]).when(spy).getAddAttributes(group);
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addGroup(group);
        verify(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
    }

    @Test
    public void updateGroup_modSizeLessThanOne_returns() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        List<Modification> mods = new ArrayList<Modification>();
        doReturn(group).when(spy).getGroupById(1);
        doReturn(mods).when(spy).getModifications(group, group);
        spy.updateGroup(group);
        verify(spy).getModifications(group, group);
    }

    @Test (expected = IllegalStateException.class)
    public void updateGroup_callsLDAPInterfaceModify_throwsLDAPException() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        group.setUniqueId("uniqueId");
        Modification modification = new Modification(ModificationType.ADD, "attr");
        List<Modification> mods = new ArrayList<Modification>();
        mods.add(modification);
        doReturn(group).when(spy).getGroupById(1);
        doReturn(mods).when(spy).getModifications(group, group);
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(List.class));
        spy.updateGroup(group);
    }

    @Test
    public void updateGroup_callsLDAPInterface_modify() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        group.setUniqueId("uniqueId");
        Modification modification = new Modification(ModificationType.ADD, "attr");
        List<Modification> mods = new ArrayList<Modification>();
        mods.add(modification);
        doReturn(group).when(spy).getGroupById(1);
        doReturn(mods).when(spy).getModifications(group, group);
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(null);
        spy.updateGroup(group);
        verify(ldapInterface).modify(anyString(), any(List.class));
    }

    @Test (expected = IllegalStateException.class)
    public void deleteGroup_callsLDAPInterfaceDelete_throwsLDAPException() throws Exception {
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).delete(anyString());
        spy.deleteGroup(1);
    }

    @Test (expected = IllegalStateException.class)
    public void deleteGroup_resultCodeIsNotSuccess_throwsIllegalStateException() throws Exception {
        when(ldapInterface.delete(anyString())).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.deleteGroup(1);
    }

    @Test
    public void deleteGroup_deletesGroup_resultCodeSuccess() throws Exception {
        when(ldapInterface.delete(anyString())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.deleteGroup(1);
    }

    @Test
    public void getNextGroupId_callsGetNextId() throws Exception {
        doReturn("nextId").when(spy).getNextId("nextGroupId");
        spy.getNextGroupId();
        verify(spy).getNextId("nextGroupId");
    }

    @Test (expected = NotFoundException.class)
    public void addGroupToUser_callsGetGroupById_throwsNotFoundException() throws Exception {
        doThrow(new NotFoundException()).when(spy).getGroupById(1);
        spy.addGroupToUser(1, "userId");
    }

    @Test (expected = BadRequestException.class)
    public void addGroupToUser_callsGetGroupsForUser_throwsBadRequestException() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        doReturn(group).when(spy).getGroupById(1);
        doThrow(new BadRequestException()).when(spy).getGroupsForUser("userId");
        spy.addGroupToUser(1, "userId");
    }

    @Test (expected = IllegalStateException.class)
    public void addGroupToUser_callsLDAPInterfaceModify_throwsLDAPException() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        List<Group> oldGroups = new ArrayList<Group>();
        doReturn(group).when(spy).getGroupById(1);
        doReturn(oldGroups).when(spy).getGroupsForUser("userId");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(List.class));
        spy.addGroupToUser(1, "userId");
    }

    @Test (expected = IllegalArgumentException.class)
    public void addGroupToUser_resultCodeNotSuccess_throwsIllegalArgumentException() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        List<Group> oldGroups = new ArrayList<Group>();
        oldGroups.add(group);
        doReturn(group).when(spy).getGroupById(1);
        doReturn(oldGroups).when(spy).getGroupsForUser("userId");
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.addGroupToUser(1, "userId");
    }

    @Test
    public void addGroupToUser_groupAdded_resultCodeSuccess() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        List<Group> oldGroups = new ArrayList<Group>();
        oldGroups.add(group);
        doReturn(group).when(spy).getGroupById(1);
        doReturn(oldGroups).when(spy).getGroupsForUser("userId");
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.addGroupToUser(1, "userId");
    }

    @Test (expected = NotFoundException.class)
    public void deleteGroupFromUser_groupIsNull_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getGroupById(1);
        spy.deleteGroupFromUser(1, "userId");
    }

    @Test (expected = BadRequestException.class)
    public void deleteGroupFromUser_callsGetGroupsForUser_throwsBadRequest() throws Exception {
        Group group = new Group();
        doReturn(group).when(spy).getGroupById(1);
        doThrow(new BadRequestException()).when(spy).getGroupsForUser("userId");
        spy.deleteGroupFromUser(1, "userId");
    }

    @Test
    public void deleteGroupFromUser_groupsSizeIsOneOrMore_addsReplaceMod() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Group group = new Group();
        group.setGroupId(1);
        Group anotherGroup = new Group();
        anotherGroup.setGroupId(2);
        List<Group> oldGroups = new ArrayList<Group>();
        oldGroups.add(group);
        oldGroups.add(anotherGroup);
        doReturn(group).when(spy).getGroupById(1);
        doReturn(oldGroups).when(spy).getGroupsForUser("userId");
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.deleteGroupFromUser(1, "userId");
        verify(ldapInterface).modify(anyString(), argumentCaptor.capture());
        List<Modification> result = argumentCaptor.getValue();
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void deleteGroupFromUser_groupsSizeLessThanOne_addsDeleteMod() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Group group = new Group();
        group.setGroupId(1);
        List<Group> oldGroups = new ArrayList<Group>();
        doReturn(group).when(spy).getGroupById(1);
        doReturn(oldGroups).when(spy).getGroupsForUser("userId");
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.deleteGroupFromUser(1, "userId");
        verify(ldapInterface).modify(anyString(), argumentCaptor.capture());
        List<Modification> result = argumentCaptor.getValue();
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test (expected = IllegalStateException.class)
    public void deleteGroupFromUser_callsLdapInterfaceModify_throwsLDAPException() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        List<Group> oldGroups = new ArrayList<Group>();
        doReturn(group).when(spy).getGroupById(1);
        doReturn(oldGroups).when(spy).getGroupsForUser("userId");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(List.class));
        spy.deleteGroupFromUser(1, "userId");
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteGroupFromUser_resultCodeNotSuccess_throwsIllegalArgumentException() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        List<Group> oldGroups = new ArrayList<Group>();
        doReturn(group).when(spy).getGroupById(1);
        doReturn(oldGroups).when(spy).getGroupsForUser("userId");
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.deleteGroupFromUser(1, "userId");
    }

    @Test (expected = IllegalStateException.class)
    public void getGroupsForUser_callsLdapInterfaceSearch_throwsLDAPSearchException() throws Exception {
        doThrow(new LDAPSearchException(ResultCode.LOCAL_ERROR, "error")).when(ldapInterface).search(anyString(),  any(SearchScope.class), any(Filter.class), any(String[].class));
        spy.getGroupsForUser("userId");
    }

    @Test (expected = NotFoundException.class)
    public void getGroupsForUser_entryCountIsZero_throwsNotFoundException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.LOCAL_ERROR;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 0, 1, new Control[0]);
        when(ldapInterface.search(anyString(),  any(SearchScope.class), any(Filter.class), any(String[].class))).thenReturn(searchResult);
        spy.getGroupsForUser("userId");
    }

    @Test (expected = IllegalStateException.class)
    public void getGroupsForUser_entryCountIsMoreThanOne_throwsIllegalStateException() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.LOCAL_ERROR;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 2, 1, new Control[0]);
        when(ldapInterface.search(anyString(),  any(SearchScope.class), any(Filter.class), any(String[].class))).thenReturn(searchResult);
        spy.getGroupsForUser("userId");
    }

    @Test
    public void getGroupsForUser_listIsNull_returnsEmptyGroupList() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.LOCAL_ERROR;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 1, 1, new Control[0]);
        when(ldapInterface.search(anyString(),  any(SearchScope.class), any(Filter.class), any(String[].class))).thenReturn(searchResult);
        List<Group> result = spy.getGroupsForUser("userId");
        assertThat("group list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getGroupsForUser_addedMissingGroup_returnsMissingGroup() throws Exception {
        Attribute attribute = new Attribute(LdapRepository.ATTR_GROUP_ID, "1");
        Attribute[] attributes = new Attribute[1];
        attributes[0] = attribute;
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", attributes);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 1, 1, new Control[0]);
        when(ldapInterface.search(anyString(),  any(SearchScope.class), any(Filter.class), any(String[].class))).thenReturn(searchResult);
        doThrow(new NotFoundException()).when(spy).getGroupById(1);
        List<Group> result = spy.getGroupsForUser("2");
        assertThat("group id", result.get(0).getGroupId(), equalTo(1));
    }

    @Test
    public void getGroupsForUser_foundGroupForUser_returnsGroup() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        Attribute attribute = new Attribute(LdapRepository.ATTR_GROUP_ID, "1");
        Attribute[] attributes = new Attribute[2];
        attributes[0] = attribute;
        attributes[1] = attribute;
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", attributes);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        ResultCode resultCode = ResultCode.SUCCESS;
        SearchResult searchResult = new SearchResult(1, resultCode, "diag", "match", new String[0], searchResultEntryList, null, 1, 1, new Control[0]);
        when(ldapInterface.search(anyString(),  any(SearchScope.class), any(Filter.class), any(String[].class))).thenReturn(searchResult);
        doReturn(group).when(spy).getGroupById(1);
        List<Group> result = spy.getGroupsForUser("2");
        assertThat("group id", result.get(0).getGroupId(), equalTo(1));
    }

    @Test
    public void getGroup_setsGroupAttributes_returnsGroup() throws Exception {
        Attribute[] attributes = new Attribute[3];
        attributes[0] = new Attribute(LdapRepository.ATTR_ID, "1");
        attributes[1] = new Attribute(LdapRepository.ATTR_GROUP_NAME, "groupName");
        attributes[2] = new Attribute(LdapRepository.ATTR_DESCRIPTION, "description");
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", attributes);
        Group result = ldapGroupRepository.getGroup(searchResultEntry);
        assertThat("unique id", result.getUniqueId(), equalTo("uniqueId"));
        assertThat("group id", result.getGroupId(), equalTo(1));
        assertThat("group name", result.getName(), equalTo("groupName"));
        assertThat("group description", result.getDescription(), equalTo("description"));
    }

    @Test
    public void getAddAttributes_addsAllAttributes_returnsArray() throws Exception {
        Group group = new Group();
        group.setGroupId(1);
        group.setDescription("description");
        group.setName("groupName");
        Attribute[] result = ldapGroupRepository.getAddAttributes(group);
        assertThat("group id", result[1].getValue(), equalTo("1"));
        assertThat("group name", result[2].getValue(), equalTo("groupName"));
        assertThat("group description", result[3].getValue(), equalTo("description"));
    }

    @Test
    public void getAddAttributes_doesNotAddAnyAttribute_returnsArray() throws Exception {
        Group group = new Group();
        group.setGroupId(0);
        Attribute[] result = ldapGroupRepository.getAddAttributes(group);
        assertThat("list size", result.length, equalTo(2));
    }

    @Test
    public void getModifications_groupNameIsNotNullAndNotEqual_addsReplaceMod() throws Exception {
        Group gOld = new Group();
        Group gNew = new Group();
        gOld.setName("old");
        gNew.setName("new");
        List<Modification> result = ldapGroupRepository.getModifications(gOld, gNew);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void getModifications_groupDescriptionNotNullAndNotEqual_addsReplaceMod() throws Exception {
        Group gOld = new Group();
        Group gNew = new Group();
        gOld.setName("old");
        gNew.setDescription("new");
        gOld.setDescription("old");
        List<Modification> result = ldapGroupRepository.getModifications(gOld, gNew);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void getModifications_groupNameMatches_addsNoMods() throws Exception {
        Group gOld = new Group();
        Group gNew = new Group();
        gNew.setName("old");
        gOld.setName("old");
        gOld.setDescription("old");
        List<Modification> result = ldapGroupRepository.getModifications(gOld, gNew);
        assertThat("mods", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getModifications_groupDescriptionMatches_addsNoMods() throws Exception {
        Group gOld = new Group();
        Group gNew = new Group();
        gNew.setDescription("old");
        gOld.setDescription("old");
        List<Modification> result = ldapGroupRepository.getModifications(gOld, gNew);
        assertThat("mods", result.isEmpty(), equalTo(true));
    }
}
