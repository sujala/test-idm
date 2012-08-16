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
    public void getLdapEntry_returnsLdapEntry() throws Exception {
        ReadOnlyEntry readOnlyEntry = new ReadOnlyEntry("uniqueId", new Attribute[0]);
        tenant.setLdapEntry(readOnlyEntry);
        ReadOnlyEntry result = tenant.getLDAPEntry();
        assertThat("ldap entry", result, equalTo(readOnlyEntry));
    }

    @Test
    public void addBaseUrlId_baseUrlDoesContainUrlId_doesNotRemoveId() throws Exception {
        String[] test = {"123"};
        tenant.setBaseUrlIds(test);
        tenant.addBaseUrlId("123");
        String[] result = tenant.getBaseUrlIds();
        assertThat("base url id", result[0], equalTo("123"));
        assertThat("string list", result.length, equalTo(1));
    }

    @Test
    public void addV1Default_baseUrl() throws Exception {
        String[] test = {"123"};
        tenant.setV1Defaults(test);
        tenant.addV1Default("123");
        String[] result = tenant.getV1Defaults();
        assertThat("base url id", result[0], equalTo("123"));
        assertThat("string list", result.length, equalTo(1));
    }

    @Test
    public void removeBaseUrlId_baseUrlDoesNotContainUrlId_doesNotRemoveId() throws Exception {
        String[] test = {"245","123"};
        tenant.setBaseUrlIds(test);
        tenant.removeBaseUrlId("123");
        String[] result = tenant.getBaseUrlIds();
        assertThat("base url id", result[0], equalTo("245"));
        assertThat("string list", result.length, equalTo(1));
    }

    @Test
    public void removeV1Default_baseUrl() throws Exception {
        String[] test = {"245","123"};
        tenant.setV1Defaults(test);
        tenant.removeV1Default("123");
        String[] result = tenant.getV1Defaults();
        assertThat("base url id", result[0], equalTo("245"));
        assertThat("string list", result.length, equalTo(1));
    }

    @Test
    public void containsBaseUrlId_baseUrlIdsIsNull_returnFalse() throws Exception {
        tenant.setBaseUrlIds(null);
        boolean result = tenant.containsBaseUrlId("123");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void containsBaseUrlId_baseUrlIdsLengthIs0_returnsFalse() throws Exception {
        tenant.setBaseUrlIds(new String[0]);
        boolean result = tenant.containsBaseUrlId("123");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void containsV1Default_baseUrlIdsLengthIs0_returnsFalse() throws Exception {
        tenant.setV1Defaults(new String[0]);
        boolean result = tenant.containsV1Default("123");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void containsBaseUrlId_baseUrlIdsLengthNotZero_returnsTrue() throws Exception {
        String[] test = {"123"};
        tenant.setBaseUrlIds(test);
        boolean result = tenant.containsBaseUrlId("123");
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void containsV1Default_baseUrlIdsLengthNotZero_returnsTrue() throws Exception {
        String[] test = {"123"};
        tenant.setV1Defaults(test);
        boolean result = tenant.containsV1Default("123");
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void copyChanges_attributesNotNullAndNotBlank_copiesChanges() throws Exception {
        Tenant modifiedTenant = new Tenant();
        modifiedTenant.setDescription("newDescription");
        modifiedTenant.setName("newName");
        modifiedTenant.setEnabled(true);
        tenant.copyChanges(modifiedTenant);
        assertThat("description", tenant.getDescription(), equalTo("newDescription"));
        assertThat("name", tenant.getName(), equalTo("newName"));
        assertThat("enabled", tenant.isEnabled(), equalTo(true));
    }
    
    @Test
    public void copyChanges_attributesNotNullAndIsBlank_setsNull() throws Exception {
        Tenant modifiedTenant = new Tenant();
        modifiedTenant.setDescription("");
        modifiedTenant.setName("");
        modifiedTenant.setEnabled(null);

        tenant.setDescription("Description");
        tenant.setName("Name");
        tenant.setEnabled(true);

        tenant.copyChanges(modifiedTenant);
        assertThat("description", tenant.getDescription(), equalTo(null));
        assertThat("name", tenant.getName(), equalTo(null));
        assertThat("enabled", tenant.isEnabled(), equalTo(true));
    }

    @Test
    public void copyChanges_attributesNull_doesNotChangeAttributes() throws Exception {
        Tenant modifiedTenant = new Tenant();
        modifiedTenant.setDescription(null);
        modifiedTenant.setName(null);
        modifiedTenant.setEnabled(null);

        tenant.setDescription("Description");
        tenant.setName("Name");
        tenant.setEnabled(true);

        tenant.copyChanges(modifiedTenant);
        assertThat("description", tenant.getDescription(), equalTo("Description"));
        assertThat("name", tenant.getName(), equalTo("Name"));
        assertThat("enabled", tenant.isEnabled(), equalTo(true));
    }
}
