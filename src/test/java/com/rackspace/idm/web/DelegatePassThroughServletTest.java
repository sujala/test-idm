package com.rackspace.idm.web;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/23/12
 * Time: 9:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class DelegatePassThroughServletTest {

    @Test
    public void callsGetServletContext() throws Exception {
        DelegatePassThroughServlet spy = spy(new DelegatePassThroughServlet());
        ServletContext servletContext = mock(ServletContext.class);
        doReturn(servletContext).when(spy).getServletContext();
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(servletContext.getNamedDispatcher(anyString())).thenReturn(dispatcher);
        HttpServletRequest request = mock(HttpServletRequest.class);
        spy.doGet(request, null);
        verify(spy).getServletContext();
    }

    @Test
    public void callsDispatcher_forward() throws Exception {
        DelegatePassThroughServlet spy = spy(new DelegatePassThroughServlet());
        ServletContext servletContext = mock(ServletContext.class);
        doReturn(servletContext).when(spy).getServletContext();
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(servletContext.getNamedDispatcher(anyString())).thenReturn(dispatcher);
        HttpServletRequest request = mock(HttpServletRequest.class);
        spy.doGet(request, null);
        verify(dispatcher).forward(any(HttpServletRequest.class), Matchers.<ServletResponse>eq(null));
    }

    @Test
    public void newRequest_returnsEmptyServletPath() throws Exception {
        DelegatePassThroughServlet spy = spy(new DelegatePassThroughServlet());
        ServletContext servletContext = mock(ServletContext.class);
        doReturn(servletContext).when(spy).getServletContext();
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(servletContext.getNamedDispatcher(anyString())).thenReturn(dispatcher);
        HttpServletRequest request = mock(HttpServletRequest.class);
        spy.doGet(request, null);
        ArgumentCaptor<HttpServletRequest> argumentCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(dispatcher).forward(argumentCaptor.capture(), Matchers.<ServletResponse>eq(null));
        assertThat(argumentCaptor.getValue().getServletPath(), equalTo(""));
    }

    @Test
    public void newRequest_returnsCombinedPathFor_getPathInfo() throws Exception {
        DelegatePassThroughServlet spy = spy(new DelegatePassThroughServlet());
        ServletContext servletContext = mock(ServletContext.class);
        doReturn(servletContext).when(spy).getServletContext();
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(servletContext.getNamedDispatcher(anyString())).thenReturn(dispatcher);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getPathInfo()).thenReturn("secondHalf");
        when(request.getServletPath()).thenReturn("firstHalf");
        spy.doGet(request, null);
        ArgumentCaptor<HttpServletRequest> argumentCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(dispatcher).forward(argumentCaptor.capture(), Matchers.<ServletResponse>eq(null));
        assertThat(argumentCaptor.getValue().getPathInfo(), equalTo("firstHalfsecondHalf"));
    }
}
