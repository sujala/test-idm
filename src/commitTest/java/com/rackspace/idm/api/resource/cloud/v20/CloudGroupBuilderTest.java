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
 * Time: 10:40:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class CloudGroupBuilderTest {
    DefaultCloudGroupBuilder defaultCloudGroupBuilder = new DefaultCloudGroupBuilder();
    private com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group;


    @Before
    public void setup(){
        group = mock(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group.class);
        when(group.getName()).thenReturn("Group1");
        when(group.getDescription()).thenReturn("Group Description");

    }

    @Test
    public void build_getName(){
        Group groupDo = defaultCloudGroupBuilder.build(group);
        assertThat("Name",groupDo.getName(),equalTo("Group1"));
    }

    @Test
    public void build_getDescription(){
        Group groupDo = defaultCloudGroupBuilder.build(group);
        assertThat("Description",groupDo.getDescription(),equalTo("Group Description"));
    }
    
}
