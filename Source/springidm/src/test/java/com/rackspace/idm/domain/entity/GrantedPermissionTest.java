package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.junit.Before;
import org.junit.Test;


import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/11/12
 * Time: 10:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class GrantedPermissionTest {

    GrantedPermission grantedPermission;
    GrantedPermission spy;

    @Before
    public void setUp() throws Exception {
        grantedPermission = new GrantedPermission("customerId","clientId","permissionId");
        spy = spy(grantedPermission);
    }

    @Test
    public void getUniqueId_ldapEntryNull_returnsNull() throws Exception {
        assertThat("returns null",grantedPermission.getUniqueId(),equalTo(null));
    }

    @Test
    public void getUniqueId_ldapEntryExists_returnsDn() throws Exception {
        grantedPermission.setLdapEntry(new ReadOnlyEntry("dn",new Attribute[0]));
        assertThat("returns dn",grantedPermission.getUniqueId(),equalTo("dn"));
    }

    @Test
    public void setResourceGroups_correctlySetsResourceGroups() throws Exception {
        String[] resourceGroups = new String[0];
        assertThat("null resource groups",grantedPermission.getResourceGroups(),equalTo(null));
        grantedPermission.setResourceGroups(resourceGroups);
        assertThat("resource groups was set",grantedPermission.getResourceGroups(),equalTo(resourceGroups));
    }

    @Test
    public void hashCode_ldapEntryNull_returnsHashCode() throws Exception {
        assertThat("returns hashcode",grantedPermission.hashCode(),equalTo(-1245085026));
    }

    @Test
    public void hashCode_ldapEntryNotNull_returnsHashCode() throws Exception {
        grantedPermission.setLdapEntry(new ReadOnlyEntry("dn",new Attribute[0]));
        assertThat("returns hashcode", grantedPermission.hashCode(), equalTo(-1244985516));
    }

    @Test
    public void equals_objectsAreDifferentClasses_returnsFalse() throws Exception {
        Permission permission = new Permission("customerId","clientId","permissionId");
        assertThat("equals",grantedPermission.equals(permission),equalTo(false));
    }

    @Test
    public void equals_ldapEntryNullButOtherLdapEntryNotNull_returnsFalse() throws Exception {
        GrantedPermission grantedPermission1 = new GrantedPermission("customerId","clientId","permissionId");
        grantedPermission1.setLdapEntry(new ReadOnlyEntry("dn",new Attribute[0]));
        assertThat("equals", grantedPermission.equals(grantedPermission1), equalTo(false));
    }

    @Test
    public void equals_bothLdapEntriesExistButNotEqual_returnsFalse() throws Exception {
        GrantedPermission grantedPermission1 = new GrantedPermission("customerId","clientId","permissionId");
        grantedPermission1.setLdapEntry(new ReadOnlyEntry("dn",new Attribute[0]));
        grantedPermission.setLdapEntry(new ReadOnlyEntry("anotherDN",new Attribute[0]));
        assertThat("equals", grantedPermission.equals(grantedPermission1), equalTo(false));
    }

    @Test
    public void equals_bothLdapEntriesExistAndEqual_returnsTrue() throws Exception {
        GrantedPermission grantedPermission1 = new GrantedPermission("customerId","clientId","permissionId");
        grantedPermission1.setLdapEntry(new ReadOnlyEntry("dn",new Attribute[0]));
        grantedPermission.setLdapEntry(new ReadOnlyEntry("dn",new Attribute[0]));
        assertThat("equals", grantedPermission.equals(grantedPermission1), equalTo(true));
    }

    @Test
    public void equals_bothResourceGroupsAreEqual_returnsTrue() throws Exception {
        GrantedPermission grantedPermission1 = new GrantedPermission("customerId","clientId","permissionId");
        grantedPermission1.setResourceGroups(new String[]{});
        grantedPermission.setResourceGroups(new String[]{});
        assertThat("equals", grantedPermission.equals(grantedPermission1), equalTo(true));
    }

    @Test
    public void copyChanges_modifiedResourceGroupNull_doesNotSetResourceGroup() throws Exception {
        spy.copyChanges(new GrantedPermission());
        verify(spy,never()).setResourceGroups(any(String[].class));
    }

    @Test
    public void copyChanges_modifiedResourceGroupNotNull_setsResourceGroup() throws Exception {
        String[] resourceGroup = new String[0];
        GrantedPermission grantedPermission1 = new GrantedPermission();
        grantedPermission1.setResourceGroups(resourceGroup);
        spy.copyChanges(grantedPermission1);
        verify(spy).setResourceGroups(resourceGroup);
    }
}