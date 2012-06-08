package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.UserPasswordCredentials;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/7/12
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForPasswordCredentialsTest {

    private String passwordCredentialsJSON ="{" +
            "   \"passwordCredentials\" : {" +
            "       \"newPassword\" : {" +
            "           \"password\" : \"newPassword\"" +
            "       }," +
            "       \"currentPassword\" : {" +
            "           \"password\" : \"currentPassword\"" +
            "       }," +
            "       \"verifyCurrentPassword\" : \"false\"" +
            "   }" +
            "}";


    private String passwordCredentialsWeirdVerifyJSON ="{" +
            "   \"passwordCredentials\" : {" +
            "       \"newPassword\" : {" +
            "           \"password\" : \"newPassword\"" +
            "       }," +
            "       \"currentPassword\" : {" +
            "           \"password\" : \"currentPassword\"" +
            "       }," +
            "       \"verifyCurrentPassword\" : \"AnythingElse\"" +
            "   }" +
            "}";


    private String emptyPasswordCredentialsJSON ="{" +
            "   \"passwordCredentials\" : {" +
            "   }" +
            "}";

    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForPasswordCredentials jsonReaderForPasswordCredentials = new JSONReaderForPasswordCredentials();
        boolean readable = jsonReaderForPasswordCredentials.isReadable(UserPasswordCredentials.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForPasswordCredentials jsonReaderForPasswordCredentials = new JSONReaderForPasswordCredentials();
        boolean readable = jsonReaderForPasswordCredentials.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnsUserPasswordCredentialsObject() throws Exception {
        JSONReaderForPasswordCredentials jsonReaderForPasswordCredentials = new JSONReaderForPasswordCredentials();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(passwordCredentialsJSON.getBytes()));
        UserPasswordCredentials userPasswordCredentials = jsonReaderForPasswordCredentials.readFrom(UserPasswordCredentials.class, null, null, null, null, inputStream);
        assertThat("User Password Credentials", userPasswordCredentials, is(UserPasswordCredentials.class));
    }

    @Test
    public void getUserPasswordCredentialsFromJSONString_withValidJSON_setsNewPassword() throws Exception {
        UserPasswordCredentials userPasswordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getUserPasswordCredentialsFromJSONString(passwordCredentialsJSON);
        assertThat("new password", userPasswordCredentialsFromJSONString.getNewPassword().getPassword(), equalTo("newPassword"));
    }

    @Test
    public void getUserPasswordCredentialsFromJSONString_withValidJSON_setsCurrentPassword() throws Exception {
        UserPasswordCredentials userPasswordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getUserPasswordCredentialsFromJSONString(passwordCredentialsJSON);
        assertThat("current password", userPasswordCredentialsFromJSONString.getCurrentPassword().getPassword(), equalTo("currentPassword"));
    }

    @Test
    public void getUserPasswordCredentialsFromJSONString_withValidJSON_setsVerifyCurrentPassword() throws Exception {
        UserPasswordCredentials userPasswordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getUserPasswordCredentialsFromJSONString(passwordCredentialsJSON);
        assertThat("verify current password", userPasswordCredentialsFromJSONString.isVerifyCurrentPassword(), equalTo(false));
    }

    @Test
    public void getUserPasswordCredentialsFromJSONString_withValidJSON_withVerifyNotTrueString_setsVerifyCurrentPassword() throws Exception {
        UserPasswordCredentials userPasswordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getUserPasswordCredentialsFromJSONString(passwordCredentialsWeirdVerifyJSON);
        assertThat("verify current password", userPasswordCredentialsFromJSONString.isVerifyCurrentPassword(), equalTo(true));
    }

    @Test
    public void getUserPasswordCredentialsFromJSONString_withValidJEmptyPasswordCredentialsSON_setsNullVerifyCurrentPassword() throws Exception {
        UserPasswordCredentials userPasswordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getUserPasswordCredentialsFromJSONString(emptyPasswordCredentialsJSON);
        assertThat("verify current password", userPasswordCredentialsFromJSONString.isVerifyCurrentPassword(), nullValue());
    }

    @Test
    public void getUserPasswordCredentialsFromJSONString_withValidJEmptyPasswordCredentialsSON_setsNullCurrentPassword() throws Exception {
        UserPasswordCredentials userPasswordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getUserPasswordCredentialsFromJSONString(emptyPasswordCredentialsJSON);
        assertThat("current password", userPasswordCredentialsFromJSONString.getCurrentPassword(), nullValue());
    }

    @Test
    public void getUserPasswordCredentialsFromJSONString_withValidJEmptyPasswordCredentialsSON_setsNullNewPassword() throws Exception {
        UserPasswordCredentials userPasswordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getUserPasswordCredentialsFromJSONString(emptyPasswordCredentialsJSON);
        assertThat("new password", userPasswordCredentialsFromJSONString.getNewPassword(), nullValue());
    }

    @Test
    public void getUserPasswordCredentialsFromJSONString_withEmptyJSON_returnsNewUserPasswordCredentialsObject() throws Exception {
        UserPasswordCredentials userPasswordCredentialsFromJSONString = JSONReaderForPasswordCredentials.getUserPasswordCredentialsFromJSONString("{ }");
        assertThat("user password credentials", userPasswordCredentialsFromJSONString, is(UserPasswordCredentials.class));
        assertThat("verify current password", userPasswordCredentialsFromJSONString.isVerifyCurrentPassword(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getUserPasswordCredentialsFromJSONString_WithInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForPasswordCredentials.getUserPasswordCredentialsFromJSONString("Invalid JSON");
    }
}
