package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

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

    @Test(expected = BadRequestException.class)
    public void checkAndGetCredentialsFromJSONString_withValidJSONAndInvalidCredentialType_throwsBadRequestException() throws Exception {
        JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString(invalidCredentialTypeJSON);
    }

    @Test(expected = BadRequestException.class)
    public void checkAndGetCredentialsFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForCredentialType.checkAndGetCredentialsFromJSONString("this invalid JSON");
    }
}
