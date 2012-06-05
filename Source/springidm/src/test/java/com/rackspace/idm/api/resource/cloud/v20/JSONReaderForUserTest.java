package com.rackspace.idm.api.resource.cloud.v20;


import com.rackspace.idm.exception.BadRequestException;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.User;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/5/12
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserTest {

    JSONReaderForUser jsonReaderForUser;

    @Before
    public void setUp() throws Exception {
        jsonReaderForUser = new JSONReaderForUser();
    }

    @Test
    public void isReadable_typeIsUser_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForUser.isReadable(User.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotUser_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForUser.isReadable(Tenant.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsUser() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"display-name\": \"acme\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("user",jsonReaderForUser.readFrom(User.class, null, null, null, null, inputStream),instanceOf(User.class));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectId() throws Exception {
        String body =
                "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"display-name\": \"john\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getId(), equalTo("123456"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectUserName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"display-name\": \"john\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getUsername(), equalTo("jqsmith"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectDisplayName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"display-name\": \"john\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getDisplayName(), equalTo("john"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserCorrectEmail() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"display-name\": \"john\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getEmail(), equalTo("john.smith@example.org"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserNotEnabled() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"display-name\": \"john\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"enabled\": false\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).isEnabled(), equalTo(false));
    }

    @Test
    public void getUserFromJSONString_passwordCredentialInput_returnsEmptyUser() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getId(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getUsername(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getDisplayName(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getEmail(), nullValue());
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).isEnabled(), equalTo(true));
    }

    @Test(expected = BadRequestException.class)
    public void getUserFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForUser.getUserFromJSONString(body);
    }

    @Test
    public void getUserFromJSONString_nullID_returnsUserNullID() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"display-name\": \"john\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getId(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullUserName_returnsUserNullUserName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"display-name\": \"john\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getUsername(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullDisplayName_returnsUserNullDisplayName() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getDisplayName(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullEmail_returnsTenantNullEmail() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"display-name\": \"john\",\n" +
                "    \"enabled\": true\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).getEmail(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullEnable_returnsUserEnableTrue() throws Exception {
        String body = "{\n" +
                "  \"user\": {\n" +
                "    \"id\": \"123456\",\n" +
                "    \"username\": \"jqsmith\",\n" +
                "    \"email\": \"john.smith@example.org\",\n" +
                "    \"display-name\": \"john\",\n" +
                "  }\n" +
                "}";
        assertThat("user", JSONReaderForUser.getUserFromJSONString(body).isEnabled(), equalTo(true));
    }
}
