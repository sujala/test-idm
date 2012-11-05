package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RsaCredentials;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 11/5/12
 * Time: 7:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForRSACredentialsTest {

    JSONReaderForRSACredentials jsonReaderForRSA;
    String jsonBody = "{\n" +
            "        \"RAX-AUTH:rsaCredentials\": {\n" +
            "    \"username\": \"jsmith\",\n" +
            "    \"tokenKey\": \"12345678\"\n" +
            "    }\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        jsonReaderForRSA = new JSONReaderForRSACredentials();
    }

    @Test
    public void isReadable_withRsaCredentialsClass_returnsTrue() throws Exception {
        boolean readable = jsonReaderForRSA.isReadable(RsaCredentials.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withNotRsaCredentialsClass_returnsFalse() throws Exception {
        boolean readable = jsonReaderForRSA.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnsRsaCredentials() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(jsonBody.getBytes()));
        RsaCredentials authenticationRequest = jsonReaderForRSA.readFrom(RsaCredentials.class, null, null, null, null, inputStream);
        assertThat("authentication request credentials", authenticationRequest, not(nullValue()));
    }

    @Test(expected = BadRequestException.class)
    public void readFrom_withInvalidJSON_throwsBadRequestException() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream("invalid JSON string".getBytes()));
        jsonReaderForRSA.readFrom(RsaCredentials.class, null, null, null, null, inputStream);
    }

    @Test
    public void getRSARequestFromJSONString_withNoRSARequestKey_returnsEmptyRSARequest() throws Exception {
        RsaCredentials rsaRequestFromJSONString = JSONReaderForRSACredentials.getRSACredentialsFromJSONString("{ }");
        assertThat("authentication request", rsaRequestFromJSONString.getUsername(), nullValue());
    }

    @Test
    public void getRSARequestFromJSONString_withValidRSARequest_returnsRSACredentials() throws Exception {
        RsaCredentials rsaRequestFromJSONString = JSONReaderForRSACredentials.getRSACredentialsFromJSONString(jsonBody);
        assertThat("authentication credentials", rsaRequestFromJSONString, instanceOf(RsaCredentials.class));
    }

    @Test
    public void getRSARequestFromJSONString_withValidTokenRequest_returnsRSARequestWithToken() throws Exception {
        RsaCredentials rsaRequestFromJSONString = JSONReaderForRSACredentials.getRSACredentialsFromJSONString(jsonBody);
        assertThat("authentication credentials token Id", rsaRequestFromJSONString.getTokenKey(), equalTo("12345678"));
    }

    @Test
    public void getAuthenticationRequestFromJSONString_withNoTokenIdRequest_returnNullTokenId() throws Exception {
        RsaCredentials rsaRequestFromJSONString = JSONReaderForRSACredentials.getRSACredentialsFromJSONString("{\n" +
            "        \"RAX-AUTH:rsaCredentials\": {}\n" +
            "}");
        assertThat("authenticationRquest tokenId", rsaRequestFromJSONString.getTokenKey(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getRSARequestFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForRSACredentials.getRSACredentialsFromJSONString("invalid JSON string");
    }

}
