package com.rackspace.idm.api.resource.cloud;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/23/12
 * Time: 4:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpHeadersAcceptXmlTest {
    HttpHeadersAcceptXml headersAcceptXml;
    HttpHeaders headerMock;

    @Before
    public void setUp() throws Exception {
        headerMock = mock(HttpHeaders.class);
        headersAcceptXml = new HttpHeadersAcceptXml(headerMock);
    }

    @Test
    public void getRequestHeaders_ifHasAccept_setsAcceptToXml() throws Exception {
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        multivaluedMap.add("Accept", "application/json");
        when(headerMock.getRequestHeaders()).thenReturn(multivaluedMap);
        MultivaluedMap<String,String> requestHeaders = headersAcceptXml.getRequestHeaders();
        assertThat("request Accept Header", requestHeaders.getFirst("Accept"), equalTo("application/xml"));
    }

    @Test
    public void getRequestHeaders_ifHasNoAcceptHeader_returnsNull() throws Exception {
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        when(headerMock.getRequestHeaders()).thenReturn(multivaluedMap);
        MultivaluedMap<String,String> requestHeaders = headersAcceptXml.getRequestHeaders();
        assertThat("request Accept Header", requestHeaders.getFirst("Accept"), equalTo(null));
    }

    @Test
    public void getRequestHeader_ifRequestingAcceptHeader_returnApplicationXml() throws Exception {
        List<String> accept = headersAcceptXml.getRequestHeader("accept");
        assertThat("header value", accept.get(0), equalTo("application/xml"));
    }

    @Test
    public void getRequestHeader_ifRequestingAnyOtherHeader_returnsValueFromInnerHeaders() throws Exception {
        List<String> accept = headersAcceptXml.getRequestHeader("someHeader");
        assertThat("header value", accept.size(), equalTo(0));
    }

    @Test
    public void getMediaType_returnsInnerHeadersMediaType() throws Exception {
        when(headerMock.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        assertThat("header media type", headersAcceptXml.getMediaType(), equalTo(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void getLanguage_returnsInnerHeadersLanguage() throws Exception {
        when(headerMock.getLanguage()).thenReturn(Locale.ENGLISH);
        assertThat("header locale", headersAcceptXml.getLanguage(), equalTo(Locale.ENGLISH));
    }

    @Test
    public void getCookies_returnsInnerHeaderCookies() throws Exception {
        Map<String, Cookie> stringCookieHashMap = new HashMap<String, Cookie>();
        when(headerMock.getCookies()).thenReturn(stringCookieHashMap);
        assertThat("header cookies", headersAcceptXml.getCookies(), equalTo(stringCookieHashMap));
    }

    @Test
    public void getAcceptableMediaTypes_returnsApplicationXml() throws Exception {
        assertThat("acceptible media type size", headersAcceptXml.getAcceptableMediaTypes().size(), equalTo(1));
        assertThat("acceptible media type", headersAcceptXml.getAcceptableMediaTypes().get(0), equalTo(MediaType.APPLICATION_XML_TYPE));
    }

    @Test
    public void getAcceptableLanguages_returnsInnerHeadersAcceptableLanguages() throws Exception {
        List<Locale> locales = new ArrayList<Locale>();
        when(headerMock.getAcceptableLanguages()).thenReturn(locales);
        assertThat("acceptable languages", headersAcceptXml.getAcceptableLanguages(), equalTo(locales));
    }
}
