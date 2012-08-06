package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/5/12
 * Time: 5:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForCredentialTypeTest {


    String passwordCredentialsJSON = "{\n" +
            "    \"passwordCredentials\": {\n" +
            "        \"username\": \"test_user\",\n" +
            "        \"password\": \"resetpass\"\n" +
            "    }\n" +
            "}";

    String apiKeyCredentialsJSON = "{\n" +
            "    \"RAX-KSKEY:apiKeyCredentials\":{\n" +
            "        \"username\":\"jsmith\",\n" +
            "        \"apiKey\":\"aaaaa-bbbbb-ccccc-12345678\"\n" +
            "    }\n" +
            "}";
    String invalidCredentialTypeJSON = "{\n" +
            "    \"invalidCredentialType\":{\n" +
            "        \"username\":\"jsmith\",\n" +
            "        \"apiKey\":\"aaaaa-bbbbb-ccccc-12345678\"\n" +
            "    }\n" +
            "}";

    @Test
    public void checkAndGetCredentialsFromJSONString_withValidJSONAndPasswordCredentials_returnsPasswordCredentials() throws Exception {
        CredentialType credentialType = JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString(passwordCredentialsJSON);
        assertThat("credential type", credentialType, instanceOf(PasswordCredentialsRequiredUsername.class));
    }

    @Test
    public void checkAndGetCredentialsFromJSONString_withValidJSONAndApiKeyCredentials_returnsApiKeyCredentials() throws Exception {
        CredentialType credentialType = JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString(apiKeyCredentialsJSON);
        assertThat("credential type", credentialType, instanceOf(ApiKeyCredentials.class));
    }

    @Test
    public void checkAndGetCredentialsFromJSONString_withValidJSONAndInvalidCredentialType_throwsBadRequestException() throws Exception {
        try {
            JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString(invalidCredentialTypeJSON);
            assertTrue("expected exception", false);
        }catch (Exception e){
            assertThat("exception message", e.getMessage(), equalTo("Unsupported credential type"));
        }
    }

    @Test(expected = BadRequestException.class)
    public void checkAndGetCredentialsFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString("this invalid JSON");
    }
}
