package com.rackspace.idm.web;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/20/12
 * Time: 4:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class DisplayCodeServletTest {
    private DisplayCodeServlet displayCodeServlet;
    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;
    private DisplayCodeServlet spy;

    @Before
    public void setUp() throws Exception {
        httpServletRequest = mock(HttpServletRequest.class);
        httpServletResponse = mock(HttpServletResponse.class);
        displayCodeServlet = new DisplayCodeServlet();

        spy = spy(displayCodeServlet);
    }

    @Test
    public void doGet_callsProcessRequest() throws Exception {
        PrintWriter printWriter = mock(PrintWriter.class);
        when(httpServletResponse.getWriter()).thenReturn(printWriter);
        spy.doGet(httpServletRequest, httpServletResponse);
        verify(spy).processRequest(httpServletRequest, httpServletResponse);
    }

    @Test
    public void doPost_callsProcessRequest() throws Exception {
        PrintWriter printWriter = mock(PrintWriter.class);
        when(httpServletResponse.getWriter()).thenReturn(printWriter);
        spy.doPost(httpServletRequest, httpServletResponse);
        verify(spy).processRequest(httpServletRequest, httpServletResponse);
    }

    @Test
    public void processRequest_callsResponse_getWriter() throws Exception {
        PrintWriter printWriter = mock(PrintWriter.class);
        when(httpServletResponse.getWriter()).thenReturn(printWriter);
        when(httpServletRequest.getParameter("error")).thenReturn("error");
        when(httpServletRequest.getParameter("code")).thenReturn("");
        displayCodeServlet.processRequest(httpServletRequest, httpServletResponse);
        verify(httpServletResponse).getWriter();
    }

    @Test
    public void processRequest_callsRequest_getParameter() throws Exception {
        PrintWriter printWriter = mock(PrintWriter.class);
        when(httpServletResponse.getWriter()).thenReturn(printWriter);
        when(httpServletRequest.getParameter("error")).thenReturn("error");
        when(httpServletRequest.getParameter("code")).thenReturn("code");
        displayCodeServlet.processRequest(httpServletRequest, httpServletResponse);
        verify(httpServletRequest, times(2)).getParameter(anyString());
    }
}
