package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.IdentityProfile;
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
 * Time: 2:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForIdentityProfileTest {
    String identityProfileJSON = "{" +
            "   \"customerIdentityProfile\" : {" +
            "       \"id\" : \"customerIdentityProfileId\"," +
            "       \"customerId\" : \"customerId\"," +
            "       \"enabled\" : false" +
            "   }" +
            "}";
    private String emptyIdentityProfileJSON = "{" +
            "   \"customerIdentityProfile\" : {" +
            "   }" +
            "}";

    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForIdentityProfile jsonReaderForIdentityProfile = new JSONReaderForIdentityProfile();
        boolean readable = jsonReaderForIdentityProfile.isReadable(IdentityProfile.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForIdentityProfile jsonReaderForIdentityProfile = new JSONReaderForIdentityProfile();
        boolean readable = jsonReaderForIdentityProfile.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnsIdentityProfileObject() throws Exception {
        JSONReaderForIdentityProfile jsonReaderForIdentityProfile = new JSONReaderForIdentityProfile();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(identityProfileJSON.getBytes()));
        IdentityProfile identityProfile = jsonReaderForIdentityProfile.readFrom(IdentityProfile.class, null, null, null, null, inputStream);
        assertThat("identity profile", identityProfile, is(IdentityProfile.class));
    }

    @Test
    public void getIdentityProfileFromJSONString_withValidJSON_setsIdentityProfileId() throws Exception {
        IdentityProfile identityProfileFromJSONString = JSONReaderForIdentityProfile.getIdentityProfileFromJSONString(identityProfileJSON);
        assertThat("identity profile id", identityProfileFromJSONString.getId(), equalTo("customerIdentityProfileId"));
    }

    @Test
    public void getIdentityProfileFromJSONString_withValidJSON_setsIdentityProfileCustomerId() throws Exception {
        IdentityProfile identityProfileFromJSONString = JSONReaderForIdentityProfile.getIdentityProfileFromJSONString(identityProfileJSON);
        assertThat("identity profile customer id", identityProfileFromJSONString.getCustomerId(), equalTo("customerId"));
    }

    @Test
    public void getIdentityProfileFromJSONString_withValidJSON_setsIdentityProfileEnabled() throws Exception {
        IdentityProfile identityProfileFromJSONString = JSONReaderForIdentityProfile.getIdentityProfileFromJSONString(identityProfileJSON);
        assertThat("idenity profile enabled", identityProfileFromJSONString.isEnabled(), equalTo(false));
    }

    @Test
    public void getIdentityProfileFromJSONString_withValidJSONAndEmptyIdentityProfile_setsNullIdentityProfileId() throws Exception {
        IdentityProfile identityProfileFromJSONString = JSONReaderForIdentityProfile.getIdentityProfileFromJSONString(emptyIdentityProfileJSON);
        assertThat("identity profile id", identityProfileFromJSONString.getId(), nullValue());
    }

    @Test
    public void getIdentityProfileFromJSONString_withValidJSONAndEmptyIdentityProfile_setsNullIdentityProfileCustomerId() throws Exception {
        IdentityProfile identityProfileFromJSONString = JSONReaderForIdentityProfile.getIdentityProfileFromJSONString(emptyIdentityProfileJSON);
        assertThat("identity profile customer id", identityProfileFromJSONString.getCustomerId(), nullValue());
    }

    @Test
    public void getIdentityProfileFromJSONString_withValidJSONAndEmptyIdentityProfile_setsNullIdentityProfileEnabled() throws Exception {
        IdentityProfile identityProfileFromJSONString = JSONReaderForIdentityProfile.getIdentityProfileFromJSONString(emptyIdentityProfileJSON);
        assertThat("idenity profile enabled", identityProfileFromJSONString.isEnabled(), nullValue());
    }

    @Test
    public void getIdentityProfileFromJSONString_WithEmptyJSON_returnsNewIdentityProfileObject() throws Exception {
        IdentityProfile identityProfileFromJSONString = JSONReaderForIdentityProfile.getIdentityProfileFromJSONString("{ }");
        assertThat("identity profile", identityProfileFromJSONString, is(IdentityProfile.class));
        assertThat("identity profile id", identityProfileFromJSONString.getId(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getIdentityProfileFromJSONString_WithInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForIdentityProfile.getIdentityProfileFromJSONString("Invalid JSON");
    }
}
