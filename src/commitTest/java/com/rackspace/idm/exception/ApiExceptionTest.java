package com.rackspace.idm.exception;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/10/12
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApiExceptionTest {
    ApiException apiException;

    @Before
    public void setUp() throws Exception {
        apiException = new ApiException(1, "message", "details");
    }

    @Test
    public void toString_returnsToString() throws Exception {
        String result = apiException.toString();
        assertThat("toString", result, equalTo("null [err=ApiError [code=1, message=message, details=details]]"));
    }
}
