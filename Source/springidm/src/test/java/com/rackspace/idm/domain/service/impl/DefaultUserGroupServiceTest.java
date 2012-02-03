package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.GroupDao;
import com.rackspace.idm.domain.entity.Group;
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
public class DefaultUserGroupServiceTest {

    private DefaultGroupService defaultUserGroupService;
    GroupDao groupDao;

    @Before
    public void setUp() throws Exception {
        defaultUserGroupService = new DefaultGroupService(groupDao);
    }

    @Ignore
    @Test
    public void getGroups_returnsNonNullValue() throws Exception {
        List<Group> groups = defaultUserGroupService.getGroups("0",0);
        assertThat("groups", groups, notNullValue());
    }
}
