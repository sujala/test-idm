package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForRole;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.v2.Role;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/6/12
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForRoleTestOld {
    JSONReaderForRole jsonReaderForRole;

    @Before
    public void setUp() throws Exception {
        jsonReaderForRole = new JSONReaderForRole();
    }

    @Test
    public void isReadable_typeIsRole_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForRole.isReadable(Role.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotRole_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForRole.isReadable(UserForCreate.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsRole() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"description\": \"Guest Access\"\n" +
                "  }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("role",jsonReaderForRole.readFrom(Role.class, null, null, null, null, inputStream),instanceOf(Role.class));
    }

}
