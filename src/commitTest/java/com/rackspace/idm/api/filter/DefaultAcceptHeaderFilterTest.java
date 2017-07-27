package com.rackspace.idm.api.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/21/12
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultAcceptHeaderFilterTest {
    private DefaultAcceptHeaderFilter defaultAcceptHeaderFilter;
    private ContainerRequest containerRequest;
    private MultivaluedMap multivaluedMap;

    @Before
    public void setUp() throws Exception {
        defaultAcceptHeaderFilter = new DefaultAcceptHeaderFilter();

        //mocks
        containerRequest = mock(ContainerRequest.class);
        multivaluedMap = mock(MultivaluedMap.class);
    }

    @Test
    public void filter_acceptMultipleAccepts_returnsSameRequest() throws Exception {
        when(containerRequest.getRequestHeaders()).thenReturn(multivaluedMap);
        ArrayList<String> headers = new ArrayList<String>();
        headers.add("application/xml");
        headers.add("*/*");
        when(multivaluedMap.get(HttpHeaders.ACCEPT)).thenReturn(headers);
        defaultAcceptHeaderFilter.filter(containerRequest);
        verify(multivaluedMap, never()).putSingle(anyString(), anyObject());
    }

    @Test
    public void filter_acceptNotAll_returnsSameRequest() throws Exception {
        when(containerRequest.getRequestHeaders()).thenReturn(multivaluedMap);
        ArrayList<String> headers = new ArrayList<String>();
        headers.add("application/xml");
        when(multivaluedMap.get(HttpHeaders.ACCEPT)).thenReturn(headers);
        defaultAcceptHeaderFilter.filter(containerRequest);
        verify(multivaluedMap, never()).putSingle(anyString(), anyObject());
    }

    @Test
    public void filter_pathStartWithCloud_callsPutSingleJSON() throws Exception {
        when(containerRequest.getRequestHeaders()).thenReturn(multivaluedMap);
        ArrayList<String> headers = new ArrayList<String>();
        headers.add("*/*");
        when(multivaluedMap.get(HttpHeaders.ACCEPT)).thenReturn(headers);
        when(containerRequest.getPath()).thenReturn("cloud");
        defaultAcceptHeaderFilter.filter(containerRequest);
        verify(multivaluedMap).putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    }

    @Test
    public void filter_pathDoesNotStartWithCloud_callsPutSingleXML() throws Exception {
        when(containerRequest.getRequestHeaders()).thenReturn(multivaluedMap);
        ArrayList<String> headers = new ArrayList<String>();
        headers.add("*/*");
        when(multivaluedMap.get(HttpHeaders.ACCEPT)).thenReturn(headers);
        when(containerRequest.getPath()).thenReturn("global");
        defaultAcceptHeaderFilter.filter(containerRequest);
        verify(multivaluedMap).putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
    }
}
