package com.rackspace.idm.domain.entity;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
    public void hashCode_attributesIsNull_returnsHashCode() throws Exception {
        int result = tenantRole.hashCode();
        assertThat("hash code", result, notNullValue());
    }

    @Test
    public void hashCode_attributesSet_returnsHashCode() throws Exception {
        tenantRole.setClientId("clientId");
        tenantRole.setRoleRsId("roleRsId");
        int result = tenantRole.hashCode();
        assertThat("hash code", result, notNullValue());
    }

    @Test
    public void equals_objectIsNull_returnsFalse() throws Exception {
        boolean result = tenantRole.equals(null);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_objectClassNotSame_returnsFalse() throws Exception {
        boolean result = tenantRole.equals("test");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_thisClientIdIsNullAndObjectClientIdNotNull_returnsFalse() throws Exception {
        tenantRole.setClientId(null);
        TenantRole obj = new TenantRole();
        obj.setClientId("notNull");
        boolean result = tenantRole.equals(obj);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_thisClientIdNotEqualsObjectClientId_returnsFalse() throws Exception {
        tenantRole.setClientId("notSame");
        TenantRole obj = new TenantRole();
        obj.setClientId("notNull");
        boolean result = tenantRole.equals(obj);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_thisRoleRsIdIsNullAndObjectRoleRsIdNotNull_returnsFalse() throws Exception {
        tenantRole.setClientId(null);
        tenantRole.setRoleRsId(null);
        TenantRole obj = new TenantRole();
        obj.setClientId(null);
        obj.setRoleRsId("notNull");
        boolean result = tenantRole.equals(obj);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_thisRoleRsIdNotEqualsObjectRoleRsId_returnsFalse() throws Exception {
        tenantRole.setRoleRsId("notSame");
        TenantRole obj = new TenantRole();
        obj.setRoleRsId("notNull");
        boolean result = tenantRole.equals(obj);
        assertThat("boolean", result, equalTo(false));
    }
}

