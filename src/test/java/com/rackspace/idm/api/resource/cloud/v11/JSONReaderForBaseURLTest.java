package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.UserType;
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
 * Time: 12:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForBaseURLTest {
    JSONReaderForBaseURL jsonReaderForBaseURL;

    @Before
    public void setUp() throws Exception {
        jsonReaderForBaseURL = new JSONReaderForBaseURL();
    }

    @Test
    public void isReadable_typeIsBaseURL_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForBaseURL.isReadable(BaseURL.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotBaseURL_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForBaseURL.isReadable(Tenant.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsBaseURL() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("baseURL",jsonReaderForBaseURL.readFrom(BaseURL.class, null, null, null, null, inputStream),instanceOf(BaseURL.class));
    }

    @Test
    public void getBaseURLFromJSONString_validJsonBody_returnsBaseURLCorrectId() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getId(), equalTo(123456));
    }

    @Test
    public void getBaseURLFromJSONString_validJsonBody_returnsBaseURLCorrectAdminURL() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getAdminURL(), equalTo("www.adminURL.com"));
    }

    @Test
    public void getBaseURLFromJSONString_validJsonBody_returnsBaseURLCorrectInternalURL() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getInternalURL(), equalTo("www.internalURL.com"));
    }

    @Test
    public void getBaseURLFromJSONString_validJsonBody_returnsBaseURLCorrectUserType() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getUserType(), equalTo(UserType.CLOUD));
    }

    @Test
    public void getBaseURLFromJSONString_validJsonBody_returnsBaseURLCorrectPublicURL() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getPublicURL(), equalTo("www.publicURL.com"));
    }

    @Test
    public void getBaseURLFromJSONString_validJsonBody_returnsBaseURLCorrectServiceName() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getServiceName(), equalTo("service"));
    }

    @Test
    public void getBaseURLFromJSONString_validJsonBody_returnsBaseURLCorrectRegion() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getRegion(), equalTo("US"));
    }

    @Test
    public void getBaseURLFromJSONString_validJsonBody_returnsBaseURLIsDefault() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).isDefault(), equalTo(true));
    }

    @Test
    public void getBaseURLFromJSONString_validJsonBody_returnsBaseURLNotEnabled() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).isEnabled(), equalTo(false));
    }

    @Test
    public void getBaseURLFromJSONString_passwordCredentialInput_returnsEmptyBaseURL() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getId(), equalTo(0));
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getAdminURL(), nullValue());
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getInternalURL(), nullValue());
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getUserType(), nullValue());
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getPublicURL(), nullValue());
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getServiceName(), nullValue());
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getRegion(), nullValue());
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).isDefault(), equalTo(false));
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).isEnabled(), equalTo(true));

    }

    @Test(expected = BadRequestException.class)
    public void getBaseURLFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForBaseURL.getBaseURLFromJSONString(body);
    }

    @Test
    public void getBaseURLFromJSONString_nullID_returnsBaseURLNullID() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getId(), equalTo(0));
    }

    @Test
    public void getBaseURLFromJSONString_nullAdminURL_returnsBaseURLNullAdminURL() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getAdminURL(), nullValue());
    }

    @Test
    public void getBaseURLFromJSONString_nullInternalURL_returnsBaseURLNullInternalURL() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getInternalURL(), nullValue());
    }

    @Test
    public void getBaseURLFromJSONString_nullUserType_returnsBaseURLNullUserType() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getUserType(), nullValue());
    }

    @Test
    public void getBaseURLFromJSONString_nullPublicURL_returnsBaseURLNullPublicURL() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getPublicURL(), nullValue());
    }

    @Test
    public void getBaseURLFromJSONString_nullServiceName_returnsBaseURLNullServiceName() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getServiceName(), nullValue());
    }

    @Test
    public void getBaseURLFromJSONString_nullRegion_returnsBaseURLNullRegion() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"default\": true,\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).getRegion(), nullValue());
    }

    @Test
    public void getBaseURLFromJSONString_DefaultFalse_returnsBaseURLDefaultFalse() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"enabled\": false,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).isDefault(), equalTo(false));
    }

    @Test
    public void getBaseURLFromJSONString_EnabledTrue_returnsBaseURLEnabledTrue() throws Exception {
        String body = "{\n" +
                "  \"OS-KSCATALOG:endpointTemplate\": {\n" +
                "    \"id\": 123456,\n" +
                "    \"adminURL\": \"www.adminURL.com\",\n" +
                "    \"internalURL\": \"www.internalURL.com\",\n" +
                "    \"userType\": \"CLOUD\",\n" +
                "    \"publicURL\": \"www.publicURL.com\"\n" +
                "    \"serviceName\": \"service\",\n" +
                "    \"region\": \"US\",\n" +
                "    \"default\": true,\n" +
                "  }\n" +
                "}";
        assertThat("baseURL", JSONReaderForBaseURL.getBaseURLFromJSONString(body).isEnabled(), equalTo(true));
    }
}
