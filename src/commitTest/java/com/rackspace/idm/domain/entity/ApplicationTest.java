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
        modifiedClient.setEnabled(true);
        modifiedClient.setDescription("newDescription");
        application.copyChanges(modifiedClient);
        assertThat("enabled", application.getEnabled(), equalTo(true));
        assertThat("description", application.getDescription(), equalTo("newDescription"));
    }
    
    @Test
    public void copyChanges_allAttributesIsNull_doesNotCopy() throws Exception {
        Application modifiedClient =  new Application();
        application.setEnabled(true);
        application.setDescription("Description");
        application.copyChanges(modifiedClient);
        assertThat("enabled", application.getEnabled(), equalTo(true));
        assertThat("description", application.getDescription(), equalTo("Description"));
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
        object.setClientId("clientId");
        object.setDescription("description");
        object.setEnabled(true);
        object.setName("name");
        object.setOpenStackType("openStackType");
        object.setRoles(roles);

        application.setClientId("clientId");
        application.setDescription("description");
        application.setEnabled(true);
        application.setName("name");
        application.setOpenStackType("openStackType");
        application.setRoles(roles);

        boolean result = application.equals(object);
        assertThat("boolean", result, equalTo(true));
    }
}
