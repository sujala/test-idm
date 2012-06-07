package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationRequest;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 6/4/12
 * Time: 3:14 PM
 */
public class JSONReaderForImpersonationTest {

    String impersonationRequestJSON = "{\n" +
            "  \"RAX-GA:impersonation\" : {\n" +
            "      \"user\": {\n" +
            "          \"username\": \"john.smith\"\n" +
            "      },\n" +
            "      \"expire-in-seconds\": \"5000\"\n" +
            "  }\n" +
            "}";

    String emptyImpersonationRequestJSON = "{\n" +
            "  \"RAX-GA:impersonation\" : {\n" +
            "  }\n" +
            "}";
    @Test
    public void isReadable_withValidType_returnsTrue() throws Exception {
        JSONReaderForImpersonation jsonReaderForImpersonation = new JSONReaderForImpersonation();
        boolean readable = jsonReaderForImpersonation.isReadable(ImpersonationRequest.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidType_returnsFalse() throws Exception {
        JSONReaderForImpersonation jsonReaderForImpersonation = new JSONReaderForImpersonation();
        boolean readable = jsonReaderForImpersonation.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidRequest_returnsImpersonationRequest() throws Exception {
        JSONReaderForImpersonation jsonReaderForImpersonation = new JSONReaderForImpersonation();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(impersonationRequestJSON.getBytes()));
        ImpersonationRequest impersonationRequest = jsonReaderForImpersonation.readFrom(ImpersonationRequest.class, null, null, null, null, inputStream);
        assertThat("impersonation Request", impersonationRequest, is(ImpersonationRequest.class));
        assertThat("impersonation Request username", impersonationRequest.getUser().getUsername(), equalTo("john.smith"));
    }

    @Test(expected = BadRequestException.class)
    public void readFrom_withInvalidRequest_throwsBadRequestException() throws Exception {
        JSONReaderForImpersonation jsonReaderForImpersonation = new JSONReaderForImpersonation();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream("Invalid JSON".getBytes()));
        jsonReaderForImpersonation.readFrom(ImpersonationRequest.class, null, null, null, null, inputStream);
    }

    @Test(expected = BadRequestException.class)
    public void getImpersonationFromJSONString_withEmptyString_throwsBadRequestException() throws Exception {
        JSONReaderForImpersonation.getImpersonationFromJSONString("");
    }

    @Test
    public void getImpersonationFromJSONString_withValidRequest_returnsImpersonationRequestObject() throws Exception {
        ImpersonationRequest impersonationRequest = JSONReaderForImpersonation.getImpersonationFromJSONString(impersonationRequestJSON);
        assertThat("impersonation request", impersonationRequest, is(ImpersonationRequest.class));
        assertThat("username", impersonationRequest.getUser().getUsername(), equalTo("john.smith"));
    }

    @Test(expected = BadRequestException.class) //TODO: should this return null user like similar calls e.g.: JSONReaderForPasswordCredentials?(sticking to the idea of these being readers and not validators)
    public void getImpersonationFromJSONString_withValidJSONAndNoUsername_throwsBadRequestException() throws Exception {
        JSONReaderForImpersonation.getImpersonationFromJSONString(emptyImpersonationRequestJSON);
    }

    @Test
    public void getImpersonationFromJSONString_withEmptyJSON_returnsNewImpersonationRequest() throws Exception {
        ImpersonationRequest impersonationRequest = JSONReaderForImpersonation.getImpersonationFromJSONString("{ }");
        assertThat("user", impersonationRequest.getUser(), nullValue());
    }

    @Test
    public void getImpersonationFromJSONString_withExpireInElement_returnsImpersonationRequestObjectWithExpireInSet() throws Exception {
        ImpersonationRequest impersonationRequest = JSONReaderForImpersonation
                .getImpersonationFromJSONString(impersonationRequestJSON);
        assertThat("impersonation request", impersonationRequest, is(ImpersonationRequest.class));
        assertThat("user", impersonationRequest.getExpireInSeconds(), equalTo(5000));
    }

    @Test(expected = BadRequestException.class)
    public void getImpersonationFromJSONString_withExpireInElementIsEmptyString_throwsBadRequestException() throws Exception {
        JSONReaderForImpersonation.getImpersonationFromJSONString("{\"RAX-GA:impersonation\":{\"user\":{\"username\":\"john.smith\"},\"expire-in-seconds\":\"\"}}");
    }

    @Test(expected = BadRequestException.class)
    public void getImpersonationFromJSONString_withExpireInElementContainsNonIntValue_throwsBadRequestException() throws Exception {
        JSONReaderForImpersonation.getImpersonationFromJSONString("{\"RAX-GA:impersonation\":{\"user\":{\"username\":\"john.smith\"},\"expire-in-seconds\":\"abc\"}}");
    }

    @Test
    public void getImpersonationFromJSONString_withExpireInElementContainsDecimalValue_throwsBadRequestException() throws Exception {
        try {
            JSONReaderForImpersonation.getImpersonationFromJSONString("{\"RAX-GA:impersonation\":{\"user\":{\"username\":\"john.smith\"},\"expire-in-seconds\":\".1\"}}");
            assertTrue("expected and exception to be thrown", false);
        } catch (Exception e) {
            assertThat("expected message", e.getMessage(), equalTo("Expire-in element should be an integer."));
        }
    }
}
