package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/10/12
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefinedPermissionTest {

    DefinedPermission definedPermission;
    DefinedPermission definedPermission1;
    DefinedPermission spy;

    @Before
    public void setUp() throws Exception {
        definedPermission = new DefinedPermission("customerId","clientId","permissionId");
        definedPermission1 = new DefinedPermission("customerId","clientId","permissionId");
        spy = spy(definedPermission);
    }

    @Test
    public void getUniqueId_ldapNull_returnsNull() throws Exception {
        assertThat("unique id is null",definedPermission.getUniqueId(),equalTo(null));
    }

    @Test
    public void getUniqueId_ldapNotNull_returnsLdapDn() throws Exception {
        definedPermission.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        assertThat("unique id is returned",definedPermission.getUniqueId(),equalTo("uniqueId"));
    }

    @Test
    public void setValueAndGetValue_setsRealValue_behavesCorrectly() throws Exception {
        assertThat("value is null",definedPermission.getValue(),equalTo(null));
        definedPermission.setValue("value");
        assertThat("value exists",definedPermission.getValue(),equalTo("value"));
    }

    @Test
    public void setValueAndGetValue_setsEmptyValue_behavesCorrectlyAndReturnsNull() throws Exception {
        assertThat("value is null",definedPermission.getValue(),equalTo(null));
        definedPermission.setValue("");
        assertThat("value is null",definedPermission.getValue(),equalTo(null));
    }

    @Test
    public void setPermissionTypeAndGetPermissionType_setsRealPermissionType_behavesCorrectly() throws Exception {
        assertThat("permission is null",definedPermission.getPermissionType(),equalTo(null));
        definedPermission.setPermissionType("permissionType");
        assertThat("permission exists",definedPermission.getPermissionType(),equalTo("permissionType"));
    }

    @Test
    public void setPermissionTypeAndGetPermissionType_setsEmptyPermissionType_behavesCorrectlyAndReturnsNull() throws Exception {
        assertThat("permission is null",definedPermission.getPermissionType(),equalTo(null));
        definedPermission.setPermissionType("");
        assertThat("permission is null",definedPermission.getPermissionType(),equalTo(null));
    }

    @Test
    public void setTitleAndGetTitle_setsRealTitle_behavesCorrectly() throws Exception {
        assertThat("title is null",definedPermission.getTitle(),equalTo(null));
        definedPermission.setTitle("title");
        assertThat("title exists",definedPermission.getTitle(),equalTo("title"));
    }

    @Test
    public void setTitleAndGetTitle_setsEmptyTitle_behavesCorrectlyAndReturnsNull() throws Exception {
        assertThat("title is null",definedPermission.getTitle(),equalTo(null));
        definedPermission.setTitle("");
        assertThat("title is null",definedPermission.getTitle(),equalTo(null));
    }

    @Test
    public void setDescriptionAndGetDescription_setsRealDescription_behavesCorrectly() throws Exception {
        assertThat("description is null",definedPermission.getDescription(),equalTo(null));
        definedPermission.setDescription("description");
        assertThat("description exists",definedPermission.getDescription(),equalTo("description"));
    }

    @Test
    public void setDescriptionAndGetDescription_setsEmptyDescription_behavesCorrectlyAndReturnsNull() throws Exception {
        assertThat("description is null",definedPermission.getDescription(),equalTo(null));
        definedPermission.setDescription("");
        assertThat("description is null",definedPermission.getDescription(),equalTo(null));
    }

    @Test
    public void hashCode_allFieldsNull_returnsHashCode() throws Exception {
        assertThat("returns hashcode",definedPermission.hashCode(),equalTo(1668640802));
    }

    @Test
    public void hashCode_descriptionNotNull_returnsHashCode() throws Exception {
        definedPermission.setDescription("description");
        assertThat("returns hashcode", definedPermission.hashCode(), equalTo(1675560734));
    }

    @Test
    public void hashCode_descriptionAndEnabledNotNull_returnsHashCode() throws Exception {
        definedPermission.setDescription("description");
        definedPermission.setEnabled(true);
        assertThat("returns hashcode", definedPermission.hashCode(), equalTo(-1736660049));
    }

    @Test
    public void hashCode_descriptionAndEnabledAndGrantedByDefaultNotNull_returnsHashCode() throws Exception {
        definedPermission.setDescription("description");
        definedPermission.setEnabled(true);
        definedPermission.setGrantedByDefault(true);
        assertThat("returns hashcode", definedPermission.hashCode(), equalTo(-599805698));
    }

    @Test
    public void hashCode_permissionTypeAndTitleAndValueNull_returnsHashCode() throws Exception {
        definedPermission.setDescription("description");
        definedPermission.setEnabled(true);
        definedPermission.setGrantedByDefault(true);
        definedPermission.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        assertThat("returns hashcode", definedPermission.hashCode(), equalTo(1754204338));
    }

    @Test
    public void hashCode_titleAndValueNull_returnsHashCode() throws Exception {
        definedPermission.setDescription("description");
        definedPermission.setEnabled(true);
        definedPermission.setGrantedByDefault(true);
        definedPermission.setLdapEntry(new ReadOnlyEntry("uniqueId", new Attribute[0]));
        definedPermission.setPermissionType("permissionType");
        assertThat("returns hashcode", definedPermission.hashCode(), equalTo(-1578398693));
    }

    @Test
    public void hashCode_valueNull_returnsHashCode() throws Exception {
        definedPermission.setDescription("description");
        definedPermission.setEnabled(true);
        definedPermission.setGrantedByDefault(true);
        definedPermission.setLdapEntry(new ReadOnlyEntry("uniqueId", new Attribute[0]));
        definedPermission.setPermissionType("permissionType");
        definedPermission.setTitle("title");
        assertThat("returns hashcode", definedPermission.hashCode(), equalTo(1843115203));
    }

    @Test
    public void hashCode_allFieldsPopulated_returnsHashCode() throws Exception {
        definedPermission.setDescription("description");
        definedPermission.setEnabled(true);
        definedPermission.setGrantedByDefault(true);
        definedPermission.setLdapEntry(new ReadOnlyEntry("uniqueId", new Attribute[0]));
        definedPermission.setPermissionType("permissionType");
        definedPermission.setTitle("title");
        definedPermission.setValue("value");
        assertThat("returns hashcode", definedPermission.hashCode(), equalTo(1955087924));
    }

    @Test
    public void equals_objectsAreDifferentClasses_returnsFalse() throws Exception {
        assertThat("equals",definedPermission.equals(new Permission()),equalTo(false));
    }

    @Test
    public void equals_descriptionIsNullButOtherDescriptionNotNull_returnsFalse() throws Exception {
        definedPermission1.setDescription("description");
        assertThat("equals",definedPermission.equals(definedPermission1),equalTo(false));
    }

    @Test
    public void equals_descriptionsExistButNotEqual_returnsFalse() throws Exception {
        definedPermission1.setDescription("aDescription");
        definedPermission.setDescription("anotherDescription");
        assertThat("equals",definedPermission.equals(definedPermission1),equalTo(false));
    }

    @Test
    public void equals_enabledNullButOtherEnabledNotNull_returnsFalse() throws Exception {
        definedPermission1.setEnabled(true);
        assertThat("equals",definedPermission.equals(definedPermission1),equalTo(false));
    }

    @Test
    public void equals_bothEnabledNotNullButNotEqual_returnsFalse() throws Exception {
        definedPermission1.setEnabled(true);
        definedPermission.setEnabled(false);
        assertThat("equals",definedPermission.equals(definedPermission1),equalTo(false));
    }

    @Test
    public void equals_grantedByDefaultNullButOtherGrantedByDefaultNotNull_returnsFalse() throws Exception {
        definedPermission1.setGrantedByDefault(true);
        assertThat("equals",definedPermission.equals(definedPermission1),equalTo(false));
    }

    @Test
    public void equals_grantedByDefaultsBothExistButNotEqual_returnsFalse() throws Exception {
        definedPermission1.setGrantedByDefault(true);
        definedPermission.setGrantedByDefault(false);
        assertThat("equals", definedPermission.equals(definedPermission1), equalTo(false));
    }

    @Test
    public void equals_ldapEntryNullButOtherLdapEntryExists_returnsFalse() throws Exception {
        definedPermission1.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        assertThat("equals",definedPermission.equals(definedPermission1),equalTo(false));
    }

    @Test
    public void equals_bothLdapEntriesExistButNotEqual_returnsFalse() throws Exception {
        definedPermission1.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        definedPermission.setLdapEntry(new ReadOnlyEntry("anotherUniqueId",new Attribute[0]));
        assertThat("equals", definedPermission.equals(definedPermission1), equalTo(false));
    }

    @Test
    public void equals_permissionTypeIsNullButOtherPermissionTypeExists_returnsFalse() throws Exception {
        definedPermission1.setPermissionType("permissionType");
        assertThat("equals",definedPermission.equals(definedPermission1),equalTo(false));
    }

    @Test
    public void equals_bothPermissionTypesExistButNotEqual_returnsFalse() throws Exception {
        definedPermission1.setPermissionType("permissionType");
        definedPermission.setPermissionType("anotherPermissionType");
        assertThat("equals", definedPermission.equals(definedPermission1), equalTo(false));
    }

    @Test
    public void equals_titleIsNullButOtherTitleExists_returnsFalse() throws Exception {
        definedPermission1.setTitle("title");
        assertThat("equals",definedPermission.equals(definedPermission1),equalTo(false));
    }

    @Test
    public void equals_bothTitlesExistButNotEqual_returnsFalse() throws Exception {
        definedPermission1.setTitle("title");
        definedPermission.setTitle("anotherTitle");
        assertThat("equals", definedPermission.equals(definedPermission1), equalTo(false));
    }

    @Test
    public void equals_valueIsNullButOtherValueNotNull_returnsFalse() throws Exception {
        definedPermission1.setValue("value");
        assertThat("equals",definedPermission.equals(definedPermission1),equalTo(false));
    }

    @Test
    public void equals_bothValuesExistButNotEqual_returnsFalse() throws Exception {
        definedPermission1.setValue("value");
        definedPermission.setValue("anotherValue");
        assertThat("equals", definedPermission.equals(definedPermission1), equalTo(false));
    }

    @Test
    public void equals_withAllFieldsSetAndEqual_returnsTrue() throws Exception {
        definedPermission1.setDescription("aDescription");
        definedPermission.setDescription("aDescription");
        definedPermission1.setEnabled(true);
        definedPermission.setEnabled(true);
        definedPermission1.setGrantedByDefault(true);
        definedPermission.setGrantedByDefault(true);
        definedPermission1.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        definedPermission.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        definedPermission1.setPermissionType("permissionType");
        definedPermission.setPermissionType("permissionType");
        definedPermission1.setTitle("title");
        definedPermission.setTitle("title");
        definedPermission1.setValue("value");
        definedPermission.setValue("value");

        assertThat("equals", definedPermission.equals(definedPermission1), equalTo(true));
    }

    @Test
    public void copyChanges_modifiedPermissionTypeNotNullAndNotBlank_copiesPermissionType() throws Exception {
        definedPermission1.setPermissionType("permissionType");
        spy.copyChanges(definedPermission1);
        verify(spy).setPermissionType("permissionType");
    }

    @Test
    public void copyChanges_modifiedPermissionTypeNull_doesNotSetPermissionType() throws Exception {
        spy.copyChanges(definedPermission1);
        verify(spy,never()).setPermissionType(anyString());
    }

    @Test
    public void copyChanges_modifiedValueNotNull_copiesValue() throws Exception {
        definedPermission1.setValue("value");
        spy.copyChanges(definedPermission1);
        verify(spy).setValue("value");
    }

    @Test
    public void copyChanges_modifiedValueNull_doesNotSetValue() throws Exception {
        spy.copyChanges(definedPermission1);
        verify(spy,never()).setValue(anyString());
    }

    @Test
    public void copyChanges_modifiedDescriptionNotNull_copiesDescription() throws Exception {
        definedPermission1.setDescription("description");
        spy.copyChanges(definedPermission1);
        verify(spy).setDescription("description");
    }

    @Test
    public void copyChanges_modifiedDescriptionNull_doesNotSetDescription() throws Exception {
        spy.copyChanges(definedPermission1);
        verify(spy,never()).setDescription(anyString());
    }

    @Test
    public void copyChanges_modifiedTitleNotNull_copiesTitle() throws Exception {
        definedPermission1.setTitle("title");
        spy.copyChanges(definedPermission1);
        verify(spy).setTitle("title");
    }

    @Test
    public void copyChanges_modifiedTitleNull_doesNotSetTitle() throws Exception {
        spy.copyChanges(definedPermission1);
        verify(spy,never()).setTitle(anyString());
    }

    @Test
    public void copyChanges_modifiedEnabledNotNull_copiesEnabled() throws Exception {
        definedPermission1.setEnabled(true);
        spy.copyChanges(definedPermission1);
        verify(spy).setEnabled(true);
    }

    @Test
    public void copyChanges_modifiedEnabledNull_doesNotSetEnabled() throws Exception {
        spy.copyChanges(definedPermission1);
        verify(spy,never()).setEnabled(anyBoolean());
    }

    @Test
    public void copyChanges_modifiedGrantedByDefaultNotNull_copiesGrantedByDefault() throws Exception {
        definedPermission1.setGrantedByDefault(true);
        spy.copyChanges(definedPermission1);
        verify(spy).setGrantedByDefault(true);
    }

    @Test
    public void copyChanges_modifiedGrantedByDefaultNull_doesNotSetGrantedByDefault() throws Exception {
        spy.copyChanges(definedPermission1);
        verify(spy,never()).setGrantedByDefault(anyBoolean());
    }
}
