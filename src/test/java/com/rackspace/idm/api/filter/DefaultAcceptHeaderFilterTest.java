package com.rackspace.idm.api.filter;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private MultivaluedMapImpl multivaluedMap;

    @Before
    public void setUp() throws Exception {
        defaultAcceptHeaderFilter = new DefaultAcceptHeaderFilter();

        //mocks
        containerRequest = mock(ContainerRequest.class);
        multivaluedMap = mock(MultivaluedMapImpl.class);
    }

    @Test
    public void filter_containsKeyIsTrue_returnsSameRequest() throws Exception {
        when(containerRequest.getRequestHeaders()).thenReturn(multivaluedMap);
        when(multivaluedMap.containsKey(HttpHeaders.ACCEPT)).thenReturn(true);
        ContainerRequest response = defaultAcceptHeaderFilter.filter(containerRequest);
        assertThat("container request", response, equalTo(containerRequest));
    }

    @Test
    public void filter_pathStartWithCloud_callsPutSingleJSON() throws Exception {
        when(containerRequest.getRequestHeaders()).thenReturn(multivaluedMap);
        when(multivaluedMap.containsKey(HttpHeaders.ACCEPT)).thenReturn(false);
        when(containerRequest.getPath()).thenReturn("cloud");
        defaultAcceptHeaderFilter.filter(containerRequest);
        verify(multivaluedMap).putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    }

    @Test
    public void filter_pathDoesNotStartWithCloud_callsPutSingleXML() throws Exception {
        when(containerRequest.getRequestHeaders()).thenReturn(multivaluedMap);
        when(multivaluedMap.containsKey(HttpHeaders.ACCEPT)).thenReturn(false);
        when(containerRequest.getPath()).thenReturn("global");
        defaultAcceptHeaderFilter.filter(containerRequest);
        verify(multivaluedMap).putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
    }
}
