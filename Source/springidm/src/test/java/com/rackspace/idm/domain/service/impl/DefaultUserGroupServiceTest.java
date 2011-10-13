package com.rackspace.idm.domain.service.impl;

import com.rackspace.cloud.servers.bean.LimitGroupType;
import com.rackspace.cloud.service.servers.CloudServers;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.idm.domain.entity.ESBCloudServersFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 5:39 PM
 */
public class DefaultUserGroupServiceTest {

    private DefaultUserGroupService defaultUserGroupService;
    private ESBCloudServersFactory esbCloudServersFactory;
    private CloudServers csClient;

    @Before
    public void setUp() throws Exception {
        defaultUserGroupService = new DefaultUserGroupService();
        esbCloudServersFactory = mock(ESBCloudServersFactory.class);
        csClient = mock(CloudServers.class);
        defaultUserGroupService.setEsbCloudServersFactory(esbCloudServersFactory);
        defaultUserGroupService.setCsClient(csClient);
        when(esbCloudServersFactory.getCSClient("123")).thenReturn(csClient);
    }

    @Test
    public void getGroups_callsESBCLoudServersFactory() throws Exception {
        defaultUserGroupService.getGroups(123);
        verify(esbCloudServersFactory).getCSClient("123");
    }

    @Test
    public void getGroups_callsCloudServersClient() throws Exception {
        defaultUserGroupService.getGroups(123);
        verify(csClient).getAPILimitsForAccount(123);
    }

    @Test
    public void getGroups_returnsNonNullValue() throws Exception {
        when(csClient.getAPILimitsForAccount(123)).thenReturn(new LimitGroupType());
        List<Group> groups = defaultUserGroupService.getGroups(123);
        assertThat("groups", groups, notNullValue());
    }
}
