package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Group;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 5/31/12
 * Time: 4:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultCloudGroupBuilderTest{

    com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group;
    DefaultCloudGroupBuilder defaultCloudGroupBuilder;

    @Before
    public void setUp() throws Exception {
        defaultCloudGroupBuilder = new DefaultCloudGroupBuilder();
        group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        group.setDescription("groupDisc");
        group.setId("1234");
        group.setName("groupName");
    }

    @Test
    public void build_withValidGroup_setsGroupDODescription() throws Exception {
        Group groupDO = defaultCloudGroupBuilder.build(group);
        assertThat("group description", groupDO.getDescription(), equalTo(group.getDescription()));
    }

    @Test
    public void build_withBlankGroupDescription_setsGroupDODescriptionToSpace() throws Exception {
        group.setDescription("");
        Group groupDO = defaultCloudGroupBuilder.build(group);
        assertThat("group description", groupDO.getDescription(), equalTo(" "));
    }

    @Test
    public void build_withValidGroup_setsGroupDOId() throws Exception {
        Group groupDO = defaultCloudGroupBuilder.build(group);
        assertThat("group Id", groupDO.getGroupId(), equalTo(Integer.parseInt(group.getId())));
    }

    @Test
    public void build_withValidNullGroupId_setsGroupDOIdToNull() throws Exception {
        group.setId(null);
        Group groupDO = defaultCloudGroupBuilder.build(group);
        assertThat("group Id", groupDO.getGroupId(), equalTo(null));
    }

    @Test
    public void build_withValidGroup_setsGroupDOName() throws Exception {
        Group groupDO = defaultCloudGroupBuilder.build(group);
        assertThat("group name", groupDO.getName(), equalTo(group.getName()));
    }


}
