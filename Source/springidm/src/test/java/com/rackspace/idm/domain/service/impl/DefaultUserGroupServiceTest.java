package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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

    @Before
    public void setUp() throws Exception {
        defaultUserGroupService = new DefaultGroupService();
    }

    @Ignore
    @Test
    public void getGroups_returnsNonNullValue() throws Exception {
        Groups groups = defaultUserGroupService.getGroups(123);
        assertThat("groups", groups, notNullValue());
    }
}
