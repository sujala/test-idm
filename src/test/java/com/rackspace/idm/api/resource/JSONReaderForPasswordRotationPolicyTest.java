package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.PasswordRotationPolicy;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/7/12
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForPasswordRotationPolicyTest {

    private String passwordRotationPolicyJSON = "{" +
            "   \"passwordRotationPolicy\" : {" +
            "       \"duration\" : \"100\"," +
            "       \"enabled\" : \"true\"" +
            "   }" +
            "}";

    private String emptyPasswordRotationPolicyJSON = "{" +
            "   \"passwordRotationPolicy\" : {" +
            "   }" +
            "}";

    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForPasswordRotationPolicy jsonReaderForPasswordRotationPolicy = new JSONReaderForPasswordRotationPolicy();
        boolean readable = jsonReaderForPasswordRotationPolicy.isReadable(PasswordRotationPolicy.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForPasswordRotationPolicy jsonReaderForPasswordRotationPolicy = new JSONReaderForPasswordRotationPolicy();
        boolean readable = jsonReaderForPasswordRotationPolicy.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnPasswordRotationPolicy() throws Exception {
        JSONReaderForPasswordRotationPolicy jsonReaderForPasswordRotationPolicy = new JSONReaderForPasswordRotationPolicy();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(passwordRotationPolicyJSON.getBytes()));
        PasswordRotationPolicy passwordRotationPolicy = jsonReaderForPasswordRotationPolicy.readFrom(PasswordRotationPolicy.class, null, null, null, null, inputStream);
        assertThat("password rotation policy", passwordRotationPolicy, is(PasswordRotationPolicy.class));
    }

    @Test
    public void getPasswordRotationPolicyFromJSONString_withValidJSON_setDuration() throws Exception {
        PasswordRotationPolicy passwordRotationPolicyFromJSONString = JSONReaderForPasswordRotationPolicy.getPasswordRotationPolicyFromJSONString(passwordRotationPolicyJSON);
        assertThat("password rotation policy duration", passwordRotationPolicyFromJSONString.getDuration(), equalTo(100));
    }

    @Test
    public void getPasswordRotationPolicyFromJSONString_withValidJSON_setEnabled() throws Exception {
        PasswordRotationPolicy passwordRotationPolicyFromJSONString = JSONReaderForPasswordRotationPolicy.getPasswordRotationPolicyFromJSONString(passwordRotationPolicyJSON);
        assertThat("password rotation policy enabled", passwordRotationPolicyFromJSONString.isEnabled(), equalTo(true));
    }

    @Test
    public void getPasswordRotationPolicyFromJSONString_withValidEmptyPasswordRotationPolicyJSON_setZeroDuration() throws Exception {
        PasswordRotationPolicy passwordRotationPolicyFromJSONString = JSONReaderForPasswordRotationPolicy.getPasswordRotationPolicyFromJSONString(emptyPasswordRotationPolicyJSON);
        assertThat("password rotation policy duration", passwordRotationPolicyFromJSONString.getDuration(), equalTo(0));
    }

    @Test
    public void getPasswordRotationPolicyFromJSONString_withValidEmptyPasswordRotationPolicyJSON_setFalseEnabled() throws Exception {
        PasswordRotationPolicy passwordRotationPolicyFromJSONString = JSONReaderForPasswordRotationPolicy.getPasswordRotationPolicyFromJSONString(emptyPasswordRotationPolicyJSON);
        assertThat("password rotation policy enabled", passwordRotationPolicyFromJSONString.isEnabled(), equalTo(false));
    }

    @Test
    public void getPasswordRotationPolicyFromJSONString_withEmptyJSON_returnNewPasswordRotationPolicyObject() throws Exception {
        PasswordRotationPolicy passwordRotationPolicyFromJSONString = JSONReaderForPasswordRotationPolicy.getPasswordRotationPolicyFromJSONString("{ }");
        assertThat("password rotation policy", passwordRotationPolicyFromJSONString, is(PasswordRotationPolicy.class));
        assertThat("password rotation policy enabled", passwordRotationPolicyFromJSONString.isEnabled(), equalTo(false));
        assertThat("password rotation policy duration", passwordRotationPolicyFromJSONString.getDuration(), equalTo(0));
    }

    @Test(expected = BadRequestException.class)
    public void getPasswordRotationPolicyFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForPasswordRotationPolicy.getPasswordRotationPolicyFromJSONString("Invalid JSON");
    }
}
