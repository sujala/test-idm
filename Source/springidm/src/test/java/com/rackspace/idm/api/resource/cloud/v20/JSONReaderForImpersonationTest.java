package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationRequest;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 6/4/12
 * Time: 3:14 PM
 */
public class JSONReaderForImpersonationTest {

    @Test(expected = BadRequestException.class)
    public void getImpersonationFromJSONString_withEmptyString_throwsBadRequestException() throws Exception {
        JSONReaderForImpersonation.getImpersonationFromJSONString("");
    }

    @Test
    public void getImpersonationFromJSONString_withValidRequest_returnsImpersonationRequestObject() throws Exception {
        ImpersonationRequest impersonationRequest = JSONReaderForImpersonation.getImpersonationFromJSONString("{\n" +
                "  \"RAX-GA:impersonation\" : {\n" +
                "      \"user\": {\n" +
                "          \"username\": \"john.smith\"\n" +
                "      }\n" +
                "  }\n" +
                "}");
        assertThat("impersonation request", impersonationRequest, is(ImpersonationRequest.class));
        assertThat("user", impersonationRequest.getUser().getUsername(), equalTo("john.smith"));
    }

    @Test
    public void getImpersonationFromJSONString_withExpireInElement_returnsImpersonationRequestObjectWithExpireInSet() throws Exception {
        ImpersonationRequest impersonationRequest = JSONReaderForImpersonation
                .getImpersonationFromJSONString("{\"RAX-GA:impersonation\":{\"user\":{\"username\":\"john.smith\"},\"expire-in-seconds\":\"5000\"}}");
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
}
