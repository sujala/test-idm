package com.rackspace.idm.api.filter;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/21/12
 * Time: 11:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class ResponseFilterTest {
    private ResponseFilter responseFilter;
    private FilterConfig filterConfig;
    private FilterChain filterChain;
    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;

    @Before
    public void setUp() throws Exception {
        responseFilter = new ResponseFilter();

        //mocks
        filterConfig = mock(FilterConfig.class);
        filterChain = mock(FilterChain.class);
        httpServletRequest = mock(HttpServletRequest.class);
        httpServletResponse = mock(HttpServletResponse.class);
    }

    @Test (expected = ServletException.class)
    public void init_allHeadersIsNull_throwsServletException() throws Exception {
        responseFilter.init(filterConfig);
    }

    @Test (expected = ServletException.class)
    public void init_headerTokenizerNotEqual2_throwsServletException() throws Exception {
        when(filterConfig.getInitParameter(anyString())).thenReturn("allHeaders");
        responseFilter.init(filterConfig);
    }

    @Test
    public void init_headerIsEmpty_doesNothing() throws Exception {
        when(filterConfig.getInitParameter(anyString())).thenReturn("");
        responseFilter.init(filterConfig);
    }

    @Test
    public void init_headerTokenizerEquals2_succeeds() throws Exception {
        when(filterConfig.getInitParameter(anyString())).thenReturn("headerName: value");
        responseFilter.init(filterConfig);
    }

    @Test
    public void doFilter_responseContainsHeaderIsFalse_setHeader() throws Exception {
        when(httpServletResponse.containsHeader("response-source")).thenReturn(false);
        when(filterConfig.getInitParameter(anyString())).thenReturn("headerName: value");
        responseFilter.init(filterConfig);
        responseFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(httpServletResponse).setHeader(anyString(), anyString());
    }

    @Test
    public void doFilter_callsFilterChain_doFilter() throws Exception {
        when(httpServletResponse.containsHeader("response-source")).thenReturn(true);
        when(filterConfig.getInitParameter(anyString())).thenReturn("headerName: value");
        responseFilter.init(filterConfig);
        responseFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    public void destroy_success() throws Exception {
        responseFilter.destroy();
    }
}
