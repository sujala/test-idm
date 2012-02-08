package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Group;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Feb 8, 2012
 * Time: 11:29:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class CloudKsGroupBuilderTest {
    DefaultCloudKsGroupBuilder defaultCloudKsGroupBuilder = new DefaultCloudKsGroupBuilder();
    private Group groupDoMock;

    @Before
    public void setup(){
        groupDoMock = mock(Group.class);
        when(groupDoMock.getName()).thenReturn("Group1");
        when(groupDoMock.getGroupId()).thenReturn(1);
        when(groupDoMock.getDescription()).thenReturn("Group Description");
    }
    @Test
    public void build_setName(){
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs = defaultCloudKsGroupBuilder.build(groupDoMock);
        assertThat("Name",groupKs.getName(),equalTo("Group1"));

    }

    @Test
    public void  build_setDescription(){
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs = defaultCloudKsGroupBuilder.build(groupDoMock);
        assertThat("Description", groupKs.getDescription(), equalTo("Group Description"));
    }

    @Test
    public void build_setId(){
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs = defaultCloudKsGroupBuilder.build(groupDoMock);
        assertThat("Id", groupKs.getId(), equalTo("1"));
    }

    
}
