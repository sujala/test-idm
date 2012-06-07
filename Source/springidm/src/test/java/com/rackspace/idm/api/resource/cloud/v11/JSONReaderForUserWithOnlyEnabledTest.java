package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/7/12
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserWithOnlyEnabledTest {
    String userOnlyEnabledJSON = "{" +
            "   \"user\" : {" +
            "       \"id\" : \"userId\"," +
            "       \"enabled\" : false" +
            "   }" +
            "}";

    String emptyUserOnlyEnabledJSON = "{" +
            "   \"user\" : {" +
            "   }" +
            "}";

    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForUserWithOnlyEnabled jsonReaderForUserWithOnlyEnabled = new JSONReaderForUserWithOnlyEnabled();
        boolean readable = jsonReaderForUserWithOnlyEnabled.isReadable(UserWithOnlyEnabled.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForUser jsonReaderForUserWithOnlyEnabled = new JSONReaderForUser();
        boolean readable = jsonReaderForUserWithOnlyEnabled.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidStream_returnsUserObject() throws Exception {
        JSONReaderForUserWithOnlyEnabled jsonReaderForUserWithOnlyEnabled = new JSONReaderForUserWithOnlyEnabled();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(userOnlyEnabledJSON.getBytes()));
        jsonReaderForUserWithOnlyEnabled.readFrom(UserWithOnlyEnabled.class, null, null, null, null, inputStream);
    }

    @Test
    public void getUserWithOnlyEnabled_withValidJSON_returnsUserWithEnabled() throws Exception {
        UserWithOnlyEnabled user = JSONReaderForUserWithOnlyEnabled.getUserWithOnlyEnabledFromJSONString(userOnlyEnabledJSON);
        assertThat("user enabled", user.isEnabled(), equalTo(false));
    }

    @Test
    public void getUserWithOnlyEnabled_withEmptyValidJSON_returnsUserWithEnabledTrue() throws Exception {
        UserWithOnlyEnabled user = JSONReaderForUserWithOnlyEnabled.getUserWithOnlyEnabledFromJSONString(emptyUserOnlyEnabledJSON);
        assertThat("user enabled", user.isEnabled(), equalTo(true));
    }

    @Test
    public void getUserWithOnlyEnabled_withValidJSON_returnsUserWithId() throws Exception {  //Should this be here?
        UserWithOnlyEnabled user = JSONReaderForUserWithOnlyEnabled.getUserWithOnlyEnabledFromJSONString(userOnlyEnabledJSON);
        assertThat("user id", user.getId(), equalTo("userId"));
    }

    @Test
    public void getUserWithOnlyEnabled_withEmptyValidJSON_returnsUserWithNullId() throws Exception {
        UserWithOnlyEnabled user = JSONReaderForUserWithOnlyEnabled.getUserWithOnlyEnabledFromJSONString(emptyUserOnlyEnabledJSON);
        assertThat("user id", user.getId(), nullValue());
    }

    @Test
    public void getUserWithOnlyEnabled_withEmptyJSON_returnsNewUserObject() throws Exception {
        UserWithOnlyEnabled user = JSONReaderForUserWithOnlyEnabled.getUserWithOnlyEnabledFromJSONString("{ }");
        assertThat("user id", user.getId(), nullValue());
        assertThat("user enabled", user.isEnabled(), equalTo(true));
    }

    @Test(expected = BadRequestException.class)
    public void getUserWithOnlyEnabled_WithInvalidJSON_ThrowsBadRequestException() throws Exception {
        JSONReaderForUserWithOnlyEnabled.getUserWithOnlyEnabledFromJSONString("Invalid JSON");
    }
}
