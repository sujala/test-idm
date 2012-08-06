package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.UserSecret;
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
 * Time: 3:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserSecretTest {
    JSONReaderForUserSecret jsonReaderForUserSecret;

    @Before
    public void setUp() throws Exception {
        jsonReaderForUserSecret = new JSONReaderForUserSecret();
    }

    @Test
    public void isReadable_typeIsUserSecret_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForUserSecret.isReadable(UserSecret.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotUserSecret_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForUserSecret.isReadable(UserForCreate.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsUserSecret() throws Exception {
        String body = "{\n" +
                "    \"secret\":{\n" +
                "        \"secretQuestion\":\"What is the color of my eyes?\",\n" +
                "        \"secretAnswer\":\"Leonardo Da Vinci\"\n" +
                "    }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("UserSecret",jsonReaderForUserSecret.readFrom(UserSecret.class, null, null, null, null, inputStream),instanceOf(UserSecret.class));
    }

    @Test
    public void getUserSecretFromJSONString_validJsonBody_returnsUserSecretCorrectSecretQuestion() throws Exception {
        String body ="{\n" +
                "    \"secret\":{\n" +
                "        \"secretQuestion\":\"What is the color of my eyes?\",\n" +
                "        \"secretAnswer\":\"Leonardo Da Vinci\"\n" +
                "    }\n" +
                "}";
        assertThat("userSecret", JSONReaderForUserSecret.getUserSecretFromJSONString(body).getSecretQuestion(), equalTo("What is the color of my eyes?"));
    }

    @Test
    public void getUserSecretFromJSONString_validJsonBody_returnsUserSecretCorrectSecretAnswer() throws Exception {
        String body = "{\n" +
                "    \"secret\":{\n" +
                "        \"secretQuestion\":\"What is the color of my eyes?\",\n" +
                "        \"secretAnswer\":\"Leonardo Da Vinci\"\n" +
                "    }\n" +
                "}";
        assertThat("userSecret", JSONReaderForUserSecret.getUserSecretFromJSONString(body).getSecretAnswer(), equalTo("Leonardo Da Vinci"));
    }

    @Test
    public void getUserSecretFromJSONString_passwordCredentialInput_returnsEmptyUserSecret() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "            \"passwordCredentials\":{ \n" +
                "            \"username\": \"jsmith\",\n" +
                "            \"password\": \"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertThat("userSecret", JSONReaderForUserSecret.getUserSecretFromJSONString(body).getSecretAnswer(), nullValue());
        assertThat("userSecret", JSONReaderForUserSecret.getUserSecretFromJSONString(body).getSecretQuestion(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getUserSecretFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForUserSecret.getUserSecretFromJSONString(body);
    }

    @Test
    public void getUserSecretFromJSONString_nullSecretAnswer_returnsUserSecretNullSecretAnswer() throws Exception {
        String body = "{\n" +
                "    \"secret\":{\n" +
                "        \"secretQuestion\":\"What is the color of my eyes?\",\n" +
                "    }\n" +
                "}";

        assertThat("secretQA", JSONReaderForUserSecret.getUserSecretFromJSONString(body).getSecretAnswer(), nullValue());
    }

    @Test
    public void getUserSecretFromJSONString_nullSecretQuestion_returnsUserSecretNullSecretQuestion() throws Exception {
        String body = "{\n" +
                "    \"secret\":{\n" +
                "        \"secretAnswer\":\"Leonardo Da Vinci\"\n" +
                "    }\n" +
                "}";
        assertThat("secretQA", JSONReaderForUserSecret.getUserSecretFromJSONString(body).getSecretQuestion(), nullValue());
    }
}
