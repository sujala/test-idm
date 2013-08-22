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
    public void setClientSecret_secretIsNull_doesNotSet() throws Exception {
        application.setClientSecret("secret");
        application.setClientSecret(null);
        String result = application.getClientSecret();
        assertThat("client secret", result, equalTo("secret"));
    }

    @Test
    public void setDefaults_setsEnabledToTrue() throws Exception {
        application.setDefaults();
        Boolean result = application.getEnabled();
        assertThat("enabled", result, equalTo(true));
    }

    @Test
    public void copyChanges_copiesAllAttributesThatIsNotNull() throws Exception {
        Application modifiedClient = new Application();
        modifiedClient.setRcn("newRCN");
        modifiedClient.setEnabled(true);
        modifiedClient.setCallBackUrl("newCallBackUrl");
        modifiedClient.setDescription("newDescription");
        modifiedClient.setScope("newScope");
        modifiedClient.setTitle("newTitle");
        application.copyChanges(modifiedClient);
        assertThat("rcn", application.getRcn(), equalTo("newRCN"));
        assertThat("enabled", application.getEnabled(), equalTo(true));
        assertThat("call back url", application.getCallBackUrl(), equalTo("newCallBackUrl"));
        assertThat("description", application.getDescription(), equalTo("newDescription"));
        assertThat("scope", application.getScope(), equalTo("newScope"));
        assertThat("title", application.getTitle(), equalTo("newTitle"));
    }
    
    @Test
    public void copyChanges_allAttributesIsNull_doesNotCopy() throws Exception {
        Application modifiedClient =  new Application();
        application.setRcn("RCN");
        application.setEnabled(true);
        application.setCallBackUrl("CallBackUrl");
        application.setDescription("Description");
        application.setScope("Scope");
        application.setTitle("Title"); 
        application.copyChanges(modifiedClient);
        assertThat("rcn", application.getRcn(), equalTo("RCN"));
        assertThat("enabled", application.getEnabled(), equalTo(true));
        assertThat("call back url", application.getCallBackUrl(), equalTo("CallBackUrl"));
        assertThat("description", application.getDescription(), equalTo("Description"));
        assertThat("scope", application.getScope(), equalTo("Scope"));
        assertThat("title", application.getTitle(), equalTo("Title"));
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
    public void equals_enabledNotNullAndNotEqualsObjectEnabled_returnsFalse() throws Exception {
        Application object = new Application();
        object.setEnabled(true);
        application.setEnabled(false);
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
    public void equals_nameIsNullAndObjectNameNotNull_returnsFalse() throws Exception {
        Application object = new Application();
        object.setName("notNull");
        application.setName(null);
        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_returnsTrue() throws Exception {
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        Application object = new Application();
        object.setCallBackUrl("callBackUrl");
        object.setClientSecret("clientSecret");
        object.setClientId("clientId");
        object.setDescription("description");
        object.setEnabled(true);
        object.setName("name");
        object.setOpenStackType("openStackType");
        object.setRcn("rcn");
        object.setTitle("title");
        object.setRoles(roles);
        object.setScope("scope");

        application.setCallBackUrl("callBackUrl");
        application.setClientSecret("clientSecret");
        application.setClientId("clientId");
        application.setDescription("description");
        application.setEnabled(true);
        application.setName("name");
        application.setOpenStackType("openStackType");
        application.setRcn("rcn");
        application.setTitle("title");
        application.setRoles(roles);
        application.setScope("scope");

        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(true));
    }
}
