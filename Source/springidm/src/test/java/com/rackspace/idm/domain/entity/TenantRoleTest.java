package com.rackspace.idm.domain.entity;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 12/29/11
 * Time: 1:59 PM
 */
public class TenantRoleTest {
    private TenantRole tenantRole = new TenantRole();
    @Test
    public void setTenantIds_copyAllObjects() throws Exception {
        String[] tenantIds = {"1", "2","3","4", "5"};
        tenantRole.setTenantIds(tenantIds);
        assertThat("size", tenantRole.getTenantIds().length,equalTo(5));
        assertThat("[0]", tenantRole.getTenantIds()[0],equalTo("1"));
        assertThat("[0]", tenantRole.getTenantIds()[1],equalTo("2"));
        assertThat("[0]", tenantRole.getTenantIds()[2],equalTo("3"));
        assertThat("[0]", tenantRole.getTenantIds()[3],equalTo("4"));
        assertThat("[0]", tenantRole.getTenantIds()[4],equalTo("5"));
    }
}
