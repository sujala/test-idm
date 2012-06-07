package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Tenant;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/7/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForBaseUrlRefTest {
    JSONReaderForBaseUrlRef jsonReaderForBaseUrlRef;

    @Before
    public void setUp() throws Exception {
        jsonReaderForBaseUrlRef = new JSONReaderForBaseUrlRef();
    }

    @Test
    public void isReadable_typeIsBaseURLRef_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForBaseUrlRef.isReadable(BaseURLRef.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotBaseURLRef_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForBaseUrlRef.isReadable(Tenant.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsBaseURLRef() throws Exception {
        String body = "{" +
                "  \"baseURLRef\": {\n" +
                "       \"id\": \"123\",\n" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "       \"v1Default\": true\n" +
                "   }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("baseURLRef",jsonReaderForBaseUrlRef.readFrom(BaseURLRef.class, null, null, null, null, inputStream),instanceOf(BaseURLRef.class));
    }

    @Test
    public void getBaseURLRefFromJSONString_validJsonBody_returnsBaseURLRefCorrectId() throws Exception {
        String body = "{" +
                "  \"baseURLRef\": {\n" +
                "       \"id\": \"123\",\n" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "       \"v1Default\": true\n" +
                "   }\n" +
                "}";
        assertThat("baseURLRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONString(body).getId(), equalTo(123));
    }

    @Test
    public void getBaseURLRefFromJSONString_validJsonBody_returnsBaseURLRefV1DefaultTrue() throws Exception {
        String body = "{" +
                "  \"baseURLRef\": {\n" +
                "       \"id\": \"123\",\n" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "       \"v1Default\": true\n" +
                "   }\n" +
                "}";
        assertThat("baseURLRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONString(body).isV1Default(), equalTo(true));
    }

    @Test
    public void getBaseURLRefFromJSONString_passwordCredentialInput_returnsEmptyBaseURLRef() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertThat("baseURLRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONString(body).getId(), equalTo(0));
        assertThat("baseURlRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONString(body).isV1Default(), equalTo(false));
    }

    @Test(expected = BadRequestException.class)
    public void getBaseURLRefFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForBaseUrlRef.getBaseURLRefFromJSONString(body);
    }

    @Test
    public void getBaseURLRefFromJSONString_nullID_returnsBaseURLRefNullID() throws Exception {
        String body = "{" +
                "  \"baseURLRef\": {\n" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "       \"v1Default\": true\n" +
                "   }\n" +
                "}";
        assertThat("baseUrlRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONString(body).getId(), equalTo(0));
    }

    @Test
    public void getBaseURLRefFromJSONString_nullV1Default_returnsBaseURLRefV1DefaultFalse() throws Exception {
        String body = "{" +
                "  \"baseURLRef\": {\n" +
                "       \"id\": \"123\",\n" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "   }\n" +
                "}";
        assertThat("baseURLRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONString(body).isV1Default(), equalTo(false));
    }

    @Test
    public void getBaseURLRefFromJSONStringWithoutWrapper_validJsonBody_returnsBaseURLRefCorrectId() throws Exception {
        String body = "{" +
                "       \"id\": \"123\",\n" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "       \"v1Default\": true\n" +
                "}";
        assertThat("baseURLRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONStringWithoutWrapper(body).getId(), equalTo(123));
    }

    @Test
    public void getBaseURLRefFromJSONStringWithoutWrapper_validJsonBody_returnsBaseURLRefCorrectHref() throws Exception {
        String body = "{" +
                "       \"id\": \"123\",\n" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "       \"v1Default\": true\n" +
                "}";
        assertThat("baseURLRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONStringWithoutWrapper(body).getHref(), equalTo("../samples/baseURLRefs.json"));
    }

    @Test
    public void getBaseURLRefFromJSONStringWithoutWrapper_validJsonBody_returnsBaseURLRefV1DefaultTrue() throws Exception {
        String body = "{" +
                "       \"id\": \"123\",\n" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "       \"v1Default\": true\n" +
                "}";
        assertThat("baseURLRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONStringWithoutWrapper(body).isV1Default(), equalTo(true));
    }

    @Test(expected = BadRequestException.class)
    public void getBaseURLRefFromJSONStringWithoutWrapper_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForBaseUrlRef.getBaseURLRefFromJSONStringWithoutWrapper(body);
    }

    @Test
    public void getBaseURLRefFromJSONStringWithoutWrapper_nullID_returnsBaseURLRefNullID() throws Exception {
        String body = "{" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "       \"v1Default\": true\n" +
                "}";
        assertThat("baseUrlRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONStringWithoutWrapper(body).getId(), equalTo(0));
    }

    @Test
    public void getBaseURLRefFromJSONStringWithoutWrapper_nullHref_returnsBaseURLRefNullHref() throws Exception {
        String body = "{" +
                "       \"id\": \"123\",\n" +
                "       \"v1Default\": true\n" +
                "}";
        assertThat("baseUrlRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONStringWithoutWrapper(body).getHref(), nullValue());
    }

    @Test
    public void getBaseURLRefFromJSONStringWithoutWrapper_nullV1Default_returnsBaseURLRefV1DefaultFalse() throws Exception {
        String body = "{" +
                "       \"id\": \"123\",\n" +
                "       \"href\": \"../samples/baseURLRefs.json\",\n" +
                "}";
        assertThat("baseURLRef", JSONReaderForBaseUrlRef.getBaseURLRefFromJSONStringWithoutWrapper(body).isV1Default(), equalTo(false));
    }
}
