package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: yung5027
 * Date: 7/20/12
 * Time: 2:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class PermissionTest {
    Permission permission;

    @Before
    public void setUp() throws Exception {
        permission = new Permission();
    }

    @Test
    public void getUniqueId_ldapEntryIsNull_returnNull() throws Exception {
        permission.setLdapEntry(null);
        String result = permission.getUniqueId();
        assertThat("unique id", result, equalTo(null));
    }

    @Test
    public void hashCode_allAttributesIsNull_returnsHashCode() throws Exception {
        permission.setLdapEntry(null);
        permission.setClientId(null);
        permission.setCustomerId(null);
        permission.setPermissionId(null);
        int result = permission.hashCode();
        assertThat("hash code", result, equalTo(923521));
    }

    @Test
    public void hashCode_allAttributesNotNull_returnsHashCode() throws Exception {
        permission.setLdapEntry(new ReadOnlyEntry("uniqueId", new Attribute[0]));
        permission.setClientId("clientId");
        permission.setCustomerId("customerId");
        permission.setPermissionId("permissionId");
        int result = permission.hashCode();
        assertThat("hash code", result, equalTo(953108178));
    }

    @Test
    public void equals_objectIsNull_returnsFalse() throws Exception {
        boolean result = permission.equals(null);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_clientIdIsNullAndObjectClientIdNotNull_returnsFalse() throws Exception {
        Permission object = new Permission();
        object.setClientId("notNull");
        permission.setClientId(null);
        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_clientIdNotNullAndNotMatchObjectClientId_returnsFalse() throws Exception {
        Permission object = new Permission();
        object.setClientId("notNull");
        permission.setClientId("notSame");
        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_customerIdIsNullAndObjectCustomerIdNotNull_returnsFalse() throws Exception {
        Permission object = new Permission();
        object.setCustomerId("notNull");
        permission.setCustomerId(null);
        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_customerIdNotNullAndNotMatchObjectCustomerId_returnsFalse() throws Exception {
        Permission object = new Permission();
        object.setCustomerId("notNull");
        permission.setCustomerId("notSame");
        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_permissionIdIsNullAndObjectPermissionIdNotNull_returnsFalse() throws Exception {
        Permission object = new Permission();
        object.setPermissionId("notNull");
        permission.setPermissionId(null);
        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_permissionIdNotNullAndNotMatchObjectPermissionId_returnsFalse() throws Exception {
        Permission object = new Permission();
        object.setPermissionId("notNull");
        permission.setPermissionId("notSame");
        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_ldapEntryIsNullAndObjectLdapEntryNotNull_returnsFalse() throws Exception {
        Permission object = new Permission();
        object.setLdapEntry(new ReadOnlyEntry("uniqueId", new Attribute[0]));
        permission.setLdapEntry(null);
        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_ldapEntryNotNullAndNotMatchObjectLdapEntry_returnsFalse() throws Exception {
        Permission object = new Permission();
        object.setLdapEntry(new ReadOnlyEntry("uniqueId", new Attribute[0]));
        permission.setLdapEntry(new ReadOnlyEntry("notSame", new Attribute[0]));
        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(false));
    }
    
    @Test
    public void equals_allAttributesNotNullAndMatches_returnsTrue() throws Exception {
        ReadOnlyEntry readOnlyEntry = new ReadOnlyEntry("same", new Attribute[0]);
        Permission object = new Permission();
        object.setClientId("same");
        object.setCustomerId("same");
        object.setPermissionId("same");
        object.setLdapEntry(readOnlyEntry);

        permission.setClientId("same");
        permission.setCustomerId("same");
        permission.setPermissionId("same");
        permission.setLdapEntry(readOnlyEntry);

        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void equals_allAttributesNullAndMatches_returnsTrue() throws Exception {
        Permission object = new Permission();
        object.setClientId(null);
        object.setCustomerId(null);
        object.setPermissionId(null);
        object.setLdapEntry(null);

        permission.setClientId(null);
        permission.setCustomerId(null);
        permission.setPermissionId(null);
        permission.setLdapEntry(null);

        boolean result = permission.equals(object);
        assertThat("boolean", result, equalTo(true));    
    }
}
