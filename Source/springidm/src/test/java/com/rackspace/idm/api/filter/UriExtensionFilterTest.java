package com.rackspace.idm.api.filter;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/20/12
 * Time: 1:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class UriExtensionFilterTest {

    UriExtensionFilter uriExtensionFilter;
    UriExtensionFilter spy;

    @Before
    public void setUp() throws Exception {
        uriExtensionFilter = new UriExtensionFilter();
        spy = spy(uriExtensionFilter);
    }

    @Test
    public void filter_shouldFilter_altersRequestAltersHeaders() throws Exception {
        MultivaluedMap<String,String> headers = new MultivaluedMapImpl();
        ContainerRequest request = mock(ContainerRequest.class);
        URI uri = URI.create("test");

        when(request.getAbsolutePath()).thenReturn(URI.create("absolutePath.com"));
        when(request.getBaseUri()).thenReturn(URI.create(""));
        when(request.getRequestHeaders()).thenReturn(headers);
        doReturn(uri).when(spy).getRequestUri("absolutePath", null);
        doNothing().when(request).setUris(null, uri);
        doReturn(true).when(spy).shouldFilter("/absolutePath.com", "com");

        spy.filter(request);
        assertThat("has correct headers",headers.size(),equalTo(1));
        assertThat("has correct values",headers.get("Accept").get(0),equalTo(""));
    }

    @Test
    public void filter_shouldFilter_setsRequestUrisProperly() throws Exception {
        MultivaluedMap<String,String> headers = new MultivaluedMapImpl();
        ContainerRequest request = mock(ContainerRequest.class);
        URI uri = URI.create("test");

        when(request.getAbsolutePath()).thenReturn(URI.create("absolutePath.com"));
        when(request.getBaseUri()).thenReturn(uri);
        when(request.getRequestHeaders()).thenReturn(headers);
        doReturn(uri).when(spy).getRequestUri("absolutePath", null);
        doNothing().when(request).setUris(null, uri);
        doReturn(true).when(spy).shouldFilter("/absolutePath.com", "com");

        spy.filter(request);
        verify(spy).getRequestUri("absolutePath", null);
        verify(request,times(2)).getBaseUri();
        verify(request).setUris(uri,uri);
    }

    @Test
    public void filter_shouldNotFilter_returnsUnalteredRequest() throws Exception {
        ContainerRequest request = mock(ContainerRequest.class);
        when(request.getAbsolutePath()).thenReturn(URI.create("absolutePath.com"));
        when(request.getBaseUri()).thenReturn(URI.create(""));
        doReturn(false).when(spy).shouldFilter("/absolutePath.com","com");
        spy.filter(request);
        verify(request,never()).getRequestHeaders();
        verify(request,never()).setUris(any(URI.class),any(URI.class));
    }

    @Test
    public void shouldFilter_jsonExtension_returnsTrue() throws Exception {
        assertThat("returns boolean",uriExtensionFilter.shouldFilter(null,"json"),equalTo(true));
    }

    @Test
    public void shouldFilter_xmlExtension_returnsTrue() throws Exception {
        assertThat("returns boolean",uriExtensionFilter.shouldFilter(null,"xml"),equalTo(true));
    }

    @Test
    public void shouldFilter_incorrectExtension_returnsFalse() throws Exception {
        assertThat("returns boolean",uriExtensionFilter.shouldFilter(null,"xml/json"),equalTo(false));
    }

    @Test
    public void getRequestUri_listPopulated_setsRequestParameters() throws Exception {
        List<String> list = new ArrayList<String>();
        list.add("testing");
        Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
        queryParams.put("test",list);
        assertThat("request", uriExtensionFilter.getRequestUri("absolutePathWithoutExtension", queryParams).getQuery(), equalTo("test=testing"));
    }

    @Test
    public void getRequestUri_listNotPopulated_doesNotSetRequestParameters() throws Exception {
        List<String> list = new ArrayList<String>();
        Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
        queryParams.put("test",list);
        assertThat("request", uriExtensionFilter.getRequestUri("absolutePathWithoutExtension", queryParams).getQuery(), equalTo(null));
    }

    @Test
    public void getRequestUri_noQueryParameters_doesNotSetRequestParameters() throws Exception {
        Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
        assertThat("request", uriExtensionFilter.getRequestUri("absolutePathWithoutExtension", queryParams).getQuery(), equalTo(null));
    }
}
