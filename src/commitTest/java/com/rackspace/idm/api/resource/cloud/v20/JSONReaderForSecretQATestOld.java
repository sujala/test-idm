package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForRaxKsQaSecretQA;
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
public class JSONReaderForSecretQATestOld {

    JSONReaderForRaxKsQaSecretQA jsonReaderForSecretQA;

    @Before
    public void setUp() throws Exception {
        jsonReaderForSecretQA = new JSONReaderForRaxKsQaSecretQA();
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

}
