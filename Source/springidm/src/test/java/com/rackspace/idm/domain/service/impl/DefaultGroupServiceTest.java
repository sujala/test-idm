package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.Users;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Feb 15, 2012
 * Time: 11:34:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultGroupServiceTest{
    private DefaultUserService defaultUserService;
    private DefaultGroupService defaultGroupService;
    User user1 = new User();
    User user2 = new User();
    Users listUsers = new Users();
    List<User> listUser;
    GroupDao groupDao;


    @Before
    public void setup(){
        groupDao = mock(GroupDao.class);
        defaultGroupService = new DefaultGroupService(groupDao);
        defaultUserService = mock(DefaultUserService.class);
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

        //set mock
        defaultGroupService.setDefaultUserService(defaultUserService);
    }


    @Test
    public void testGetAllEnabledUsers() throws Exception {
        FilterParam[] filterParam = new FilterParam[]{new FilterParam(FilterParam.FilterParamName.GROUP_ID, 1)};
        when(defaultUserService.getAllUsers(filterParam, 0, 0)).thenReturn(listUsers);
        Users user = defaultGroupService.getAllEnabledUsers(filterParam,"0", 0);
        assertThat("user1",user.getUsers().get(0).getUsername(),equalTo("user1"));
        assertThat("user2",user.getUsers().size(),equalTo(1));
    }
}
