package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:25 PM
 */
public class DelegateCloud20ServiceTest {

    DummyCloud20Service dummyCloud20Service;
    DelegateCloud20Service delegateCloud20Service;
    DefaultCloud20Service defaultCloud20Service = mock(DefaultCloud20Service.class);
    CloudClient cloudClient = mock(CloudClient.class);
    HttpHeaders httpHeaders = mock(HttpHeaders.class);
    Marshaller marshaller = mock(Marshaller.class);
    private final Configuration config = mock(Configuration.class);
    AuthenticationRequest authenticationRequest =mock(AuthenticationRequest.class);
    String url = "url";
    Boolean disabled = true;
    String body = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns12:endpointTemplate xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";


    @Before
    public void setUp() throws IOException, JAXBException {
        dummyCloud20Service = new DummyCloud20Service();
        delegateCloud20Service = new DelegateCloud20Service();
        delegateCloud20Service.setDummyCloud20Service(dummyCloud20Service);
        delegateCloud20Service.setCloudClient(cloudClient);
        delegateCloud20Service.setDefaultCloud20Service(defaultCloud20Service);
        when(config.getString("cloudAuth20url")).thenReturn(url);
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(disabled);
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

    @Test
    public void listExtensions_useCloudAuthIsTrue_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(cloudClient).get(url+"extensions",null);
    }

    @Test
    public void listExtensions_callsConfigUseCloudAuth() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void listExtensions_useCloudAuthIsFalse_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud20Service.listExtensions(null);
        verify(cloudClient,times(0)).get(url,null);
    }


   @Test
    public void listTenants_useCloudAuthIsTrue_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.listTenants(null, "token", null, null);
        verify(cloudClient).get(url+"tenants",null);
    }

    @Test
    public void listTenants_useCloudAuthIsFalse_doesntCallCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud20Service.listTenants(null, "token",null, null);
        verify(cloudClient,times(0)).get(url+"tenants",null);
    }

    @Test
    public void listTenants_useCloudAuthIsFalse_returns200() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(defaultCloud20Service.listTenants(null,"token",null,null)).thenReturn(Response.ok());
        Response.ResponseBuilder responseBuilder = delegateCloud20Service.listTenants(null, "token", null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_defaultServiceReturns404_callsClient() throws Exception {
        when(defaultCloud20Service.validateToken(null,null,null,null)).thenReturn(Response.status(404));
        delegateCloud20Service.validateToken(null,null,"tokenId",null);
        verify(cloudClient).get(url+"tokens/tokenId",null);
    }

    @Test
    public void validateToken_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.validateToken(null,null,null,null)).thenReturn(Response.status(401));
        delegateCloud20Service.validateToken(null,null,null,null);
        verify(cloudClient).get(url+"tokens/"+null,null);
    }

    @Test
    public void checkTokenToken_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.checkToken(null,null,null,null)).thenReturn(Response.status(401));
        delegateCloud20Service.checkToken(null,null,null,null);
        verify(cloudClient).get(url+"tokens/"+null,null);
    }

    @Test
    public void checkToken_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.checkToken(null,null,null,null)).thenReturn(Response.status(404));
        delegateCloud20Service.checkToken(null,null,null,null);
        verify(cloudClient).get(url+"tokens/"+null,null);
    }

    @Test
    public void listEndpointsForToken_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.listEndpointsForToken(null,null,null)).thenReturn(Response.status(401));
        delegateCloud20Service.listEndpointsForToken(null,null,null);
        verify(cloudClient).get(url+"tokens/"+null+"/endpoints",null);
    }

    @Test
    public void listEndpointsForToken_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.listEndpointsForToken(null,null,null)).thenReturn(Response.status(404));
        delegateCloud20Service.listEndpointsForToken(null,null,null);
        verify(cloudClient).get(url+"tokens/"+null+"/endpoints",null);
    }

    @Test
    public void getExtension_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getExtension(null,null)).thenReturn(Response.status(401));
        delegateCloud20Service.getExtension(null,null);
        verify(cloudClient).get(url+"extensions/"+null,null);
    }

    @Test
    public void getExtension_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getExtension(null,null)).thenReturn(Response.status(404));
        delegateCloud20Service.getExtension(null,null);
        verify(cloudClient).get(url+"extensions/"+null,null);
    }

    @Test
    public void addEndpointTemplate_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.addEndpointTemplate(null,null,null,null)).thenReturn(Response.status(401));
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        verify(cloudClient).post(url + "OS-KSCATALOG/endpointTemplates", null, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns12:endpointTemplate xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
    }

    @Test
    public void addEndpointTemplate_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.addEndpointTemplate(null, null, null, null)).thenReturn(Response.status(404));
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        body = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns12:endpointTemplate xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        verify(cloudClient).post(url + "OS-KSCATALOG/endpointTemplates", null, body);
    }

    @Test
    public void getEndpointTemplate_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getEndpointTemplate(null, null, null)).thenReturn(Response.status(401));
        delegateCloud20Service.getEndpointTemplate(null, null, null);
        verify(cloudClient).get(url + "OS-KSCATALOG/endpointTemplates/null", null);
    }

    @Test
    public void getEndpointTemplate_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getEndpointTemplate(null, null, null)).thenReturn(Response.status(404));
        delegateCloud20Service.getEndpointTemplate(null, null, null);
        verify(cloudClient).get(url + "OS-KSCATALOG/endpointTemplates/null", null);
    }

    @Test
    public void deleteEndpointTemplate_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteEndpointTemplate(null, null, null)).thenReturn(Response.status(401));
        delegateCloud20Service.deleteEndpointTemplate(null, null, null);
        verify(cloudClient).delete(url + "OS-KSCATALOG/endpointTemplates/null", null);
    }

    @Test
    public void deleteEndpointTemplate_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteEndpointTemplate(null, null, null)).thenReturn(Response.status(404));
        delegateCloud20Service.deleteEndpointTemplate(null, null, null);
        verify(cloudClient).delete(url + "OS-KSCATALOG/endpointTemplates/null", null);
    }

    @Test
    public void listEndpoints_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.listEndpoints(null, null, null)).thenReturn(Response.status(401));
        delegateCloud20Service.listEndpoints(null, null, null);
        verify(cloudClient).get(url+"tenants/null/"+"OS-KSCATALOG/endpoints",null);
    }

    @Test
    public void listEndpoints_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.listEndpoints(null, null, null)).thenReturn(Response.status(404));
        delegateCloud20Service.listEndpoints(null, null, null);
        verify(cloudClient).get(url+"tenants/null/"+"OS-KSCATALOG/endpoints",null);
    }

    @Test
    public void addEndpoint_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.addEndpoint(null, null, null, null)).thenReturn(Response.status(401));
        delegateCloud20Service.addEndpoint(null, null, null, null);
        verify(cloudClient).post(url + "tenants/null/" + "OS-KSCATALOG/endpoints", null, body);
    }

    @Test
    public void addEndpoint_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.addEndpoint(null, null, null, null)).thenReturn(Response.status(404));
        delegateCloud20Service.addEndpoint(null, null, null, null);
        verify(cloudClient).post(url + "tenants/null/" + "OS-KSCATALOG/endpoints", null, body);
    }

    @Test
    public void getEndpoint_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getEndpoint(null, null, null,null)).thenReturn(Response.status(401));
        delegateCloud20Service.getEndpoint(null, null, null,null);
        verify(cloudClient).get(url+"tenants/null/"+"OS-KSCATALOG/endpoints/null",null);
    }

    @Test
    public void getEndpoint_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getEndpoint(null, null, null,null)).thenReturn(Response.status(404));
        delegateCloud20Service.getEndpoint(null, null, null,null);
        verify(cloudClient).get(url+"tenants/null/"+"OS-KSCATALOG/endpoints/null",null);
    }

    @Test
    public void deleteEndpoint_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteEndpoint(null, null, null,null)).thenReturn(Response.status(401));
        delegateCloud20Service.deleteEndpoint(null, null, null,null);
        verify(cloudClient).delete(url+"tenants/null/"+"OS-KSCATALOG/endpoints/null",null);
    }

    @Test
    public void deleteEndpoint_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteEndpoint(null, null, null,null)).thenReturn(Response.status(404));
        delegateCloud20Service.deleteEndpoint(null, null, null,null);
        verify(cloudClient).delete(url+"tenants/null/"+"OS-KSCATALOG/endpoints/null",null);
    }

    @Test
    public void getSecretQA_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getSecretQA( null, null,null)).thenReturn(Response.status(401));
        delegateCloud20Service.getSecretQA( null, null,null);
        verify(cloudClient).get(url+"users/null/RAX-KSQA/secretqa/",null);
    }

    @Test
    public void getSecretQA_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getSecretQA( null, null,null)).thenReturn(Response.status(404));
        delegateCloud20Service.getSecretQA( null, null,null);
        verify(cloudClient).get(url+"users/null/RAX-KSQA/secretqa/",null);
    }

    @Test
    public void updateSecretQA_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.updateSecretQA( null,null, null,null)).thenReturn(Response.status(401));
        delegateCloud20Service.updateSecretQA( null,null, null,null);
        verify(cloudClient).post(url+"users/null/RAX-KSQA/secretqa/",null,"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns13:secretQA xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
    }

    @Test
    public void updateSecretQA_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.updateSecretQA( null,null, null,null)).thenReturn(Response.status(404));
        delegateCloud20Service.updateSecretQA(null, null, null,null);
        verify(cloudClient).post(url+"users/null/RAX-KSQA/secretqa/",null,"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns13:secretQA xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
    }
}
