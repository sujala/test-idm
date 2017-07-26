package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.DuplicateException;
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
        Response.ResponseBuilder builder = cloudExceptionResponse.exceptionResponse(new DuplicateException("Duplicate username."));
        assertThat("response code", builder.build().getStatus(), equalTo(409));
    }

    @Test
    public void methodNotAllowedExceptionResponse_returns405() throws Exception {
        Response.ResponseBuilder responseBuilder = cloudExceptionResponse.methodNotAllowedExceptionResponse("foo");
        assertThat("response code",responseBuilder.build().getStatus(), equalTo(405));

    }

    @Test
    public void exceptionResponse_exceptionIsInstanceOfNumberFormatException_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = cloudExceptionResponse.exceptionResponse(new NumberFormatException());
        assertThat("response code",responseBuilder.build().getStatus(), equalTo(400));
    }
    
    @Test
    public void exceptionResponse_exceptionIsInstanceOfBaseUrlConflictException_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = cloudExceptionResponse.exceptionResponse(new BaseUrlConflictException());
        assertThat("response code",responseBuilder.build().getStatus(), equalTo(400));
    }
}
