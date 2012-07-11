package com.rackspace.idm.domain.entity;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
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

    @Test
    public void addTenantId_tenantIdsIsNull_addsId() throws Exception {
        tenantRole.setTenantIds(null);
        tenantRole.addTenantId("id");
        String[] tenantIds = tenantRole.getTenantIds();
        assertThat("tenantIds", tenantIds, hasItemInArray("id"));
    }

    @Test
        public void addTenantId_tenantIdsIsEmpty_addsId() throws Exception {
            tenantRole.setTenantIds(new String[]{});
            tenantRole.addTenantId("id");
            String[] tenantIds = tenantRole.getTenantIds();
            assertThat("tenantIds", tenantIds, hasItemInArray("id"));
        }
}
