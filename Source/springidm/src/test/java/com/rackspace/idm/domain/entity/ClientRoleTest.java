package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: yung5027
 * Date: 7/26/12
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClientRoleTest {
    ClientRole clientRole;

    @Before
    public void setUp() throws Exception {
        clientRole = new ClientRole();
    }

    @Test
    public void getLdapEntry_returnsLdapEntry() throws Exception {
        ReadOnlyEntry result = clientRole.getLDAPEntry();
        assertThat("ldap entry", result, equalTo(null));
    }

    @Test
    public void copyChanges_modifiedClientDescriptionIsBlank_setsDescriptionToNull() throws Exception {
        clientRole.setDescription("notNull");
        ClientRole modifiedClient = new ClientRole();
        modifiedClient.setDescription("");
        clientRole.copyChanges(modifiedClient);
        assertThat("description", clientRole.getDescription(), equalTo(null));
    }

    @Test
    public void copyChanges_modifiedClientDescriptionIsNull_setsDescriptionToNull() throws Exception {
        clientRole.setDescription("notNull");
        ClientRole modifiedClient = new ClientRole();
        modifiedClient.setDescription(null);
        clientRole.copyChanges(modifiedClient);
        assertThat("description", clientRole.getDescription(), equalTo(null));
    }

    @Test
    public void copyChanges_modifiedClientDescriptionNotBlank_setsDescriptionToModifiedClientDescription() throws Exception {
        clientRole.setDescription("notNull");
        ClientRole modifiedClient = new ClientRole();
        modifiedClient.setDescription("newDescription");
        clientRole.copyChanges(modifiedClient);
        assertThat("description", clientRole.getDescription(), equalTo("newDescription"));
    }
}
