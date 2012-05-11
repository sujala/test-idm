package com.rackspace.idm.api.resource.cloud;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 12/30/11
 * Time: 8:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class CloudExceptionResponseTest {

    CloudExceptionResponse cloudExceptionResponse;

    @Before
    public void setUp() throws Exception {
        cloudExceptionResponse = new CloudExceptionResponse();
    }

    @Test
    public void usernameConflictExceptionResponse_returns409() throws Exception {
        Response.ResponseBuilder builder = cloudExceptionResponse.usernameConflictExceptionResponse("foo");
        assertThat("response code", builder.build().getStatus(), equalTo(409));
    }

    @Test
    public void methodNotAllowedExceptionResponse_returns405() throws Exception {
        Response.ResponseBuilder responseBuilder = cloudExceptionResponse.methodNotAllowedExceptionResponse("foo");
        assertThat("response code",responseBuilder.build().getStatus(), equalTo(405));

    }
}
