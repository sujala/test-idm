package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.exception.BadRequestException;
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
public class JSONReaderForRoleTest {
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

    @Test
    public void getRoleFromJSONString_validJsonBody_returnsRoleCorrectId() throws Exception {
        String body ="{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"description\": \"Guest Access\"\n" +
                "  }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getId(), equalTo("123"));
    }

    @Test
    public void getRoleFromJSONString_validJsonBody_returnsRoleCorrectName() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"description\": \"Guest Access\"\n" +
                "  }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getName(), equalTo("Guest"));
    }

    @Test
    public void getRoleFromJSONString_validJsonBody_returnsRoleCorrectDescription() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"description\": \"Guest Access\"\n" +
                "  }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getDescription(), equalTo("Guest Access"));
    }

    @Test
    public void getRoleFromJSONString_validJsonBody_returnsRoleCorrectTenantID() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"description\": \"Guest Access\"\n" +
                "  }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getTenantId(), equalTo("456"));
    }

    @Test
    public void getRoleFromJSONString_validJsonBody_returnsRoleCorrectServiceID() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"description\": \"Guest Access\"\n" +
                "  }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getServiceId(), equalTo("789"));
    }

    @Test
    public void getRoleFromJSONString_passwordCredentialInput_returnsEmptyRole() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getId(), nullValue());
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getName(), nullValue());
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getDescription(), nullValue());
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getTenantId(), nullValue());
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getServiceId(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getRoleFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForRole.getRoleFromJSONString(body);
    }

    @Test
    public void getRoleFromJSONString_nullID_returnsRoleNullID() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"description\": \"Guest Access\"\n" +
                "  }\n" +
                "}";

        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getId(), nullValue());
    }

    @Test
    public void getRoleFromJSONString_nullName_returnsRoleNullName() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "    \"description\": \"Guest Access\"\n" +
                "  }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getName(), nullValue());
    }

    @Test
    public void getRoleFromJSONString_nullDescription_returnsRoleNullDescription() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "  }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getDescription(), nullValue());
    }

    @Test
    public void getRoleFromJSONString_nullTenantID_returnsRoleNullTenantID() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"description\": \"Guest Access\"\n" +
                "    \"serviceId\": \"789\"\n" +
                "  }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getTenantId(), nullValue());
    }

    @Test
    public void getRoleFromJSONString_nullServiceID_returnsRoleNullServiceID() throws Exception {
        String body = "{\n" +
                "  \"role\": {\n" +
                "    \"id\": \"123\",\n" +
                "    \"name\": \"Guest\",\n" +
                "    \"tenantId\": \"456\"\n" +
                "    \"description\": \"Guest Access\"\n" +
                "  }\n" +
                "}";
        assertThat("role", JSONReaderForRole.getRoleFromJSONString(body).getServiceId(), nullValue());
    }
}
