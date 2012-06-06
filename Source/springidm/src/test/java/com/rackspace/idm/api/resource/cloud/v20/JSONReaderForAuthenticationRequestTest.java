package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/5/12
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForAuthenticationRequestTest {

    JSONReaderForAuthenticationRequest jsonReaderForAuthenticationRequest;
    String authRequestWithApiKey = "{\n" +
            "    \"auth\": \n" +
            "    {\n" +
            "        \"RAX-KSKEY:apiKeyCredentials\": {\n" +
            "    \"username\": \"jsmith\",\n" +
            "    \"apiKey\": \"aaaaa-bbbbb-ccccc-12345678\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
    String authRequestWithPassword = "{\n" +
            "    \"auth\":{\n" +
            "        \"passwordCredentials\":{\n" +
            "            \"username\":\"jsmith\",\n" +
            "            \"password\":\"theUsersPassword\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
    String authRequestWithToken = "{\n" +
            "    \"auth\": {\n" +
            "        \"token\": {\n" +
            "            \"id\": \"vvvvvvvv-wwww-xxxx-yyyy-zzzzzzzzzzzz\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
    String authRequestWithTenantNameAndId = "{\n" +
            "    \"auth\": {\n" +
            "        \"tenantName\": \"jsmith\",\n" +
            "        \"tenantId\": \"11110000111\"\n" +
            "    }\n" +
            "}";


    @Before
    public void setUp() throws Exception {
        jsonReaderForAuthenticationRequest = new JSONReaderForAuthenticationRequest();
    }

    @Test
    public void isReadable_withAuthenticationRequestClass_returnsTrue() throws Exception {
        boolean readable = jsonReaderForAuthenticationRequest.isReadable(AuthenticationRequest.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void readFrom_withValidJSON_returnsAuthenticationRequest() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(authRequestWithApiKey.getBytes()));
        AuthenticationRequest authenticationRequest = jsonReaderForAuthenticationRequest.readFrom(AuthenticationRequest.class, null, null, null, null, inputStream);
        assertThat("authentication request credentials", authenticationRequest, not(nullValue()));
    }

    @Test(expected = BadRequestException.class)
    public void readFrom_withInvalidJSON_throwsBadRequestException() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream("invalid JSON string".getBytes()));
        jsonReaderForAuthenticationRequest.readFrom(AuthenticationRequest.class, null, null, null, null, inputStream);
    }

    @Test
    public void getAuthenticationRequestFromJSONString_withValidApiKeyAuthRequest_returnsAuthenticationRequestWithApiKeyCredentials() throws Exception {
        AuthenticationRequest authenticationRequestFromJSONString = JSONReaderForAuthenticationRequest.getAuthenticationRequestFromJSONString(authRequestWithApiKey);
        assertThat("authentication credentials", authenticationRequestFromJSONString.getCredential().getValue(), instanceOf(ApiKeyCredentials.class));
    }

    @Test
    public void getAuthenticationRequestFromJSONString_withValidPasswordAuthRequest_returnsAuthenticationRequestWithPasswordCredentialsRequiredUsername() throws Exception {
        AuthenticationRequest authenticationRequestFromJSONString = JSONReaderForAuthenticationRequest.getAuthenticationRequestFromJSONString(authRequestWithPassword);
        assertThat("authentication credential", authenticationRequestFromJSONString.getCredential().getValue(), instanceOf(PasswordCredentialsRequiredUsername.class));
    }

    @Test
    public void getAuthenticationRequestFromJSONString_withValidTokenRequest_returnsAuthenticationRequestWithToken() throws Exception {
        AuthenticationRequest authenticationRequestFromJSONString = JSONReaderForAuthenticationRequest.getAuthenticationRequestFromJSONString(authRequestWithToken);
        assertThat("authentication credentials token Id", authenticationRequestFromJSONString.getToken().getId(), equalTo("vvvvvvvv-wwww-xxxx-yyyy-zzzzzzzzzzzz"));
    }

    @Test
    public void getAuthenticationRequestFromJSONString_withTenantName_returnsAuthenticationRequestWithTenantName() throws Exception {
        AuthenticationRequest authenticationRequestFromJSONString = JSONReaderForAuthenticationRequest.getAuthenticationRequestFromJSONString(authRequestWithTenantNameAndId);
        assertThat("authentication credentials tenantName", authenticationRequestFromJSONString.getTenantName(), equalTo("jsmith"));
    }

    @Test
    public void getAuthenticationRequestFromJSONString_withTenantId_returnsAuthenticationRequestWithTenantId() throws Exception {
        AuthenticationRequest authenticationRequestFromJSONString = JSONReaderForAuthenticationRequest.getAuthenticationRequestFromJSONString(authRequestWithTenantNameAndId);
        assertThat("authentication credentials tenantId", authenticationRequestFromJSONString.getTenantId(), equalTo("11110000111"));
    }

    @Test(expected = BadRequestException.class)
    public void getAuthenticationRequestFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForAuthenticationRequest.getAuthenticationRequestFromJSONString("invalid JSON string");
    }

}
