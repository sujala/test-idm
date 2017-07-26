package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: yung5027
 * Date: 7/19/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class TenantTest {
    Tenant tenant;

    @Before
    public void setUp() throws Exception {
        tenant = new Tenant();
    }

    @Test
    public void addBaseUrlId_baseUrlDoesContainUrlId_doesNotRemoveId() throws Exception {
        tenant.getBaseUrlIds().add("123");
        tenant.getBaseUrlIds().add("123");
        HashSet<String> result = tenant.getBaseUrlIds();
        assertThat("base url id", result.contains("123"), equalTo(true));
        assertThat("string list", result.size(), equalTo(1));
    }

    @Test
    public void addV1Default_baseUrl() throws Exception {
        tenant.getV1Defaults().add("123");
        tenant.getV1Defaults().add("123");
        HashSet<String> result = tenant.getV1Defaults();
        assertThat("base url id", result.contains("123"), equalTo(true));
        assertThat("string list", result.size(), equalTo(1));
    }

    @Test
    public void removeBaseUrlId_baseUrlDoesNotContainUrlId_doesNotRemoveId() throws Exception {
        tenant.getBaseUrlIds().add("245");
        tenant.getBaseUrlIds().add("123");
        tenant.getBaseUrlIds().remove("123");
        HashSet<String> result = tenant.getBaseUrlIds();
        assertThat("base url id", result.contains("245"), equalTo(true));
        assertThat("string list", result.size(), equalTo(1));
    }

    @Test
    public void removeV1Default_baseUrl() throws Exception {
        tenant.getV1Defaults().add("245");
        tenant.getV1Defaults().add("123");
        tenant.getV1Defaults().remove("123");
        HashSet<String> result = tenant.getV1Defaults();
        assertThat("base url id", result.contains("245"), equalTo(true));
        assertThat("string list", result.size(), equalTo(1));
    }

    @Test
    public void containsBaseUrlId_baseUrlIdsIsNull_returnFalse() throws Exception {
        tenant.getBaseUrlIds().clear();
        boolean result = tenant.getBaseUrlIds().contains("123");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void containsBaseUrlId_baseUrlIdsLengthIs0_returnsFalse() throws Exception {
        tenant.getBaseUrlIds().clear();
        boolean result = tenant.getBaseUrlIds().contains("123");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void containsV1Default_baseUrlIdsLengthIs0_returnsFalse() throws Exception {
        tenant.getV1Defaults().clear();
        boolean result = tenant.getV1Defaults().contains("123");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void containsBaseUrlId_baseUrlIdsLengthNotZero_returnsTrue() throws Exception {
        tenant.getBaseUrlIds().add("123");
        boolean result = tenant.getBaseUrlIds().contains("123");
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void containsV1Default_baseUrlIdsLengthNotZero_returnsTrue() throws Exception {
        tenant.getV1Defaults().add("123");
        boolean result = tenant.getV1Defaults().contains("123");
        assertThat("boolean", result, equalTo(true));
    }
}
