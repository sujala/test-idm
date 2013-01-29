package com.rackspace.idm.domain.service.impl;

import org.apache.commons.configuration.Configuration;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspacecloud.docs.auth.api.v1.Group;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Feb 15, 2012
 * Time: 11:34:03 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultGroupServiceTestOld {
    @InjectMocks
    DefaultGroupService defaultGroupService = new DefaultGroupService();
    User user1 = new User();
    User user2 = new User();
    Users listUsers = new Users();
    List<User> listUser;
    @Mock
    GroupDao groupDao;
    @Mock
    DefaultUserService defaultUserService;
    @Mock
    Configuration config;

    DefaultGroupService spy;

    @Before
    public void setup(){
        user1.setDisplayName("user1");
        user1.setUsername("user1");
        user1.setEnabled(true);
        user1.setId("1");
        user2.setDisplayName("user2");
        user2.setUsername("user2");
        user2.setEnabled(false);
        user2.setId("2");
        listUser = new ArrayList<User>();
        listUser.add(user1);
        listUser.add(user2);
        listUsers.setUsers(listUser);
        spy = spy(defaultGroupService);
    }


    @Test
    public void testGetAllEnabledUsers() throws Exception {
        FilterParam[] filterParam = new FilterParam[]{new FilterParam(FilterParam.FilterParamName.GROUP_ID, 1)};
        when(defaultUserService.getAllUsers(filterParam, 0, 0)).thenReturn(listUsers);
        Users user = defaultGroupService.getAllEnabledUsers(filterParam,"0", 0);
        assertThat("user1",user.getUsers().get(0).getUsername(),equalTo("user1"));
        assertThat("user2",user.getUsers().size(),equalTo(1));
    }

    @Test
    public void getGroups_returnsList() throws Exception {
        when(groupDao.getGroups(null,null)).thenReturn(new ArrayList<com.rackspace.idm.domain.entity.Group>());
        assertThat("groups list", defaultGroupService.getGroups(null, null), instanceOf(List.class));
    }

    @Test
    public void getGroupsByMossoId_returnsNull() throws Exception {
        assertThat("null",defaultGroupService.getGroupsByMossoId(null),nullValue());
    }

    @Test
    public void getGroupListByMossoId_returnsNull() throws Exception {
        assertThat("null",defaultGroupService.getGroupListByMossoId(null),nullValue());
    }

    @Test
    public void getGroupById_returnsGroup() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        when(groupDao.getGroupById(0)).thenReturn(group);
        assertThat("group",defaultGroupService.getGroupById(0),instanceOf(com.rackspace.idm.domain.entity.Group.class));
    }

    @Test (expected = NotFoundException.class)
    public void getGroupByName_nullGroup_throwsNotFoundException() throws Exception {
        when(groupDao.getGroupByName(null)).thenReturn(null);
        defaultGroupService.getGroupByName(null);
    }

    @Test
    public void getGroupByName_groupExists_returnsGroup() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        when(groupDao.getGroupByName(null)).thenReturn(group);
        defaultGroupService.getGroupByName(null);
        assertThat("group",defaultGroupService.getGroupByName(null),instanceOf(com.rackspace.idm.domain.entity.Group.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addGroup_nullGroup_throwsIllegalArgumentException() throws Exception {
        defaultGroupService.addGroup(null);
    }

    @Test
    public void addGroup_callsVerifyDuplicateGroup() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        doNothing().when(spy).verifyDuplicateGroup(group);
        when(groupDao.getNextGroupId()).thenReturn("0");
        doNothing().when(groupDao).addGroup(group);
        spy.addGroup(group);
        verify(spy).verifyDuplicateGroup(group);
    }

    @Test
    public void addGroup_callsGroupDaoMethodGet() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        doNothing().when(spy).verifyDuplicateGroup(group);
        when(groupDao.getNextGroupId()).thenReturn("0");
        doNothing().when(groupDao).addGroup(group);
        spy.addGroup(group);
        verify(groupDao).getNextGroupId();
    }

    @Test
    public void addGroup_setsGroupID() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        doNothing().when(spy).verifyDuplicateGroup(group);
        when(groupDao.getNextGroupId()).thenReturn("12");
        doNothing().when(groupDao).addGroup(group);
        spy.addGroup(group);
        assertThat("groupID",group.getGroupId(),equalTo(12));
    }

    @Test
    public void addGroup_callsGroupDaoMethodAdd() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        doNothing().when(spy).verifyDuplicateGroup(group);
        when(groupDao.getNextGroupId()).thenReturn("0");
        doNothing().when(groupDao).addGroup(group);
        spy.addGroup(group);
        verify(groupDao).addGroup(group);
    }

    @Test
    public void insertGroup_callsGroupDaoMember() throws Exception {
        doNothing().when(groupDao).addGroup(null);
        defaultGroupService.insertGroup(null);
        verify(groupDao).addGroup(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateGroup_nullGroup_callsGroupDaoMethod() throws Exception {
        defaultGroupService.updateGroup(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateGroup_nullGroupID_callsGroupDaoMethod() throws Exception {
        defaultGroupService.updateGroup(new com.rackspace.idm.domain.entity.Group());
    }

    @Test (expected = BadRequestException.class)
    public void updateGroup_nullGroupName_callsGroupDaoMethod() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        group.setGroupId(123);
        when(config.getString("defaultGroupId")).thenReturn("0");
        when(groupDao.getGroupById(123)).thenReturn(group);
        defaultGroupService.updateGroup(group);
    }

    @Test
    public void updateGroup_groupExists_callsGroupDaoMethodGet() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        group.setGroupId(123);
        group.setName("John Smith");
        when(config.getString("defaultGroupId")).thenReturn("0");
        when(groupDao.getGroupById(123)).thenReturn(group);
        doNothing().when(groupDao).updateGroup(group);

        defaultGroupService.updateGroup(group);

        verify(groupDao).getGroupById(123);

    }

    @Test
    public void updateGroup_groupNamesNotEqual_callsVerifiyDuplicateGroup() throws Exception {
        com.rackspace.idm.domain.entity.Group group1 = new com.rackspace.idm.domain.entity.Group();
        com.rackspace.idm.domain.entity.Group group2 = new com.rackspace.idm.domain.entity.Group();
        group1.setGroupId(123);
        group1.setName("John Smith");
        group2.setName("Adam Smith");
        when(config.getString("defaultGroupId")).thenReturn("0");
        when(groupDao.getGroupById(123)).thenReturn(group2);
        doNothing().when(groupDao).updateGroup(group1);
        doNothing().when(spy).verifyDuplicateGroup(group1);

        spy.updateGroup(group1);

        verify(spy).verifyDuplicateGroup(group1);

    }

    @Test
    public void updateGroup_groupExists_callsGroupDaoMethodUpdate() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        group.setGroupId(123);
        group.setName("John Smith");
        when(groupDao.getGroupById(123)).thenReturn(group);
        doNothing().when(groupDao).updateGroup(group);

        defaultGroupService.updateGroup(group);

        verify(groupDao).updateGroup(group);

    }

    @Test (expected = IllegalArgumentException.class)
    public void verifyDuplicateGroup_nullGroup_doesNothing() throws Exception {
        defaultGroupService.verifyDuplicateGroup(null);
    }

    @Test
    public void verifyDuplicateGroup_existsNull_doesNothing() throws Exception {
        com.rackspace.idm.domain.entity.Group group = mock(com.rackspace.idm.domain.entity.Group.class);
        defaultGroupService.verifyDuplicateGroup(group);
    }

    @Test (expected = DuplicateException.class)
    public void verifyDuplicateGroup_existsNotNull_throwsDuplicateException() throws Exception {
        com.rackspace.idm.domain.entity.Group group = mock(com.rackspace.idm.domain.entity.Group.class);
        when(groupDao.getGroupByName(null)).thenReturn(group);
        defaultGroupService.verifyDuplicateGroup(group);
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteGroup_nullId_throwsIllegalArgumentException() throws Exception {
        defaultGroupService.deleteGroup(null);
    }

    @Test (expected = NotFoundException.class)
    public void deleteGroup_nullExists_throwsNotFoundException() throws Exception {
        defaultGroupService.deleteGroup("123");
    }

    @Test (expected = BadRequestException.class)
    public void deleteGroup_existsNotNullAndUserEnabled_throwsBadRequestException() throws Exception {
        User user = new User();
        user.setEnabled(true);
        Users users = new Users();
        users.setUsers(new ArrayList<User>());
        users.getUsers().add(user);
        when(groupDao.getGroupById(123)).thenReturn(new com.rackspace.idm.domain.entity.Group());
        when(defaultUserService.getAllUsers(any(FilterParam[].class))).thenReturn(users);
        defaultGroupService.deleteGroup("123");
    }

    @Test
    public void deleteGroup_existsNotNullAndUserNotEnabled_callsDeleteGroupFromUser() throws Exception {
        User user = new User();
        user.setEnabled(false);
        Users users = new Users();
        users.setUsers(new ArrayList<User>());
        users.getUsers().add(user);
        when(groupDao.getGroupById(123)).thenReturn(new com.rackspace.idm.domain.entity.Group());
        when(defaultUserService.getAllUsers(any(FilterParam[].class))).thenReturn(users);
        doNothing().when(spy).deleteGroupFromUser(123, null);
        doNothing().when(groupDao).deleteGroup(123);
        spy.deleteGroup("123");
        verify(spy).deleteGroupFromUser(123,null);
    }

    @Test
    public void deleteGroup_existsNotNullAndNoUsers_callsGroupDaoMethod() throws Exception {
        Users users = new Users();
        users.setUsers(new ArrayList<User>());
        when(groupDao.getGroupById(123)).thenReturn(new com.rackspace.idm.domain.entity.Group());
        when(defaultUserService.getAllUsers(any(FilterParam[].class))).thenReturn(users);
        doNothing().when(groupDao).deleteGroup(123);
        defaultGroupService.deleteGroup("123");
        verify(groupDao).deleteGroup(123);
    }

    @Test
    public void deleteGroup_existsNotNullAndNoUsers_doesNotCallDeleteGroupFromUser() throws Exception {
        Users users = new Users();
        users.setUsers(new ArrayList<User>());
        when(groupDao.getGroupById(123)).thenReturn(new com.rackspace.idm.domain.entity.Group());
        when(defaultUserService.getAllUsers(any(FilterParam[].class))).thenReturn(users);
        doNothing().when(spy).deleteGroupFromUser(123, null);
        doNothing().when(groupDao).deleteGroup(123);
        spy.deleteGroup("123");
        verify(spy,never()).deleteGroupFromUser(anyInt(), anyString());
    }

    @Test
    public void addGroupToUser_callsGroupDaoMethod() throws Exception {
        defaultGroupService.addGroupToUser(0,null);
        verify(groupDao).addGroupToUser(0,null);
    }

    @Test
    public void deleteGroupFromUser_callsGroupDaoMethod() throws Exception {
        defaultGroupService.deleteGroupFromUser(0,null);
        verify(groupDao).deleteGroupFromUser(0,null);
    }

    @Test
    public void getGroupsForUser_callsGroupDaoMethod() throws Exception {
        defaultGroupService.getGroupsForUser(null);
        verify(groupDao).getGroupsForUser(null);
    }
}
