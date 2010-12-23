package com.rackspace.idm.interceptors;

import java.net.URISyntaxException;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.GlobalConstants;

public class AcceptHeaderDefaultingInterceptorTest {
    private AcceptHeaderDefaultingInterceptor interceptor;
    private MockHttpRequest request;

    @Before
    public void setUp() throws URISyntaxException {
        interceptor = new AcceptHeaderDefaultingInterceptor();
        request = MockHttpRequest.get("/foo");
    }

    @Test
    public void shouldSetAcceptHeaderToJsonIfNotGiven() {
        interceptor.preProcess(request, null);
        Object respOverride = request
            .getAttribute(GlobalConstants.RESPONSE_TYPE_DEFAULT);
        Assert.assertEquals(MediaType.APPLICATION_JSON, respOverride);
    }

    @Test
    public void shouldSetAcceptHeaderToJsonIfSetToWildcard() {
        request.accept(MediaType.WILDCARD);
        interceptor.preProcess(request, null);
        Object respOverride = request
            .getAttribute(GlobalConstants.RESPONSE_TYPE_DEFAULT);
        Assert.assertEquals(MediaType.APPLICATION_JSON, respOverride);
    }

    @Test
    public void shouldNotOverrideIfSetToXml() {
        request.accept(MediaType.APPLICATION_XML);
        interceptor.preProcess(request, null);
        Object respOverride = request
            .getAttribute(GlobalConstants.RESPONSE_TYPE_DEFAULT);
        Assert.assertNull(respOverride);
    }

    @Test
    public void shouldNotOverrideIfSetToJson() {
        request.accept(MediaType.APPLICATION_JSON);
        interceptor.preProcess(request, null);
        Object respOverride = request
            .getAttribute(GlobalConstants.RESPONSE_TYPE_DEFAULT);
        Assert.assertNull(respOverride);
    }
}
