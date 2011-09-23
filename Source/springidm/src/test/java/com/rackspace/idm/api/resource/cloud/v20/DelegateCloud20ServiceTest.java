package com.rackspace.idm.api.resource.cloud.v20;

import java.util.HashMap;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:25 PM
 */
public class DelegateCloud20ServiceTest {

    DelegateCloud20Service delegateCloud20Service;
    CloudClient cloudClient = mock(CloudClient.class);
    HttpHeaders httpHeaders = mock(HttpHeaders.class);
    private Configuration config = mock(Configuration.class);
    AuthenticationRequest authenticationRequest =mock(AuthenticationRequest.class);
    String url = "url";

    @Before
    public void setUp() throws IOException, JAXBException {
        delegateCloud20Service = new DelegateCloud20Service();
        delegateCloud20Service.setCloudClient(cloudClient);
        when(config.getString("cloudAuth20url")).thenReturn(url);
        delegateCloud20Service.setConfig(config);
        when(cloudClient.post(anyString(), Matchers.<HttpHeaders>any(), anyString())).thenReturn(Response.ok());
    }

    @Test
    public void authenticate_returnsResponse() throws Exception {
        assertThat("response", delegateCloud20Service.authenticate(httpHeaders, authenticationRequest), instanceOf(Response.ResponseBuilder.class));
    }

    @Test
    public void authenticate_callsClient() throws Exception {
        delegateCloud20Service.authenticate(httpHeaders, authenticationRequest);
        verify(cloudClient).post(anyString(), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void appendQueryParams_withNoParams_remainsSame() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", request, equalTo(updatedRequest));
    }

    @Test
    public void appendQueryParams_withOneParams_returnsCorrectValue() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("param", "value");
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", updatedRequest, equalTo("http://localhost/echo?param=value"));
    }

    @Test
    public void appendQueryParams_withTwoParams_returnsCorrectValue() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("param", "value");
        params.put("param2", "value2");
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", updatedRequest, anyOf(
            equalTo("http://localhost/echo?param=value&param2=value2"),
            equalTo("http://localhost/echo?param2=value2&param=value")
        ));
    }

    @Test
    public void appendQueryParams_withNullParam_returnsCorrectValue() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("param", "value");
        params.put("param2", null);
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", updatedRequest, equalTo("http://localhost/echo?param=value"));
    }
}
