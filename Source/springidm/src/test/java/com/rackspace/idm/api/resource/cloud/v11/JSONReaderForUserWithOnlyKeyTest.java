package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey;
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
 * Time: 11:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserWithOnlyKeyTest {

    String userOnlyKeyJSON = "{" +
            "   \"user\" : {" +
            "       \"id\" : \"userId\"," +
            "       \"key\" : \"apiKey\"" +
            "   }" +
            "}";

    String emptyUserOnlyKeyJSON = "{" +
            "   \"user\" : {" +
            "   }" +
            "}";

    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForUserWithOnlyKey jsonReaderForUserWithOnlyKey = new JSONReaderForUserWithOnlyKey();
        boolean readable = jsonReaderForUserWithOnlyKey.isReadable(UserWithOnlyKey.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForUser jsonReaderForUserWithOnlyKey = new JSONReaderForUser();
        boolean readable = jsonReaderForUserWithOnlyKey.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidStream_returnsUserObject() throws Exception {
        JSONReaderForUserWithOnlyKey jsonReaderForUserWithOnlyKey = new JSONReaderForUserWithOnlyKey();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(userOnlyKeyJSON.getBytes()));
        UserWithOnlyKey userWithOnlyKey = jsonReaderForUserWithOnlyKey.readFrom(UserWithOnlyKey.class, null, null, null, null, inputStream);
        assertThat("user" , userWithOnlyKey, is(UserWithOnlyKey.class));
    }

    @Test
    public void getUserWithOnlyKey_withValidJSON_returnsUserWithEnabled() throws Exception {
        UserWithOnlyKey user = JSONReaderForUserWithOnlyKey.getUserWithOnlyKeyFromJSONString(userOnlyKeyJSON);
        assertThat("user enabled", user.getKey(), equalTo("apiKey"));
    }

    @Test
    public void getUserWithOnlyKey_withEmptyValidJSON_returnsUserWithEnabledTrue() throws Exception {
        UserWithOnlyKey user = JSONReaderForUserWithOnlyKey.getUserWithOnlyKeyFromJSONString(emptyUserOnlyKeyJSON);
        assertThat("user enabled", user.getKey(), nullValue());
    }

    @Test
    public void getUserWithOnlyKey_withValidJSON_returnsUserWithId() throws Exception {  //Should this be here?
        UserWithOnlyKey user = JSONReaderForUserWithOnlyKey.getUserWithOnlyKeyFromJSONString(userOnlyKeyJSON);
        assertThat("user id", user.getId(), equalTo("userId"));
    }

    @Test
    public void getUserWithOnlyKey_withEmptyValidJSON_returnsUserWithNullId() throws Exception {
        UserWithOnlyKey user = JSONReaderForUserWithOnlyKey.getUserWithOnlyKeyFromJSONString(emptyUserOnlyKeyJSON);
        assertThat("user id", user.getId(), nullValue());
    }

    @Test
    public void getUserWithOnlyKey_withEmptyJSON_returnsNewUserObject() throws Exception {
        UserWithOnlyKey user = JSONReaderForUserWithOnlyKey.getUserWithOnlyKeyFromJSONString("{ }");
        assertThat("user id", user.getId(), nullValue());
        assertThat("user enabled", user.getKey(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getUserWithOnlyKey_WithInvalidJSON_ThrowsBadRequestException() throws Exception {
        JSONReaderForUserWithOnlyKey.getUserWithOnlyKeyFromJSONString("Invalid JSON");
    }
}
