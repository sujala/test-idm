package com.rackspace.docs.identity.api.ext.rax_ksgrp.v1;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {

    ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createGroup_returnsGroup() throws Exception {
        assertThat("group",objectFactory.createGroup(),instanceOf(Group.class));
    }

    @Test
    public void createGroups_returnGroups() throws Exception {
        assertThat("groups",objectFactory.createGroups(),instanceOf(Groups.class));
    }

    @Test
    public void createTenantGroup_returnsTenantGroup() throws Exception {
        assertThat("tenant group",objectFactory.createTenantGroup(),instanceOf(TenantGroup.class));
    }
}
