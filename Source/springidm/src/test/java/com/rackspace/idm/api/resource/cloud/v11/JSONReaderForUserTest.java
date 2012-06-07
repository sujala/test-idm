package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.User;
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
 * Time: 10:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForUserTest {

    String userJSON = "{ "+
            "   \"user\" : {\n" +
            "        \"id\" : \"userId\",\n" +
            "        \"mossoId\" : 123456,\n" +
            "        \"nastId\" : \"nastId\",\n" +
            "        \"key\" : \"apiKey\",\n" +
            "        \"enabled\" : true,\n" +
            "        \"baseURLRefs\" : [" +
            "            {\n" +
            "                \"id\" : 1,\n" +
            "                \"href\" : \"https://auth.api.rackspacecloud.com/v1.1/baseURLs/1\",\n" +
            "                \"v1Default\" : true\n" +
            "            }" +
            "        ]" +
            "   }" +
            "}";

    String emptyUserJSON = "{ "+
            "   \"user\" : {\n" +
            "   }" +
            "}";


    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForUser jsonReaderForUser = new JSONReaderForUser();
        boolean readable = jsonReaderForUser.isReadable(User.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForUser jsonReaderForUser = new JSONReaderForUser();
        boolean readable = jsonReaderForUser.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidStream_returnsUserObject() throws Exception {
        JSONReaderForUser jsonReaderForUser = new JSONReaderForUser();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(userJSON.getBytes()));
        jsonReaderForUser.readFrom(User.class, null, null, null, null, inputStream);
    }

    @Test
    public void getUserFromJSONString_withValidJSON_setsUserId() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(userJSON);
        assertThat("user Id", userFromJSONString.getId(), equalTo("userId"));
    }

    @Test
    public void getUserFromJSONString_withValidJSON_setsUserMossoId() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(userJSON);
        assertThat("user mossoId", userFromJSONString.getMossoId(), equalTo(123456));
    }

    @Test
    public void getUserFromJSONString_withValidJSON_setsUserNastId() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(userJSON);
        assertThat("user nastId", userFromJSONString.getNastId(), equalTo("nastId"));
    }

    @Test
    public void getUserFromJSONString_withValidJSON_setsKey() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(userJSON);
        assertThat("user key", userFromJSONString.getKey(), equalTo("apiKey"));
    }

    @Test
    public void getUserFromJSONString_withValidJSON_setsEnabled() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(userJSON);
        assertThat("user enabled", userFromJSONString.isEnabled(), equalTo(true));
    }

    @Test
    public void getUserFromJSONString_withValidJSON_setsBaseURLRefs() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(userJSON);
        assertThat("user baseURLRefs size", userFromJSONString.getBaseURLRefs().getBaseURLRef().size(), equalTo(1));
    }

    @Test
    public void getUserFromJSONString_withEmptyValidJSON_setsNullUserId() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(emptyUserJSON);
        assertThat("user Id", userFromJSONString.getId(), nullValue());
    }

    @Test
    public void getUserFromJSONString_withEmptyValidJSON_setsZeroUserMossoId() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(emptyUserJSON);
        assertThat("user mossoId", userFromJSONString.getMossoId(), nullValue());
    }

    @Test
    public void getUserFromJSONString_withEmptyValidJSON_setsNullUserNastId() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(emptyUserJSON);
        assertThat("user nastId", userFromJSONString.getNastId(), nullValue());
    }

    @Test
    public void getUserFromJSONString_withEmptyValidJSON_setsNullKey() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(emptyUserJSON);
        assertThat("user key", userFromJSONString.getKey(), nullValue());
    }

    @Test
    public void getUserFromJSONString_withEmptyValidJSON_setsEnabledFalse() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(emptyUserJSON);
        assertThat("user enabled", userFromJSONString.isEnabled(), equalTo(true));
    }

    @Test
    public void getUserFromJSONString_withEmptyValidJSON_setsNullBaseURLRefs() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(emptyUserJSON);
        assertThat("user baseURLRefs size", userFromJSONString.getBaseURLRefs(), nullValue());
    }

    @Test
    public void getUserFromJSONString_withEmptyJSON_returnsNewUser() throws Exception {
        User userFromJSONString = JSONReaderForUser.getUserFromJSONString(emptyUserJSON);
        assertThat("user", userFromJSONString, is(User.class));
        assertThat("user id", userFromJSONString.getId(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getUserFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForUser.getUserFromJSONString("Invalid JSON");
    }
}
