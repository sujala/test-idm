package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.exception.BadRequestException;
import org.hamcrest.Matchers;
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

    JSONReaderForApiKeyCredentials jsonReaderForApiKey;
    String jsonBody = "{\n" +
            "    \"RAX-KSKEY:apiKeyCredentials\":{\n" +
            "        \"username\":\"jsmith\",\n" +
            "        \"apiKey\":\"aaaaa-bbbbb-ccccc-12345678\"\n" +
            "    }\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        jsonReaderForApiKey = new JSONReaderForApiKeyCredentials();
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

    @Test
    public void getApiKeyCredentialsFromJSONString_withEmptyJSON_returnsNewApiKeyCredentials() throws Exception {
        ApiKeyCredentials apiKeyCredentialsFromJSONString = JSONReaderForApiKeyCredentials.getApiKeyCredentialsFromJSONString("{ }");
        assertThat("credential apiKey", apiKeyCredentialsFromJSONString.getApiKey(), nullValue());
    }

    @Test
    public void getApiKeyCredentialsFromJSONString_withValidString_returnsCorrectCredentialApiKey() throws Exception {
        ApiKeyCredentials apiKeyCredentialsFromJSONString = JSONReaderForApiKeyCredentials.getApiKeyCredentialsFromJSONString(jsonBody);
        assertThat("credential's apiKey", apiKeyCredentialsFromJSONString.getApiKey(), equalTo("aaaaa-bbbbb-ccccc-12345678"));
    }

    @Test
    public void getApiKeyCredentialsFromJSONString_withValidString_returnsCorrectCredentialUsername() throws Exception {
        ApiKeyCredentials apiKeyCredentialsFromJSONString = JSONReaderForApiKeyCredentials.getApiKeyCredentialsFromJSONString(jsonBody);
        assertThat("credential's username", apiKeyCredentialsFromJSONString.getUsername(), equalTo("jsmith"));
    }

    @Test(expected = BadRequestException.class)
    public void getApiKeyCredentialsFromJSONString_withInvalidString_throwsBadRequestException() throws Exception {
        JSONReaderForApiKeyCredentials.getApiKeyCredentialsFromJSONString("th1s, Is. Not* a valid() JSON Stri_ng");
    }

    @Test
    public void checkAndGetApiKeyCredentialsFromJSONString_withValidCredentials_returnsCredentialsWithCorrectUsername() throws Exception {
        ApiKeyCredentials apiKeyCredentials = JSONReaderForApiKeyCredentials.checkAndGetApiKeyCredentialsFromJSONString(jsonBody);
        assertThat("credential's username", apiKeyCredentials.getUsername(), equalTo("jsmith"));
    }

    @Test
    public void checkAndGetApiKeyCredentialsFromJSONString_withValidCredentials_returnsCredentialsWithCorrectApiKey() throws Exception {
        ApiKeyCredentials apiKeyCredentials = JSONReaderForApiKeyCredentials.checkAndGetApiKeyCredentialsFromJSONString(jsonBody);
        assertThat("credential's apiKey", apiKeyCredentials.getApiKey(), equalTo("aaaaa-bbbbb-ccccc-12345678"));
    }

    @Test
    public void getApiKeyCredentialsFromJSONString_withInvalidUsername_returnsNullUsername() throws Exception {
        String brokenJsonBody = "{\n" +
                "    \"RAX-KSKEY:apiKeyCredentials\":{\n" +
                "        \"apiKey\":\"aaaaa-bbbbb-ccccc-12345678\"\n" +
                "    }\n" +
                "}";
        ApiKeyCredentials apiKeyCredentials = JSONReaderForApiKeyCredentials.getApiKeyCredentialsFromJSONString(brokenJsonBody);
        assertThat("apiKeyCredentials username", apiKeyCredentials.getUsername(), nullValue());
    }

    @Test
    public void getApiKeyCredentialsFromJSONString_withInvalidApiKey_returnsNullApiKey() throws Exception {
        String brokenJsonBody = "{\n" +
                "    \"RAX-KSKEY:apiKeyCredentials\":{\n" +
                "        \"username\":\"jsmith\",\n" +
                "    }\n" +
                "}";
        ApiKeyCredentials apiKeyCredentials = JSONReaderForApiKeyCredentials.getApiKeyCredentialsFromJSONString(brokenJsonBody);
        assertThat("apiKeyCredentials apiKey", apiKeyCredentials.getApiKey(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void checkAndGetApiKeyCredentialsFromJSONString_withInvalidUsername_throwsBadRequestException() throws Exception {
        String brokenJsonBody = "{\n" +
                "    \"RAX-KSKEY:apiKeyCredentials\":{\n" +
                "        \"apiKey\":\"aaaaa-bbbbb-ccccc-12345678\"\n" +
                "    }\n" +
                "}";
        JSONReaderForApiKeyCredentials.checkAndGetApiKeyCredentialsFromJSONString(brokenJsonBody);
    }

    @Test(expected = BadRequestException.class)
    public void checkAndGetApiKeyCredentialsFromJSONString_withInvalidApiKey_throwsBadRequestException() throws Exception {
        String brokenJsonBody = "{\n" +
                "    \"RAX-KSKEY:apiKeyCredentials\":{\n" +
                "        \"username\":\"jsmith\",\n" +
                "    }\n" +
                "}";
        JSONReaderForApiKeyCredentials.checkAndGetApiKeyCredentialsFromJSONString(brokenJsonBody);
    }

    @Test(expected = BadRequestException.class)
    public void checkAndGetApiKeyCredentialsFromJSONString_withInvalidString_throwsBadRequestException() throws Exception {
        String brokenJsonBody = "Not Valid JSON";
        JSONReaderForApiKeyCredentials.checkAndGetApiKeyCredentialsFromJSONString(brokenJsonBody);
    }
}
