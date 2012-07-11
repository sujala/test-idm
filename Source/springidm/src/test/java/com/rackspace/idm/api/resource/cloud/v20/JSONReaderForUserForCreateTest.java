package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.v2.Tenant;

import javax.xml.namespace.QName;
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
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserForCreateTest {


    JSONReaderForUserForCreate jsonReaderForUserForCreate;

    @Before
    public void setUp() throws Exception {
        jsonReaderForUserForCreate = new JSONReaderForUserForCreate();
    }

    @Test
    public void isReadable_typeIsUserForCreate_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForUserForCreate.isReadable(UserForCreate.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotUserForCreate_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForUserForCreate.isReadable(Tenant.class, null, null, null), equalTo(false));
    }

    @Test
    public void getUserFromJSONString_containsDefaultRegion_setRegion() throws Exception {
        String body = "{\n" +
                        "    \"user\": {\n" +
                        "            \"id\": \"123456\",\n" +
                        "            \"username\": \"cmarin1subX2\",\n" +
                        "            \"display-name\": \"marin\",\n" +
                        "            \"email\": \"cmarin1-sub@example.com\",\n" +
                        "            \"enabled\": false,\n" +
                        "            \"OS-KSADM:password\":\"Password48\",\n" +
                        "            \"RAX-AUTH:defaultRegion\":\"foo\"\n" +
                        "        }\n" +
                        "}";
        assertThat("default region", JSONReaderForUserForCreate.getUserFromJSONString(body).getOtherAttributes().get(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0","defaultRegion")), equalTo("foo"));
    }

    @Test
    public void readFrom_returnsUserForCreate() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "            \"id\": \"123456\",\n" +
                "            \"username\": \"cmarin1subX2\",\n" +
                "            \"display-name\": \"marin\",\n" +
                "            \"email\": \"cmarin1-sub@example.com\",\n" +
                "            \"enabled\": false,\n" +
                "            \"OS-KSADM:password\":\"Password48\"\n" +
                "        }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("userForCreate",jsonReaderForUserForCreate.readFrom(UserForCreate.class, null, null, null, null, inputStream),instanceOf(UserForCreate.class));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserForCreateCorrectId() throws Exception {
        String body ="{\n" +
                "    \"user\": {\n" +
                "            \"id\": \"123456\",\n" +
                "            \"username\": \"cmarin1subX2\",\n" +
                "            \"display-name\": \"marin\",\n" +
                "            \"email\": \"cmarin1-sub@example.com\",\n" +
                "            \"enabled\": false,\n" +
                "            \"OS-KSADM:password\":\"Password48\"\n" +
                "        }\n" +
                "}";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getId(), equalTo("123456"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserForCreateCorrectUserName() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "            \"id\": \"123456\",\n" +
                "            \"username\": \"cmarin1subX2\",\n" +
                "            \"display-name\": \"marin\",\n" +
                "            \"email\": \"cmarin1-sub@example.com\",\n" +
                "            \"enabled\": false,\n" +
                "            \"OS-KSADM:password\":\"Password48\"\n" +
                "        }\n" +
                "}";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getUsername(), equalTo("cmarin1subX2"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserForCreateCorrectDisplayName() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "            \"id\": \"123456\",     \n" +
                "            \"username\": \"cmarin1subX2\",     \n" +
                "            \"display-name\": \"marin\",     \n" +
                "            \"email\": \"cmarin1-sub@example.com\",     \n" +
                "            \"enabled\": false,     \n" +
                "            \"OS-KSADM:password\":\"Password48\"     \n" +
                "        }     \n" +
                "}\n";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getDisplayName(), equalTo("marin"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserForCreateCorrectEmail() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "            \"id\": \"123456\",     \n" +
                "            \"username\": \"cmarin1subX2\",     \n" +
                "            \"display-name\": \"marin\",     \n" +
                "            \"email\": \"cmarin1-sub@example.com\",     \n" +
                "            \"enabled\": false,     \n" +
                "            \"OS-KSADM:password\":\"Password48\"     \n" +
                "        }     \n" +
                "}\n";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getEmail(), equalTo("cmarin1-sub@example.com"));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserForCreateNotEnabled() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "            \"id\": \"123456\",     \n" +
                "            \"username\": \"cmarin1subX2\",     \n" +
                "            \"display-name\": \"marin\",     \n" +
                "            \"email\": \"cmarin1-sub@example.com\",     \n" +
                "            \"enabled\": false,     \n" +
                "            \"OS-KSADM:password\":\"Password48\"     \n" +
                "        }     \n" +
                "}\n";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).isEnabled(), equalTo(false));
    }

    @Test
    public void getUserFromJSONString_validJsonBody_returnsUserForCreateCorrectPassword() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "            \"id\": \"123456\",     \n" +
                "            \"username\": \"cmarin1subX2\",     \n" +
                "            \"display-name\": \"marin\",     \n" +
                "            \"email\": \"cmarin1-sub@example.com\",     \n" +
                "            \"enabled\": false,     \n" +
                "            \"OS-KSADM:password\":\"Password48\"     \n" +
                "        }     \n" +
                "}\n";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getPassword(), equalTo("Password48"));
    }

    @Test
    public void getUserFromJSONString_passwordCredentialInput_returnsEmptyUserForCreate() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getId(), nullValue());
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getUsername(), nullValue());
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getDisplayName(), nullValue());
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getPassword(), nullValue());
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getEmail(), nullValue());
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).isEnabled(), equalTo(true));
    }

    @Test(expected = BadRequestException.class)
    public void getUserFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForUserForCreate.getUserFromJSONString(body);
    }

    @Test
    public void getUserFromJSONString_nullID_returnsUserForCreateNullID() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "           \"username\": \"cmarin1subX2\",\n" +
                "           \"display-name\": \"marin\",\n" +
                "           \"email\": \"cmarin1-sub@example.com\",\n" +
                "           \"enabled\": false,\n" +
                "           \"OS-KSADM:password\": \"Password48\"\n" +
                "        }\n" +
                "}";

        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getId(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullUserName_returnsUserForCreateNullUserName() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "             \"id\": \"123456\",     \n" +
                "             \"display-name\": \"marin\",\n" +
                "             \"email\": \"cmarin1-sub@example.com\",\n" +
                "             \"enabled\": false,\n" +
                "             \"OS-KSADM:password\":\"Password48\"\n" +
                "        }\n" +
                "}";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getUsername(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullDisplayName_returnsUserForCreateNullDisplayName() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "             \"id\": \"123456\",     \n" +
                "            \"username\": \"cmarin1subX2\",\n" +
                "            \"email\": \"cmarin1-sub@example.com\",\n" +
                "            \"enabled\": false,\n" +
                "            \"OS-KSADM:password\":\"Password48\"\n" +
                "        }\n" +
                "}";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getDisplayName(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullEmail_returnsUserForCreateNullEmail() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "             \"id\": \"123456\",     \n" +
                "             \"username\": \"cmarin1subX2\",\n" +
                "             \"display-name\": \"marin\",\n" +
                "             \"enabled\": false,\n" +
                "             \"OS-KSADM:password\":\"Password48\"\n" +
                "        }\n" +
                "}";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getEmail(), nullValue());
    }

    @Test
    public void getUserFromJSONString_nullEnable_returnsUserForCreateEnableTrue() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "             \"id\": \"123456\",     \n" +
                "            \"username\": \"cmarin1subX2\",\n" +
                "             \"display-name\": \"marin\",\n" +
                "            \"email\": \"cmarin1-sub@example.com\",\n" +
                "            \"OS-KSADM:password\":\"Password48\"\n" +
                "        }\n" +
                "}";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).isEnabled(), equalTo(true));
    }

    @Test
    public void getUserFromJSONString_nullPassword_returnsUserForCreateNullPassword() throws Exception {
        String body = "{\n" +
                "    \"user\": {\n" +
                "             \"id\": \"123456\",     \n" +
                "            \"username\": \"cmarin1subX2\",\n" +
                "             \"display-name\": \"marin\",\n" +
                "             \"enabled\": false,\n" +
                "            \"email\": \"cmarin1-sub@example.com\",\n" +
                "        }\n" +
                "}";
        assertThat("userForCreate", JSONReaderForUserForCreate.getUserFromJSONString(body).getPassword(), nullValue());
    }
}
