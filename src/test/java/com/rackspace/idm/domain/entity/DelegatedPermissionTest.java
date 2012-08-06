package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/10/12
 * Time: 11:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class DelegatedPermissionTest {
    DelegatedPermission delegatedPermission;
    ReadOnlyEntry ldapEntry = new ReadOnlyEntry("uniqueId",new Attribute[0]);
    DelegatedPermission spy;

    @Before
    public void setUp() throws Exception {
        delegatedPermission = new DelegatedPermission();
        delegatedPermission.setLdapEntry(ldapEntry);
        spy = spy(delegatedPermission);
    }

    @Test
    public void getUniqueId_ldapEntryIsNull_returnsNull() throws Exception {
        delegatedPermission.setLdapEntry(null);
        String result = delegatedPermission.getUniqueId();
        assertThat("unique id", result, equalTo(null));
        delegatedPermission.setLdapEntry(ldapEntry);
    }

    @Test
    public void getUniqueId_ldapEntryNotNull_returnsDN() throws Exception {;
        assertThat("dn string", delegatedPermission.getUniqueId(), equalTo("uniqueId"));
    }

    @Test
    public void getResourceGroups_returnsResourceGroups() throws Exception {
        String[] result = delegatedPermission.getResourceGroups();
        assertThat("resource groups", result, equalTo(null));
    }

    @Test
    public void hashCode_ldapNull_returnsHashCode() throws Exception {
        delegatedPermission.setLdapEntry(null);
        int result = delegatedPermission.hashCode();
        assertThat("hash code", result, equalTo(887503681));
        delegatedPermission.setLdapEntry(ldapEntry);
    }

    @Test
    public void hashCode_ldapNotNull_returnsHashCode() throws Exception {
        int result = delegatedPermission.hashCode();
        assertThat("hash code", result, equalTo(349171701));
    }

    @Test
    public void equals_isNotPermission_returnsFalse() throws Exception {
        assertThat("equals",delegatedPermission.equals(new User()),equalTo(false));
    }

    @Test
    public void equals_isNotSameTypeOf_returnsFalse() throws Exception {
        Permission definedPermission = new Permission();
        assertThat("equals", delegatedPermission.equals(definedPermission), equalTo(false));
    }

    @Test
    public void equals_ldapEntryNullAndOtherLdapNotNull_returnsFalse() throws Exception {
        DelegatedPermission delegatedPermission1 = new DelegatedPermission();
        delegatedPermission1.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        delegatedPermission.setLdapEntry(null);
        assertThat("equals",delegatedPermission.equals(delegatedPermission1),equalTo(false));
        delegatedPermission.setLdapEntry(ldapEntry);
    }

    @Test
    public void equals_ldapEntryNullAndOtherLdapNotNull_returnsTrue() throws Exception {
        DelegatedPermission delegatedPermission1 = new DelegatedPermission();
        delegatedPermission1.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        delegatedPermission.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        assertThat("equals",delegatedPermission.equals(delegatedPermission1),equalTo(true));
        delegatedPermission.setLdapEntry(ldapEntry);
    }

    @Test
    public void equals_bothLdapEntriesNull_returnsFalse() throws Exception {
        DelegatedPermission delegatedPermission1 = new DelegatedPermission();
        delegatedPermission.setLdapEntry(null);
        delegatedPermission.setResourceGroups(new String[1]);
        assertThat("equals", delegatedPermission.equals(delegatedPermission1), equalTo(false));
        delegatedPermission.setLdapEntry(ldapEntry);
    }

    @Test
    public void equals_ldapEntriesExistButNotEqual_returnsFalse() throws Exception {
        DelegatedPermission delegatedPermission1 = new DelegatedPermission();
        delegatedPermission1.setLdapEntry(new ReadOnlyEntry("stringDN",new Attribute[0]));
        assertThat("equals", delegatedPermission.equals(delegatedPermission1), equalTo(false));
    }

    @Test
    public void equals_equalButNotSameObject_returnsTrue() throws Exception {
        DelegatedPermission delegatedPermission1 = new DelegatedPermission();
        DelegatedPermission delegatedPermission2 = new DelegatedPermission();
        assertThat("equals",delegatedPermission1.equals(delegatedPermission2),equalTo(true));
    }

    @Test
    public void copyChanges_otherHasNoResourceGroups_doesNotCallSetResourceGroups() throws Exception {
        spy.copyChanges(new DelegatedPermission());
        verify(spy,never()).setResourceGroups(any(String[].class));
    }

    @Test
    public void copyChanges_otherHasResourceGroups_callSetResourceGroups() throws Exception {
        String[] resourceGroups = new String[0];
        DelegatedPermission delegatedPermission1 = new DelegatedPermission();
        delegatedPermission1.setResourceGroups(resourceGroups);
        spy.copyChanges(delegatedPermission1);
        verify(spy).setResourceGroups(resourceGroups);
    }
}
