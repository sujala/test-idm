package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/10/12
 * Time: 11:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class DelegatedPermissionTest {
    DelegatedPermission delegatedPermission;

    @Before
    public void setUp() throws Exception {
        delegatedPermission = new DelegatedPermission();
    }

    @Test
    public void getUniqueId_ldapEntryIsNull_returnsNull() throws Exception {
        String result = delegatedPermission.getUniqueId();
        assertThat("unique id", result, equalTo(null));
    }

    @Test
    public void getResourceGroups_returnsResourceGroups() throws Exception {
        String[] result = delegatedPermission.getResourceGroups();
        assertThat("resource groups", result, equalTo(null));
    }

    @Test
    public void hasCode_returnsHashCode() throws Exception {
        int result = delegatedPermission.hashCode();
        assertThat("hash code", result, equalTo(887503681));
    }
}
