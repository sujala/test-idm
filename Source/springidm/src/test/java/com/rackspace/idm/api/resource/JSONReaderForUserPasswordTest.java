package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;

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
 * Date: 6/7/12
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserPasswordTest {
    JSONReaderForUserPassword jsonReaderForUserPassword;

    @Before
    public void setUp() throws Exception {
        jsonReaderForUserPassword = new JSONReaderForUserPassword();
    }

    @Test
    public void isReadable_typeIsUserPassword_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForUserPassword.isReadable(UserPassword.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotUserPassword_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForUserPassword.isReadable(UserForCreate.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsUserPassword() throws Exception {
        String body = "{\n" +
                "    \"userPassword\":{\n" +
                "        \"password\":\"bananas\",\n" +
                "    }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("userPassword",jsonReaderForUserPassword.readFrom(UserPassword.class, null, null, null, null, inputStream),instanceOf(UserPassword.class));
    }

    @Test
    public void getUserPasswordFromJSONString_validJsonBody_returnsUserPasswordCorrectPassword() throws Exception {
        String body ="{\n" +
                "    \"userPassword\":{\n" +
                "        \"password\":\"bananas\",\n" +
                "    }\n" +
                "}";
        assertThat("userPassword", JSONReaderForUserPassword.getUserPasswordFromJSONString(body).getPassword(), equalTo("bananas"));
    }

    @Test
    public void getUserPasswordFromJSONString_passwordCredentialInput_returnsEmptyUserPassword() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "            \"passwordCredentials\":{ \n" +
                "            \"username\": \"jsmith\",\n" +
                "            \"password\": \"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertThat("userPassword", JSONReaderForUserPassword.getUserPasswordFromJSONString(body).getPassword(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getUserPasswordFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForUserPassword.getUserPasswordFromJSONString(body);
    }

    @Test
    public void getUserPasswordFromJSONString_nullPassword_returnsUserPasswordNullPassword() throws Exception {
        String body = "{\n" +
                "    \"userPassword\":{\n" +
                "    }\n" +
                "}";

        assertThat("secretQA", JSONReaderForUserPassword.getUserPasswordFromJSONString(body).getPassword(), nullValue());
    }

    @Test
    public void getUserPasswordFromJSONStringWithoutWrapper_validJsonBody_returnsUserPasswordCorrectPassword() throws Exception {
        String body ="{\n" +
                "        \"password\":\"bananas\",\n" +
                "}";
        assertThat("userPassword", JSONReaderForUserPassword.getUserPasswordFromJSONStringWithoutWrapper(body).getPassword(), equalTo("bananas"));
    }

    @Test
    public void getUserPasswordFromJSONStringWithoutWrapper_passwordCredentialInput_returnsEmptyUserPassword() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "            \"passwordCredentials\":{ \n" +
                "            \"username\": \"jsmith\",\n" +
                "            \"password\": \"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertThat("userPassword", JSONReaderForUserPassword.getUserPasswordFromJSONStringWithoutWrapper(body).getPassword(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getUserPasswordFromJSONStringWithoutWrapper_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForUserPassword.getUserPasswordFromJSONStringWithoutWrapper(body);
    }

    @Test
    public void getUserPasswordFromJSONStringWithoutWrapper_nullPassword_returnsUserPasswordNullPassword() throws Exception {
        String body = "{\n" +
                "}";

        assertThat("secretQA", JSONReaderForUserPassword.getUserPasswordFromJSONStringWithoutWrapper(body).getPassword(), nullValue());
    }
}
