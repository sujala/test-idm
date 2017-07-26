package com.rackspace.idm.api.resource.cloud;

import com.sun.grizzly.http.servlet.HttpServletResponseImpl;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mortbay.jetty.Response;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponseWrapper;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/21/12
 * Time: 9:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class UrlFilterTest {

    UrlFilter urlFiler;
    ServletResponse servletResponse;
    HttpServletRequest servletRequest;
    FilterChain filterChain;

    @Before
    public void setUp() throws Exception {
        servletResponse = mock(ServletResponse.class);
        servletRequest = mock(HttpServletRequest.class);
        filterChain = mock(FilterChain.class);
        urlFiler = new UrlFilter();
        urlFiler.init(null);
    }

    @Test
    public void doFilter_callsFilterChain_doFilter() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("this.uri/cloud");
        when(servletRequest.getPathInfo()).thenReturn("path.info");
        when(servletRequest.getParameterMap()).thenReturn(new HashMap());

        urlFiler.doFilter(servletRequest, servletResponse, filterChain);

        verify(filterChain).doFilter(any(ServletRequest.class), eq(servletResponse));
    }

    @Test
    public void doFilter_WithEmptyMap_PassesOldMap() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("this.uri/cloud");
        when(servletRequest.getPathInfo()).thenReturn("path.info");
        HashMap<String, String[]> oldMap = new HashMap<String, String[]>();
        when(servletRequest.getParameterMap()).thenReturn(oldMap);

        urlFiler.doFilter(servletRequest, servletResponse, filterChain);

        ArgumentCaptor<HttpServletRequest> argumentCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(argumentCaptor.capture(), eq(servletResponse));
        assertThat("new requests map", (HashMap<String, String[]>) argumentCaptor.getValue().getParameterMap(), equalTo(oldMap));
    }

    @Test
    public void doFilter_WithNonEmptyMap_createsDecodedMap() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("this.uri/cloud");
        when(servletRequest.getPathInfo()).thenReturn("path.info");
        HashMap<String, String[]> oldMap = new HashMap<String, String[]>();
        oldMap.put("someKey", new String[]{"some", "values"});
        when(servletRequest.getParameterMap()).thenReturn(oldMap);

        urlFiler.doFilter(servletRequest, servletResponse, filterChain);

        ArgumentCaptor<HttpServletRequest> argumentCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(argumentCaptor.capture(), eq(servletResponse));
        assertThat("new requests map", (HashMap<String, String[]>) argumentCaptor.getValue().getParameterMap(), not(equalTo(oldMap)));
    }

    @Test
    public void doFilter_passesDecodedUriToNewRequest() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("this.uri/cloud+service");
        when(servletRequest.getPathInfo()).thenReturn("path.info");
        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());

        urlFiler.doFilter(servletRequest, servletResponse, filterChain);

        ArgumentCaptor<HttpServletRequest> argumentCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(argumentCaptor.capture(), eq(servletResponse));
        assertThat("new requests uri", argumentCaptor.getValue().getRequestURI(), equalTo("this.uri/cloud service"));
    }

    @Test
    public void doFilter_passesDecodedPathInfoToNewRequest() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("this.uri/cloud");
        when(servletRequest.getPathInfo()).thenReturn("path.info+something");
        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());

        urlFiler.doFilter(servletRequest, servletResponse, filterChain);

        ArgumentCaptor<HttpServletRequest> argumentCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(argumentCaptor.capture(), eq(servletResponse));
        assertThat("new requests path info", argumentCaptor.getValue().getPathInfo(), equalTo("path.info something"));
    }
}
