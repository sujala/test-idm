package com.rackspace.idm.domain.service.impl;

import org.apache.commons.configuration.Configuration;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
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
        when(groupDao.getGroupById("0")).thenReturn(group);
        assertThat("group",defaultGroupService.getGroupById("0"),instanceOf(com.rackspace.idm.domain.entity.Group.class));
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
        group.setGroupId("123");
        when(config.getString("defaultGroupId")).thenReturn("0");
        when(groupDao.getGroupById("123")).thenReturn(group);
        defaultGroupService.updateGroup(group);
    }

    @Test
    public void updateGroup_groupExists_callsGroupDaoMethodGet() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        group.setGroupId("123");
        group.setName("John Smith");
        when(config.getString("defaultGroupId")).thenReturn("0");
        when(groupDao.getGroupById("123")).thenReturn(group);
        doNothing().when(groupDao).updateGroup(group);

        defaultGroupService.updateGroup(group);

        verify(groupDao).getGroupById("123");
    }

    @Test
    public void updateGroup_groupExists_callsGroupDaoMethodUpdate() throws Exception {
        com.rackspace.idm.domain.entity.Group group = new com.rackspace.idm.domain.entity.Group();
        group.setGroupId("123");
        group.setName("John Smith");
        when(groupDao.getGroupById("123")).thenReturn(group);
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
}
