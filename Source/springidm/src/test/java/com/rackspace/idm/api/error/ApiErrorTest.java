package com.rackspace.idm.api.error;

import com.rackspace.idm.GlobalConstants;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/9/12
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApiErrorTest {
    ApiError apiError;

    @Before
    public void setUp() throws Exception {
        apiError = new ApiError();
    }

    @Test
    public void getXmlNamespace_returnsXMLNS() throws Exception {
        String result = apiError.getXmlNamespace();
        assertThat("xml ns", result, equalTo(GlobalConstants.API_NAMESPACE_LOCATION));
    }

    @Test
    public void getCode_returnsCode() throws Exception {
        apiError.setStatusCode(1);
        int result = apiError.getCode();
        assertThat("code", result, equalTo(1));
    }

    @Test
    public void getMessage_returnsMessage() throws Exception {
        apiError.setMessage("message");
        String result = apiError.getMessage();
        assertThat("message", result, equalTo("message"));
    }

    @Test
    public void getDetails_returnsDetails() throws Exception {
        apiError.setDetails("details");
        String result = apiError.getDetails();
        assertThat("details", result, equalTo("details"));
    }

    @Test
    public void toString_returnsApiErrorString() throws Exception {
        String result = apiError.toString();
        assertThat("toString", result, equalTo("ApiError [code=0, message=null, details=null]"));
    }
}
