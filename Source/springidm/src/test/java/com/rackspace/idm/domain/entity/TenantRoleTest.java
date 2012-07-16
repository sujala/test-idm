package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.ReadOnlyEntry;
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
        tenantRole.addTenantId(null);
        String[] tenantIds = tenantRole.getTenantIds();
        assertThat("tenantIds", tenantIds, hasItemInArray("id"));
    }

    @Test
    public void getLdapEntry_returnsLdapEntry() throws Exception {
        ReadOnlyEntry result = tenantRole.getLDAPEntry();
        assertThat("ldap", result, equalTo(null));
    }

    @Test
    public void removeTenantId_tenantIdsIsNull_returns() throws Exception {
        tenantRole.setTenantIds(null);
        tenantRole.removeTenantId(null);
    }

    @Test
    public void removeTenantId_emptyArrayList_returns() throws Exception {
        tenantRole.setTenantIds(new String[0]);
        tenantRole.removeTenantId(null);
    }

    @Test
    public void removeTenantId_idNotFound_doesNothing() throws Exception {
        String[] test = {"1"};
        tenantRole.setTenantIds(test);
        tenantRole.removeTenantId("2");
        assertThat("tenant id", tenantRole.getTenantIds()[0], equalTo("1"));
    }

    @Test
    public void containsTenantId_tenantIdsIsNull_returnsFalse() throws Exception {
        tenantRole.setTenantIds(null);
        boolean result = tenantRole.containsTenantId(null);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void containsTenantId_emptyArrayList_returnsFalse() throws Exception {
        tenantRole.setTenantIds(new String[0]);
        boolean result = tenantRole.containsTenantId(null);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void hashCode_attributesIsNull_returnsHashCode() throws Exception {
        int result = tenantRole.hashCode();
        assertThat("hash code", result, equalTo(29791));
    }

    @Test
    public void hashCode_attributesSet_returnsHashCode() throws Exception {
        tenantRole.setClientId("clientId");
        tenantRole.setRoleRsId("roleRsId");
        int result = tenantRole.hashCode();
        assertThat("hash code", result, equalTo(1426093715));
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

    @Test
    public void equals_thisTenantIdsNotMatchObjectTenantIds_returnsFalse() throws Exception {
        tenantRole.setRoleRsId(null);
        tenantRole.setTenantIds(new String[0]);
        TenantRole obj = new TenantRole();
        obj.setRoleRsId(null);
        obj.setTenantIds(new String[1]);
        boolean result = tenantRole.equals(obj);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_everythingMatches_returnsTrue() throws Exception {
        tenantRole.setTenantIds(new String[0]);
        TenantRole obj = new TenantRole();
        obj.setTenantIds(new String[0]);
        boolean result = tenantRole.equals(obj);
        assertThat("boolean", result, equalTo(true));
    }
}
