package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.exception.BadRequestException;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/6/12
 * Time: 10:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForPasswordCredentialsTest {
    String passwordCredentialsJSON = "{" +
            "   \"passwordCredentials\": {" +
            "       \"username\": \"jsmith\"," +
            "       \"password\": \"secretPassword\"" +
            "   }" +
            "}";

    @Test
    public void getPasswordCredentialsFromJSONString_withUsername_setsUsername() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getPasswordCredentialsFromJSONString(passwordCredentialsJSON);
        assertThat("passwordCredentials username", passwordCredentialsFromJSONString.getUsername(), equalTo("jsmith"));
    }

    @Test
    public void getPasswordCredentialsFromJSONString_withPassword_setsPassword() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getPasswordCredentialsFromJSONString(passwordCredentialsJSON);
        assertThat("passwordCredentials password", passwordCredentialsFromJSONString.getPassword(), equalTo("secretPassword"));
    }

    @Test
    public void getPasswordCredentialsFromJSONString_withNoUsername_returnNullUsername() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getPasswordCredentialsFromJSONString("{" +
                "   \"passwordCredentials\": {" +
                "       \"password\": \"secretPassword\"" +
                "   }" +
                "}");
        assertThat("username", passwordCredentialsFromJSONString.getUsername(), nullValue());
    }

    @Test
    public void getPasswordCredentialsFromJSONString_withNoPassword_returnNullPassword() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getPasswordCredentialsFromJSONString("{" +
                "   \"passwordCredentials\": {" +
                "       \"username\": \"jsmith\"," +
                "   }" +
                "}");
        assertThat("password", passwordCredentialsFromJSONString.getPassword(), equalTo(null));
    }

    @Test
    public void getPasswordCredentialsFromJSONString_withPasswordCredentials_returnsNewPasswordCredentials() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getPasswordCredentialsFromJSONString("{ }");
        assertThat("passwordCredentials password", passwordCredentialsFromJSONString.getPassword(), nullValue());
        assertThat("passwordCredentials username", passwordCredentialsFromJSONString.getUsername(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void checkAndGetPasswordCredentialsFromJSONString_withNoPassword_throwsBadRequestException() throws Exception {
        JSONReaderForPasswordCredentials.checkAndGetPasswordCredentialsFromJSONString("{" +
                "   \"passwordCredentials\": {" +
                "       \"username\": \"jsmith\"," +
                "   }" +
                "}");
    }

    @Test(expected = BadRequestException.class)
    public void checkAndGetPasswordCredentialsFromJSONString_withNoUsername_throwsBadRequestException() throws Exception {
        JSONReaderForPasswordCredentials.checkAndGetPasswordCredentialsFromJSONString("{" +
                "   \"passwordCredentials\": {" +
                "       \"password\": \"secretPassword\"," +
                "   }" +
                "}");
    }

    @Test(expected = BadRequestException.class)
    public void getPasswordCredentialsFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForPasswordCredentials.getPasswordCredentialsFromJSONString("invalid JSON");
    }




}
