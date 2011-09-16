package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
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
    JAXBElement<AuthenticationRequest> authenticationRequest =mock(JAXBElement.class);
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
}
