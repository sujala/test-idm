package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/17/12
 * Time: 10:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationTest {
    Application application;

    @Before
    public void setUp() throws Exception {
        application = new Application();
    }

    @Test
    public void setUniqueId_uniqueIdIsNull_doesNotSet() throws Exception {
        application.setUniqueId("notNull");
        application.setUniqueId(null);
        String result = application.getUniqueId();
        assertThat("unique id", result, equalTo("notNull"));
    }

    @Test
    public void setClientSecret_secretIsNull_doesNotSet() throws Exception {
        application.setClientSecret("secret");
        application.setClientSecret(null);
        String result = application.getClientSecret();
        assertThat("client secret", result, equalTo("secret"));
    }

    @Test
    public void setDefaults_setsEnabledToTrue() throws Exception {
        application.setDefaults();
        Boolean result = application.isEnabled();
        assertThat("enabled", result, equalTo(true));
    }

    @Test
    public void copyChanges_copiesAllAttributesThatIsNotNull() throws Exception {
        Application modifiedClient = new Application();
        modifiedClient.setRCN("newRCN");
        modifiedClient.setEnabled(true);
        modifiedClient.setCallBackUrl("newCallBackUrl");
        modifiedClient.setDescription("newDescription");
        modifiedClient.setScope("newScope");
        modifiedClient.setTitle("newTitle");
        application.copyChanges(modifiedClient);
        assertThat("rcn", application.getRCN(), equalTo("newRCN"));
        assertThat("enabled", application.isEnabled(), equalTo(true));
        assertThat("call back url", application.getCallBackUrl(), equalTo("newCallBackUrl"));
        assertThat("description", application.getDescription(), equalTo("newDescription"));
        assertThat("scope", application.getScope(), equalTo("newScope"));
        assertThat("title", application.getTitle(), equalTo("newTitle"));
    }
    
    @Test
    public void copyChanges_allAttributesIsNull_doesNotCopy() throws Exception {
        Application modifiedClient =  new Application();
        application.setRCN("RCN");
        application.setEnabled(true);
        application.setCallBackUrl("CallBackUrl");
        application.setDescription("Description");
        application.setScope("Scope");
        application.setTitle("Title"); 
        application.copyChanges(modifiedClient);
        assertThat("rcn", application.getRCN(), equalTo("RCN"));
        assertThat("enabled", application.isEnabled(), equalTo(true));
        assertThat("call back url", application.getCallBackUrl(), equalTo("CallBackUrl"));
        assertThat("description", application.getDescription(), equalTo("Description"));
        assertThat("scope", application.getScope(), equalTo("Scope"));
        assertThat("title", application.getTitle(), equalTo("Title"));
    }

    @Test
    public void hashCode_attributesNotNull_returnsHashCode() throws Exception {
        application.setRCN("RCN");
        application.setEnabled(true);
        application.setCallBackUrl("callBackUrl");
        application.setDescription("Description");
        application.setScope("Scope");
        application.setTitle("Title");
        application.setClientId("clientId");
        application.setClientSecret("clientSecret");
        application.setName("name");
        application.setOpenStackType("openStackType");
        application.setRoles(new ArrayList<TenantRole>());
        application.setUniqueId("uniqueId");
        int result = application.hashCode();
        assertThat("hash code", result, equalTo(-570848428));
    }

    @Test
    public void hashCode_attributesIsNull_returnsHashCode() throws Exception {
        application.setRCN(null);
        application.setEnabled(null);
        application.setCallBackUrl(null);
        application.setDescription(null);
        application.setScope(null);
        application.setTitle(null);
        application.setClientId(null);
        application.setClientSecret(null);
        application.setName(null);
        application.setOpenStackType(null);
        application.setRoles(null);
        application.setUniqueId(null);
        int result = application.hashCode();
        assertThat("hash code", result, equalTo(-293403007));
    }

    @Test
    public void equals_callBackUrlIsNullAndObjectCallBackUrlNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setCallBackUrl("notNull");
        application.setCallBackUrl(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_callBackUrlNotNullAndNotEqualObjectCallBackUrl_returnsFalse() throws Exception {
        Application object = new Application();
        object.setCallBackUrl("notNull");
        application.setCallBackUrl("notSame");
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_clientSecretIsNullAndObjectClientSecretNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setClientSecret("notNull");
        application.setClientSecret(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_descriptionIsNullAndObjectDescriptionNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setDescription("notNull");
        application.setDescription(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_descriptionNotNullAndNotEqualsObjectDescription_returnsFalse() throws Exception {
        Application object = new Application();
        object.setDescription("notNull");
        application.setDescription("notSame");
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_enabledIsNullAndObjectEnabledNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setEnabled(true);
        application.setEnabled(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_openStackTypeIsNullAndObjectOpenStackTypeNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setOpenStackType("notNull");
        application.setOpenStackType(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_openStackTypeNotNullAndNotEqualsObjectOpenStackType_returnsFalse() throws Exception {
        Application object = new Application();
        object.setOpenStackType("notNull");
        application.setOpenStackType("notSame");
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_RolesIsNullAndObjectRolesNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setRoles(new ArrayList<TenantRole>());
        application.setRoles(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_RolesNotNullAndNotEqualsObjectRoles_returnsFalse() throws Exception {
        Application object = new Application();
        object.setRoles(new ArrayList<TenantRole>());
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        roles.add(tenantRole);
        application.setRoles(roles);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_scopeIsNullAndObjectScopeNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setScope("notNull");
        application.setScope(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_scopeNotNullAndNotEqualsObjectScope_returnsFalse() throws Exception {
        Application object = new Application();
        object.setScope("notNull");
        application.setScope("notSame");
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_titleIsNullAndObjectTitleNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setTitle("notNull");
        application.setTitle(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_titleNotNullAndNotEqualsObjectTitle_returnsFalse() throws Exception {
        Application object = new Application();
        object.setTitle("notNull");
        application.setTitle("notSame");
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_uniqueIdIsNullAndObjectUniqueIdNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setUniqueId("notNull");
        application.setUniqueId(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_uniqueIdNotNullAndNotEqualsObjectUniqueId_returnsFalse() throws Exception {
        Application object = new Application();
        object.setUniqueId("notNull");
        application.setUniqueId("notSame");
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_returnsTrue() throws Exception {
        Application object = new Application();
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(true));
    }
}
