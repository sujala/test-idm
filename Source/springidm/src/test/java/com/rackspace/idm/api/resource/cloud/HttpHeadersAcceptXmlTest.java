package com.rackspace.idm.api.resource.cloud;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import static org.mockito.Mockito.mock;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/23/12
 * Time: 4:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpHeadersAcceptXmlTest {
    HttpHeadersAcceptXml headersAcceptXml;

    @Before
    public void setUp() throws Exception {
        HttpHeaders headerMock = mock(HttpHeaders.class);
        headersAcceptXml = new HttpHeadersAcceptXml(headerMock);
    }

    @Test
    @Ignore
    public void getRequestHeaders_ifHasAccept_setsAcceptToXml() throws Exception {
        MultivaluedMap<String,String> requestHeaders = headersAcceptXml.getRequestHeaders();       //TODO: finish these tests
    }
}
