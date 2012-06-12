package com.rackspace.idm.web;


import com.sun.grizzly.http.servlet.HttpServletResponseImpl;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/12/12
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class AcceptServletTest {
    private AcceptServlet acceptServlet;
    private AcceptServlet spy;
    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;

    @Before
    public void setup() throws Exception {
        acceptServlet = new AcceptServlet();

        // Setup mocks
        httpServletRequest = mock(HttpServletRequest.class);
        httpServletResponse = mock(HttpServletResponse.class);

        spy = spy(acceptServlet);
    }

    @Test
    public void doPost_redirectUriIsBlank_setStatus400() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("");
        acceptServlet.doPost(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).setStatus(400);
    }

    @Test
    public void doPost_allParametersIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyResponseTypeIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyClientIdIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyScopeIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyAcceptIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyDaysIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyUsernameIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_onlyVerificationIsBlank_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

    @Test
    public void doPost_responseTypeEqualsCode_setErrorResponseInvalidRequest() throws Exception {
        when(httpServletRequest.getParameter("redirect_uri")).thenReturn("redirect_uri");
        when(httpServletRequest.getParameter("response_type")).thenReturn("response_type");
        when(httpServletRequest.getParameter("client_id")).thenReturn("client_id");
        when(httpServletRequest.getParameter("scope")).thenReturn("scope");
        when(httpServletRequest.getParameter("accept")).thenReturn("accept");
        when(httpServletRequest.getParameter("days")).thenReturn("days");
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("verification")).thenReturn("verification");
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).setErrorResponse(httpServletResponse, "redirect_uri", "invalid_request");
    }

}
