package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
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
 * Date: 6/6/12
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForSecretQATest {

    JSONReaderForSecretQA jsonReaderForSecretQA;

    @Before
    public void setUp() throws Exception {
        jsonReaderForSecretQA = new JSONReaderForSecretQA();
    }

    @Test
    public void isReadable_typeIsSecretQA_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForSecretQA.isReadable(SecretQA.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotSecretQA_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForSecretQA.isReadable(UserForCreate.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsSecretQA() throws Exception {
        String body = "{\n" +
                "    \"RAX-KSQA:secretQA\":{\n" +
                "        \"question\":\"What is the color of my eyes?\",\n" +
                "        \"answer\":\"Leonardo Da Vinci\"\n" +
                "    }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("secretQA",jsonReaderForSecretQA.readFrom(SecretQA.class, null, null, null, null, inputStream),instanceOf(SecretQA.class));
    }

    @Test
    public void getSecretQAFromJSONString_validJsonBody_returnsSecretQACorrectAnswer() throws Exception {
        String body ="{\n" +
                "    \"RAX-KSQA:secretQA\":{\n" +
                "        \"question\":\"What is the color of my eyes?\",\n" +
                "        \"answer\":\"Leonardo Da Vinci\"\n" +
                "    }\n" +
                "}";
        assertThat("secretQA", JSONReaderForSecretQA.getSecretQAFromJSONString(body).getAnswer(), equalTo("Leonardo Da Vinci"));
    }

    @Test
    public void getSecretQAFromJSONString_validJsonBody_returnsSecretQACorrectQuestion() throws Exception {
        String body = "{\n" +
                "    \"RAX-KSQA:secretQA\":{\n" +
                "        \"question\":\"What is the color of my eyes?\",\n" +
                "        \"answer\":\"Leonardo Da Vinci\"\n" +
                "    }\n" +
                "}";
        assertThat("secretQA", JSONReaderForSecretQA.getSecretQAFromJSONString(body).getQuestion(), equalTo("What is the color of my eyes?"));
    }

    @Test
    public void getSecretQAFromJSONString_passwordCredentialInput_returnsEmptySecretQA() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "            \"passwordCredentials\":{ \n" +
                "            \"username\": \"jsmith\",\n" +
                "            \"password\": \"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertThat("secretQA", JSONReaderForSecretQA.getSecretQAFromJSONString(body).getAnswer(), nullValue());
        assertThat("secretQA", JSONReaderForSecretQA.getSecretQAFromJSONString(body).getQuestion(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getSecretQAFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForSecretQA.getSecretQAFromJSONString(body);
    }

    @Test
    public void getSecretQAFromJSONString_nullAnswer_returnsSecretQANullAnswer() throws Exception {
        String body = "{\n" +
                "    \"RAX-KSQA:secretQA\":{\n" +
                "        \"question\":\"What is the color of my eyes?\",\n" +
                "    }\n" +
                "}";

        assertThat("secretQA", JSONReaderForSecretQA.getSecretQAFromJSONString(body).getAnswer(), nullValue());
    }

    @Test
    public void getSecretQAFromJSONString_nullQuestion_returnsSecretQANullAnswer() throws Exception {
        String body = "{\n" +
                "    \"RAX-KSQA:secretQA\":{\n" +
                "        \"answer\":\"Leonardo Da Vinci\"\n" +
                "    }\n" +
                "}";
        assertThat("secretQA", JSONReaderForSecretQA.getSecretQAFromJSONString(body).getQuestion(), nullValue());
    }
}
