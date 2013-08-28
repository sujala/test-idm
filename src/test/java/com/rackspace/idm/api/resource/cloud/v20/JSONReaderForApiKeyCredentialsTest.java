package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForRaxKsKeyApiKeyCredentials;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/5/12
 * Time: 3:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForApiKeyCredentialsTest {

    JSONReaderForRaxKsKeyApiKeyCredentials jsonReaderForApiKey;
    String jsonBody = "{\n" +
            "    \"RAX-KSKEY:apiKeyCredentials\":{\n" +
            "        \"username\":\"jsmith\",\n" +
            "        \"apiKey\":\"aaaaa-bbbbb-ccccc-12345678\"\n" +
            "    }\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        jsonReaderForApiKey = new JSONReaderForRaxKsKeyApiKeyCredentials();
    }

    @Test
    public void isReadable_withApiKeyCredentialsClass_returnsTrue() throws Exception {
        boolean readable = jsonReaderForApiKey.isReadable(ApiKeyCredentials.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withNonApiKeyCredentialsClass_returnsFalse() throws Exception {
        boolean readable = jsonReaderForApiKey.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidCredentials_returnsCorrectCredentials() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(jsonBody.getBytes()));
        ApiKeyCredentials apiKeyCredentials = jsonReaderForApiKey.readFrom(ApiKeyCredentials.class, null, null, null, null, inputStream);
        assertThat("returned credentials", apiKeyCredentials.getApiKey(), equalTo("aaaaa-bbbbb-ccccc-12345678"));
    }
}
