package com.rackspace.idm.domain.service.impl;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.service.UserService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:39 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultUserGroupServiceTest {

    @InjectMocks
    private DefaultGroupService defaultUserGroupService = new DefaultGroupService();
    @Mock
    private UserService userService;
    @Mock
    private GroupDao groupDao;

    @Ignore
    @Test
    public void getGroups_returnsNonNullValue() throws Exception {
        List<Group> groups = defaultUserGroupService.getGroups("0",0);
        assertThat("groups", groups, notNullValue());
    }
}
