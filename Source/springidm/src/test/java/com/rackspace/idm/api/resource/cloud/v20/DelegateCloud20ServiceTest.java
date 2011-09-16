package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
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
    String requestBody = "body";
    String url = "url";

    @Before
    public void setUp() throws IOException {
        delegateCloud20Service = new DelegateCloud20Service();
        delegateCloud20Service.setCloudClient(cloudClient);
        when(config.getString("cloudAuth20url")).thenReturn(url);
        when(cloudClient.post(url, httpHeaders, requestBody)).thenReturn(Response.ok());
        delegateCloud20Service.setConfig(config);
    }
    @Test
    public void authenticate_returnsResponse() throws Exception {
        assertThat("response", delegateCloud20Service.authenticate(httpHeaders,requestBody), instanceOf(Response.ResponseBuilder.class));
    }

    @Test
    public void authenticate_callsClient() throws Exception {
        delegateCloud20Service.authenticate(httpHeaders,requestBody);
        verify(cloudClient).post(url,httpHeaders,requestBody);
    }
}
